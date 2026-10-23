package org.example.memosm.viewmodel.delegates

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.example.memosm.data.offline.SessionCacheStore
import org.example.memosm.data.offline.SessionSnapshotData
import org.example.memosm.model.Shortcut
import org.example.memosm.model.filter.MemoFilter
import org.example.memosm.model.filter.normalizeTag
import org.example.memosm.viewmodel.AccountContext
import org.example.memosm.viewmodel.AccountSession
import org.example.memosm.viewmodel.MemosUiState

/**
 * Owns the notes feed's filter. Keeping every mutation here means the state, the request filter
 * and the persisted copy can never disagree, and the UI only says *what* changed instead of
 * rebuilding the whole filter by hand.
 */
interface FilterDelegate {
    /** Replace the filter; a no-op when nothing actually changes. */
    fun apply(filter: MemoFilter)

    /**
     * Pick a saved view (null clears it). A view is a preset: it replaces the base clause and the
     * facets, which is what the old exclusive shortcut/tag pair already implied.
     */
    fun selectView(shortcut: Shortcut?)

    /** Add the tag, or remove it when it is already applied. */
    fun toggleTag(tag: String)

    fun clear()
}

class FilterDelegateImpl(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<MemosUiState>,
    private val sessionCacheStore: SessionCacheStore,
    private val accountSession: AccountSession,
    private val onFilterApplied: (MemoFilter) -> Unit
) : FilterDelegate {

    override fun apply(filter: MemoFilter) {
        val context = accountSession.current ?: return
        val next = filter.normalized()
        if (uiState.value.userMemoList.filter == next) return
        accountSession.update(uiState, context) {
            it.copy(userMemoList = it.userMemoList.copy(filter = next))
        }
        persist(context, next)
        onFilterApplied(next)
    }

    override fun selectView(shortcut: Shortcut?) {
        if (shortcut == null) {
            clear()
            return
        }
        val cel = shortcut.filter?.trim().orEmpty()
        // A view with no expression filters nothing; treating it as "all memos" is less
        // surprising than a checked view that hides nothing.
        if (cel.isEmpty()) {
            clear()
            return
        }
        apply(MemoFilter(viewName = shortcut.name, legacyCel = listOf(cel)))
    }

    override fun toggleTag(tag: String) {
        val name = normalizeTag(tag)
        if (name.isEmpty()) return
        val current = uiState.value.userMemoList.filter
        apply(if (name in current.tags) current.withoutTag(name) else current.withTag(name))
    }

    override fun clear() = apply(MemoFilter())

    /**
     * Persist into the session snapshot next to the shortcuts, so the feed reopens with the same
     * filter after a cold start. Read-modify-write: the snapshot also holds the profile data other
     * writers refresh.
     */
    private fun persist(context: AccountContext, filter: MemoFilter) {
        if (!accountSession.isCurrent(context)) return
        val accountId = context.account.id
        scope.launch {
            runCatching {
                val current = sessionCacheStore.get(accountId) ?: SessionSnapshotData()
                sessionCacheStore.save(accountId, current.copy(filter = filter))
            }
        }
    }
}
