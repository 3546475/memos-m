package org.example.moxia.data.sync

import android.util.Log
import kotlinx.coroutines.CancellationException
import org.example.moxia.viewmodel.AccountContext
import org.example.moxia.viewmodel.AccountSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.example.moxia.api.GsonProvider
import org.example.moxia.api.MemosApi
import org.example.moxia.data.DataStoreManager
import org.example.moxia.data.audit.SyncAuditLogger
import org.example.moxia.data.cache.MemoCacheRepository
import org.example.moxia.data.media.AttachmentUploadQueue
import org.example.moxia.model.Memo
import org.example.moxia.model.User

private const val TAG = "SyncManager"

/**
 * Pushes queued local writes to the server,
 * detects content conflicts (server modified while we were offline) and lets
 * the user decide how to resolve them.
 *
 * Ops are processed in creation order so chained edits (create -> update ->
 * delete) apply correctly. A failed op does not block the rest of the queue;
 * it keeps its lastError and is retried on the next sync.
 */
class SyncManager(
    private val scope: CoroutineScope,
    private val repository: SyncRepository,
    private val memoCacheRepository: MemoCacheRepository,
    private val dataStoreManager: DataStoreManager,
    private val workScheduler: SyncWorkScheduler,
    private val auditLogger: SyncAuditLogger,
    private val accountSession: AccountSession,
    private val currentUserProvider: () -> User?,
    private val attachmentUploadQueueProvider: () -> AttachmentUploadQueue? = { null },
    private val onMemoSynced: suspend (memo: Memo, tempName: String?) -> Unit,
    private val onMemoDeleted: suspend (memoName: String) -> Unit,
    private val onCommentsRefresh: suspend () -> Unit,
    private val onConflict: (ConflictItem) -> Unit
) {

    private val gson = GsonProvider.gson

    private val _pendingOps = MutableStateFlow<List<PendingOp>>(emptyList())
    val pendingOps: StateFlow<List<PendingOp>> = _pendingOps.asStateFlow()

    data class AccountPendingOps(val context: AccountContext, val ops: List<PendingOp>)

    /** Re-emits unchanged queues when an account activation resets its UI. */
    val currentPendingOps: Flow<AccountPendingOps> =
        combine(accountSession.contexts, pendingOps) { context, ops ->
            if (context != null && ops.all { it.accountId == context.account.id })
                AccountPendingOps(context, ops) else null
        }.filterNotNull()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var syncJob: Job? = null
    private var retryJob: Job? = null

    /**
     * Set when a push is requested while a pass is already running. The running pass
     * takes its queue snapshot up front, so an op queued afterwards would otherwise
     * wait for the WorkManager fallback (up to six hours on the periodic request).
     */
    @Volatile
    private var rerunRequested = false

    @OptIn(ExperimentalCoroutinesApi::class)
    fun startObserving(accountIdFlow: Flow<String?>) {
        scope.launch {
            accountIdFlow.flatMapLatest { accountId ->
                if (accountId == null) flowOf(emptyList())
                else repository.getOpsFlow(accountId)
            }.collect { ops ->
                _pendingOps.value = ops
            }
        }
    }

    suspend fun enqueue(op: PendingOp, writeLocal: (suspend () -> Unit)? = null) {
        repository.enqueue(op, writeLocal)
        auditLogger.record(op.accountId, "QUEUE", "ENQUEUED", op.type, op.memoName)
        workScheduler.schedule(op.accountId)
    }

    suspend fun deleteOp(id: String) {
        repository.getOp(id)?.let { auditLogger.record(it.accountId, "QUEUE", "DISCARDED", it.type, it.memoName) }
        repository.deleteOp(id)
    }

    suspend fun clearForAccount(accountId: String) = repository.clearForAccount(accountId)

    /**
     * Clears the recorded failure of one queued op and pushes again. The background
     * worker skips permanently failed ops, so this is the way out of "needs
     * attention" without discarding the user's write.
     */
    suspend fun retryOp(id: String) {
        val op = repository.getOp(id) ?: return
        repository.retryOp(id)
        auditLogger.record(op.accountId, "QUEUE", "RETRY", op.type, op.memoName)
        pushPendingChanges()
    }

    fun cancelSync() {
        syncJob?.cancel()
        retryJob?.cancel()
        rerunRequested = false
        _isSyncing.value = false
    }

    /** Push all queued changes to the server, regardless of previous failures. */
    fun pushPendingChanges() {
        if (_isSyncing.value) {
            rerunRequested = true
            return
        }
        val context = accountSession.current ?: return
        if (!context.networkReady) return
        val accountId = context.account.id
        val user = currentUserProvider()
        syncJob?.cancel()
        // Claim the attempt before launching so consecutive gestures cannot
        // cancel and replace a replay that has not started running yet.
        _isSyncing.value = true
        syncJob = scope.launch {
            try {
                if (!accountSession.isCurrent(context)) return@launch
                AccountSyncCoordinator.withAccountLock(accountId) {
                    if (!accountSession.isCurrent(context)) return@withAccountLock
                    var ops = repository.getOps(accountId)
                    val executor = newExecutor(context, user)
                    if (ops.isNotEmpty()) {
                        Log.d(TAG, "pushPendingChanges: pushing ${ops.size} ops")
                    }
                    var index = 0
                    while (index < ops.size) {
                        if (!accountSession.isCurrent(context)) return@withAccountLock
                        // Reload after each acknowledgement so temporary-name
                        // remaps and rebased follow-ups are used immediately.
                        val opId = ops[index].id
                        val op = repository.getOp(opId) ?: run {
                            ops = repository.getOps(accountId)
                            index++
                            continue
                        }
                        try {
                            val success = executor.replay(op)
                            if (success) {
                                repository.deleteOp(op.id)
                                auditLogger.record(accountId, "SYNC", "SUCCESS", op.type, op.memoName)
                                ops = repository.getOps(accountId)
                                continue
                            }
                            index++
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(TAG, "Op ${op.id} (${op.type}) failed", e)
                            // 4xx means the server deterministically rejected the
                            // op: mark it for attention and let the next push retry it.
                            // 5xx and transport errors are transient: keep retrying
                            // with backoff. 408/429 are transient too (timeout /
                            // rate limit), matching OutboxSyncWorker and
                            // AttachmentUploadQueue.
                            val permanent = e is retrofit2.HttpException &&
                                e.code() in 400..499 && e.code() !in setOf(408, 429)
                            repository.markFailed(
                                op.id, op.attemptCount + 1, e.message,
                                System.currentTimeMillis(), permanent
                            )
                            auditLogger.record(
                                accountId,
                                "SYNC",
                                if (permanent) "REJECTED" else "RETRY",
                                op.type,
                                op.memoName,
                                failureCode(e)
                            )
                            if (!permanent) {
                                scheduleRetry(accountId, op.attemptCount + 1)
                            }
                            index++
                        }
                    }
                    if (repository.getOps(accountId).isEmpty()) {
                        dataStoreManager.saveLastSyncTime(accountId, System.currentTimeMillis())
                    }
                }
            } finally {
                if (accountSession.isCurrent(context)) _isSyncing.value = false
            }
            // Honour a request that arrived while this pass ran: the op it cares about may
            // have been queued after the queue snapshot was taken.
            if (rerunRequested) {
                rerunRequested = false
                if (accountSession.isCurrent(context)) pushPendingChanges()
            }
        }
    }

    private fun scheduleRetry(accountId: String, attemptCount: Int) {
        val delayMs = minOf(30_000L shl minOf(attemptCount, 4), 600_000L)
        workScheduler.schedule(accountId)
        retryJob?.cancel()
        retryJob = scope.launch {
            delay(delayMs)
            if (accountSession.current?.account?.id == accountId) {
                pushPendingChanges()
            }
        }
    }

    private fun failureCode(error: Exception): String = when (error) {
        is retrofit2.HttpException -> "http_${error.code()}"
        else -> error.javaClass.simpleName.take(64)
    }

    /**
     * Apply the user's decision for a detected conflict. [mergedContent] is
     * required for [ConflictResolution.MERGE]: the user-edited third version
     * combining the local and server content.
     */
    fun resolveConflict(
        item: ConflictItem,
        resolution: ConflictResolution,
        mergedContent: String? = null
    ) {
        val context = accountSession.current ?: return
        if (item.accountId != context.account.id) return
        val accountId = context.account.id
        val api = context.api
        val user = currentUserProvider()
        scope.launch {
            AccountSyncCoordinator.withAccountLock(accountId) {
                if (!accountSession.isCurrent(context)) return@withAccountLock
                val ownedOp = repository.getOp(item.opId) ?: return@withAccountLock
                if (ownedOp.accountId != accountId || ownedOp.memoName != item.memoName) return@withAccountLock
                val executor = newExecutor(context, user)
                // KEEP_LOCAL and MERGE overwrite the server copy, and the dialog may have been
                // open for minutes. Re-read before writing: pushing the version snapshotted when
                // the conflict was detected would silently drop a newer server version.
                val overwritesServer = resolution == ConflictResolution.KEEP_LOCAL ||
                    resolution == ConflictResolution.MERGE
                if (overwritesServer && !serverUnchangedSinceConflict(item, context, api)) {
                    return@withAccountLock
                }
                when (resolution) {
                    ConflictResolution.KEEP_LOCAL -> {
                        val op = repository.getOp(item.opId) ?: return@withAccountLock
                        val local = gson.fromJson(op.payloadJson, Memo::class.java)
                            ?: return@withAccountLock
                        try {
                            val updated = api.updateMemo(
                                item.memoName, local, op.updateMask ?: "content"
                            )
                            // Follow-up edits were based on the same local version;
                            // re-anchor them to the freshly-applied server state so
                            // they don't re-trigger a conflict on the next sync.
                            executor.anchorFollowUps(
                                item.memoName, op.baseUpdateTime, updated.updateTime
                            )
                            executor.cacheMemo(updated)
                            repository.deleteOp(item.opId)
                            if (accountSession.isCurrent(context)) onMemoSynced(updated, null)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(TAG, "Keep-local push failed for $item.memoName", e)
                            // Leave the op queued and re-surface the conflict so the
                            // user can retry or defer.
                            if (accountSession.isCurrent(context)) onConflict(item)
                        }
                    }

                    ConflictResolution.MERGE -> {
                        val op = repository.getOp(item.opId) ?: return@withAccountLock
                        val local = gson.fromJson(op.payloadJson, Memo::class.java)
                            ?: return@withAccountLock
                        if (mergedContent == null) {
                            // MERGE without a merged version would silently
                            // overwrite the server version with the local one;
                            // refuse and re-surface the conflict instead.
                            Log.e(TAG, "MERGE resolution without mergedContent for ${item.memoName}")
                            if (accountSession.isCurrent(context)) onConflict(item)
                            return@withAccountLock
                        }
                        val merged = local.copy(content = mergedContent)
                        try {
                            val updated = api.updateMemo(
                                item.memoName, merged, op.updateMask ?: "content"
                            )
                            executor.anchorFollowUps(
                                item.memoName, op.baseUpdateTime, updated.updateTime
                            )
                            executor.cacheMemo(updated)
                            repository.deleteOp(item.opId)
                            if (accountSession.isCurrent(context)) onMemoSynced(updated, null)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(TAG, "Merge push failed for $item.memoName", e)
                            if (accountSession.isCurrent(context)) onConflict(item)
                        }
                    }

                    ConflictResolution.KEEP_SERVER -> {
                        val op = repository.getOp(item.opId) ?: return@withAccountLock
                        repository.deleteOp(item.opId)
                        // Re-anchor follow-up edits (queued while offline, based
                        // on the discarded local version) to the adopted server
                        // state so they apply on top of it instead of re-triggering
                        // a conflict for the same memo.
                        executor.anchorFollowUps(
                            item.memoName, op.baseUpdateTime,
                            item.serverMemo.updateTime
                        )
                        executor.cacheMemo(item.serverMemo)
                        if (accountSession.isCurrent(context)) onMemoSynced(item.serverMemo, null)
                    }

                    ConflictResolution.LATER -> {
                        // Leave the op queued; it will re-trigger a conflict on next sync.
                    }
                }
            }
        }
    }

    /**
     * True while the server still holds the version the conflict was reported against. When it
     * changed again, the newer version is surfaced as a fresh conflict instead of being
     * overwritten by the stale local payload; an unreadable server is treated the same way so a
     * transient failure cannot silently approve an overwrite.
     */
    private suspend fun serverUnchangedSinceConflict(
        item: ConflictItem,
        context: AccountContext,
        api: MemosApi
    ): Boolean {
        return try {
            val fresh = api.getMemo(item.memoName)
            if (fresh.updateTime == item.serverMemo.updateTime) {
                true
            } else {
                Log.d(TAG, "Conflict on ${item.memoName} changed again; re-surfacing")
                if (accountSession.isCurrent(context)) {
                    onConflict(
                        ConflictItem(
                            opId = item.opId,
                            accountId = item.accountId,
                            memoName = item.memoName,
                            localMemo = item.localMemo,
                            serverMemo = fresh
                        )
                    )
                }
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Could not re-read ${item.memoName} before resolving", e)
            if (accountSession.isCurrent(context)) onConflict(item)
            false
        }
    }

    /**
     * Per-op replay logic is shared with [OutboxSyncWorker] via
     * [OpReplayExecutor]; this wires the UI-facing callbacks (conflict
     * surfacing, comment refresh, memo update/delete notifications) into it.
     */
    private fun newExecutor(context: AccountContext, currentUser: User?) =
        OpReplayExecutor(
            api = context.api,
            repository = repository,
            memoCacheRepository = memoCacheRepository,
            auditLogger = auditLogger,
            accountId = context.account.id,
            currentUser = currentUser,
            attachmentUploadQueueProvider = attachmentUploadQueueProvider,
            onMemoSynced = { memo, name ->
                if (accountSession.isCurrent(context)) onMemoSynced(memo, name)
            },
            onMemoDeleted = { name ->
                if (accountSession.isCurrent(context)) onMemoDeleted(name)
            },
            onCommentsRefresh = {
                if (accountSession.isCurrent(context)) onCommentsRefresh()
            },
            onConflict = { item ->
                if (accountSession.isCurrent(context)) onConflict(item)
            }
        )
}
