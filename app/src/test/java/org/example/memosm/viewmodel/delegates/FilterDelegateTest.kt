package org.example.memosm.viewmodel.delegates

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.example.memosm.account.stub
import org.example.memosm.api.MemosApi
import org.example.memosm.data.offline.SessionCacheStore
import org.example.memosm.data.offline.SessionSnapshotData
import org.example.memosm.data.store.SnapshotStore
import org.example.memosm.model.Account
import org.example.memosm.model.filter.MemoFilter
import org.example.memosm.model.filter.MemoSort
import org.example.memosm.viewmodel.AccountSession
import org.example.memosm.viewmodel.MemosUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilterDelegateTest {

    private class FakeSnapshotStore : SnapshotStore<SessionSnapshotData> {
        val values = mutableMapOf<String, SessionSnapshotData>()

        override suspend fun get(accountId: String): SessionSnapshotData? = values[accountId]

        override suspend fun save(accountId: String, value: SessionSnapshotData) {
            values[accountId] = value
        }

        override suspend fun clear(accountId: String) {
            values.remove(accountId)
        }
    }

    @Test
    fun `apply stores the filter, reports it and persists it`() = runTest {
        val store = FakeSnapshotStore()
        val session = AccountSession(backgroundScope)
        session.activate(Account(id = "A"), stub<MemosApi> { name, _ -> error(name) }, OkHttpClient())
        val state = MutableStateFlow(MemosUiState())
        val applied = mutableListOf<MemoFilter>()
        val delegate = FilterDelegateImpl(
            backgroundScope,
            state,
            SessionCacheStore(store),
            session
        ) { applied += it }

        delegate.apply(MemoFilter(tags = setOf(" #work ")))
        runCurrent()

        assertEquals(setOf("work"), state.value.userMemoList.filter.tags)
        assertEquals(1, applied.size)
        assertEquals(setOf("work"), store.values["A"]?.filter?.tags)
    }

    @Test
    fun `applying the same filter twice neither refetches nor rewrites`() = runTest {
        val store = FakeSnapshotStore()
        val session = AccountSession(backgroundScope)
        session.activate(Account(id = "A"), stub<MemosApi> { name, _ -> error(name) }, OkHttpClient())
        val state = MutableStateFlow(MemosUiState())
        val applied = mutableListOf<MemoFilter>()
        val delegate = FilterDelegateImpl(
            backgroundScope,
            state,
            SessionCacheStore(store),
            session
        ) { applied += it }

        val filter = MemoFilter(tags = setOf("work"))
        delegate.apply(filter)
        delegate.apply(filter.copy(tags = setOf(" work ")).normalized())
        runCurrent()

        assertEquals(1, applied.size)
    }

    @Test
    fun `toggle tag adds then removes without touching the view clause`() = runTest {
        val session = AccountSession(backgroundScope)
        session.activate(Account(id = "A"), stub<MemosApi> { name, _ -> error(name) }, OkHttpClient())
        val state = MutableStateFlow(MemosUiState())
        val delegate = FilterDelegateImpl(
            backgroundScope,
            state,
            SessionCacheStore(FakeSnapshotStore()),
            session
        ) {}
        delegate.apply(MemoFilter(viewName = "users/1/shortcuts/v", legacyCel = listOf("pinned")))

        delegate.toggleTag("#work")
        assertEquals(setOf("work"), state.value.userMemoList.filter.tags)
        assertEquals("users/1/shortcuts/v", state.value.userMemoList.filter.viewName)

        delegate.toggleTag("work")
        assertTrue(state.value.userMemoList.filter.tags.isEmpty())
        assertEquals("users/1/shortcuts/v", state.value.userMemoList.filter.viewName)
    }

    @Test
    fun `clear removes every condition and the sort`() = runTest {
        val session = AccountSession(backgroundScope)
        session.activate(Account(id = "A"), stub<MemosApi> { name, _ -> error(name) }, OkHttpClient())
        val state = MutableStateFlow(MemosUiState())
        val delegate = FilterDelegateImpl(
            backgroundScope,
            state,
            SessionCacheStore(FakeSnapshotStore()),
            session
        ) {}

        delegate.apply(
            MemoFilter(tags = setOf("work"), sort = MemoSort.OLDEST)
        )
        delegate.clear()

        assertEquals(MemoFilter(), state.value.userMemoList.filter)
    }
}
