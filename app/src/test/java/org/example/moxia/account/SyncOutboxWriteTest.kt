package org.example.moxia.account

import kotlinx.coroutines.test.runTest
import org.example.moxia.data.sync.PendingOp
import org.example.moxia.data.sync.PendingOpDao
import org.example.moxia.data.sync.PendingOpType
import org.example.moxia.data.sync.SyncRepository
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Contract of [SyncRepository.enqueue] with a local write: the optimistic cache write and the
 * outbox insert belong to the same unit of work. With a database they share one transaction; the
 * fallback used by these tests (no database) still has to preserve the order, because the card
 * must exist before a replay can pick the op up.
 */
class SyncOutboxWriteTest {

    @Test
    fun `optimistic write runs before its operation is queued`() = runTest {
        val order = mutableListOf<String>()
        val dao = stub<PendingOpDao> { name, _ ->
            when (name) {
                "getLatestForMemo" -> null
                "insert" -> { order += "op" }
                else -> error(name)
            }
        }
        val repository = SyncRepository(dao)

        repository.enqueue(
            PendingOp.new(
                accountId = "acc-1",
                type = PendingOpType.CREATE,
                memoName = "offline-client-1",
                payloadJson = "{}"
            ),
            writeLocal = { order += "cache" }
        )

        assertEquals(listOf("cache", "op"), order)
    }

    @Test
    fun `queueing without a local write keeps the create path unchanged`() = runTest {
        val inserted = mutableListOf<PendingOp>()
        val dao = stub<PendingOpDao> { name, args ->
            when (name) {
                "insert" -> { inserted += args[0] as PendingOp }
                else -> error(name)
            }
        }
        val repository = SyncRepository(dao)
        val op = PendingOp.new("acc-1", PendingOpType.DELETE, "memos/1")

        repository.enqueue(op)

        assertEquals(listOf(op), inserted)
    }

    /**
     * Consecutive offline edits still collapse into one UPDATE row, even when each edit is
     * queued through the transactional overload.
     */
    @Test
    fun `consecutive edits merge inside the queued write`() = runTest {
        var latest: PendingOp? = null
        val inserted = mutableListOf<PendingOp>()
        val dao = stub<PendingOpDao> { name, args ->
            when (name) {
                "getLatestForMemo" -> latest
                "insert" -> {
                    inserted += args[0] as PendingOp
                    latest = args[0] as PendingOp
                }
                else -> error(name)
            }
        }
        val repository = SyncRepository(dao)
        val first = PendingOp.new(
            accountId = "acc-1",
            type = PendingOpType.UPDATE,
            memoName = "memos/1",
            payloadJson = "{\"content\":\"first\"}",
            updateMask = "content",
            baseUpdateTime = "2024-01-01T00:00:00Z"
        )
        val second = first.copy(
            id = "other",
            createdAt = first.createdAt + 10,
            payloadJson = "{\"content\":\"second\"}",
            updateMask = "visibility",
            baseUpdateTime = "2024-01-02T00:00:00Z"
        )

        repository.enqueue(first)
        repository.enqueue(second, writeLocal = {})

        assertEquals(2, inserted.size)
        val merged = inserted.last()
        // Same row, newest payload, union mask, original base version.
        assertEquals(first.id, merged.id)
        assertEquals(first.createdAt, merged.createdAt)
        assertEquals("{\"content\":\"second\"}", merged.payloadJson)
        assertEquals("content,visibility", merged.updateMask)
        assertEquals("2024-01-01T00:00:00Z", merged.baseUpdateTime)
    }
}
