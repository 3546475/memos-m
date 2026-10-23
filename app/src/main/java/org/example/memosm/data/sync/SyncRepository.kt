package org.example.memosm.data.sync

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import org.example.memosm.data.cache.MemoCacheDatabase

/**
 * Repository for the offline write queue (pending operations).
 */
class SyncRepository(
    private val dao: PendingOpDao,
    /** Same database as the memo cache; enables queueing a write and applying it atomically. */
    private val database: MemoCacheDatabase? = null
) {

    suspend fun enqueue(op: PendingOp) {
        // Consecutive edits to the same memo can be collapsed safely: each
        // UPDATE payload is a complete optimistic memo, so only the newest
        // payload is needed while retaining the original server base version.
        if (op.type == PendingOpType.UPDATE.name && op.memoName != null) {
            val previous = dao.getLatestForMemo(
                op.accountId, op.memoName, PendingOpType.UPDATE.name
            )
            if (previous != null && previous.createdAt <= op.createdAt) {
                val mergedMask = (listOfNotNull(previous.updateMask, op.updateMask)
                    .flatMap { it.split(',') }
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
                    .joinToString(","))
                dao.insert(
                    op.copy(
                        id = previous.id,
                        createdAt = previous.createdAt,
                        baseUpdateTime = previous.baseUpdateTime ?: op.baseUpdateTime,
                        updateMask = mergedMask.ifBlank { op.updateMask }
                    )
                )
                return
            }
        }
        dao.insert(op)
    }

    /**
     * Queues [op] and runs [writeLocal] (the optimistic cache write it belongs to) in one
     * database transaction when the database is available.
     *
     * Keeping both writes together is what makes the local-first guarantee hold: a list refresh
     * landing between them could replace the cache with a version that has no card yet, or with
     * the pre-edit content, while the op survives — or leave an op whose card was never written.
     * The cache-protection subqueries only know names that are already queued.
     */
    suspend fun enqueue(op: PendingOp, writeLocal: (suspend () -> Unit)?) {
        if (writeLocal == null) {
            enqueue(op)
            return
        }
        val database = database
        if (database == null) {
            writeLocal()
            enqueue(op)
        } else {
            database.withTransaction {
                writeLocal()
                enqueue(op)
            }
        }
    }

    suspend fun getOps(accountId: String): List<PendingOp> = dao.getOps(accountId)

    suspend fun getOp(id: String): PendingOp? = dao.getOp(id)

    fun getOpsFlow(accountId: String): Flow<List<PendingOp>> = dao.getOpsFlow(accountId)

    suspend fun deleteOp(id: String) = dao.deleteOp(id)

    suspend fun clearForAccount(accountId: String) = dao.deleteOpsForAccount(accountId)

    suspend fun markFailed(
        id: String,
        attemptCount: Int,
        lastError: String?,
        lastAttemptAt: Long,
        permanentlyFailed: Boolean = false
    ) = dao.markFailed(id, attemptCount, lastError, lastAttemptAt, permanentlyFailed)

    suspend fun renameMemo(accountId: String, oldName: String, newName: String) =
        dao.renameMemo(accountId, oldName, newName)

    suspend fun setBaseUpdateTime(id: String, baseUpdateTime: String?) =
        dao.setBaseUpdateTime(id, baseUpdateTime)

    /**
     * Clears the recorded failure of one op so it is eligible again. A permanently
     * failed op is skipped by the background worker, so without this the row would
     * stay "needs attention" until the user discards it.
     */
    suspend fun retryOp(id: String) = dao.clearFailure(id)
}
