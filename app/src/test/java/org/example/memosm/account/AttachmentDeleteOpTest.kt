package org.example.memosm.account

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.example.memosm.api.MemosApi
import org.example.memosm.data.audit.SyncAuditDao
import org.example.memosm.data.audit.SyncAuditLogger
import org.example.memosm.data.cache.MemoCacheRepository
import org.example.memosm.data.cache.MemoDao
import org.example.memosm.data.sync.OpReplayExecutor
import org.example.memosm.data.sync.PendingOp
import org.example.memosm.data.sync.PendingOpDao
import org.example.memosm.data.sync.PendingOpType
import org.example.memosm.data.sync.SyncRepository
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Replay semantics of the offline attachment deletion (Resources page). The local rows were
 * already removed when the op was queued, so the only question is how the server call is
 * classified: "already gone" is success, anything else is a real failure that stays queued.
 */
class AttachmentDeleteOpTest {

    @Test
    fun `a 404 counts as deleted`() = runTest {
        val executor = executor(failure = httpError(404))

        assertTrue(executor.replay(deleteOp()))
    }

    @Test
    fun `a server error keeps the operation queued`() = runTest {
        val executor = executor(failure = httpError(500))

        try {
            executor.replay(deleteOp())
            fail("A server error must propagate so the op is retried")
        } catch (_: HttpException) {
        }
    }

    private fun deleteOp() =
        PendingOp.new("acc-1", PendingOpType.ATTACHMENT_DELETE, "attachments/9")

    private fun executor(failure: Exception) = OpReplayExecutor(
        api = stub<MemosApi> { name, _ ->
            check(name == "deleteAttachment") { name }
            throw failure
        },
        repository = SyncRepository(stub<PendingOpDao> { name, _ -> error(name) }),
        memoCacheRepository = MemoCacheRepository(stub<MemoDao> { name, _ -> error(name) }),
        auditLogger = SyncAuditLogger(stub<SyncAuditDao> { name, _ ->
            if (name == "count") 0 else Unit
        }),
        accountId = "acc-1"
    )

    private fun httpError(code: Int) = HttpException(
        Response.error<Any>(code, """{"message":"error"}""".toResponseBody("application/json".toMediaType()))
    )
}
