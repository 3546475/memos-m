package org.example.memosm.data.backup

import com.google.gson.JsonParser
import java.io.File
import org.example.memosm.data.sync.PendingOpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Restore of backed-up queued edits: an edit that can be replayed faithfully becomes a live
 * outbox row (with a clean retry budget), while an edit that still depends on an attachment the
 * server never received stays a review record.
 */
class QueuedEditRestoreTest {

    private val root = File("restored-attachments")

    @Test
    fun `a plain edit becomes a live outbox row`() {
        val edit = JsonParser.parseString(
            """
            {"id":"op-1","type":"UPDATE","memoName":"memos/1","updateMask":"content",
             "baseUpdateTime":"2024-01-01T00:00:00Z","createdAt":123,
             "payload":{"content":"edited"}}
            """.trimIndent()
        ).asJsonObject

        val op = livePendingOp("acc-1", edit, root)!!

        assertEquals("op-1", op.id)
        assertEquals("acc-1", op.accountId)
        assertEquals(PendingOpType.UPDATE.name, op.type)
        assertEquals("memos/1", op.memoName)
        assertEquals("content", op.updateMask)
        assertEquals("2024-01-01T00:00:00Z", op.baseUpdateTime)
        assertEquals(123L, op.createdAt)
        // New work for this installation: no failure history carried over.
        assertEquals(0, op.attemptCount)
        assertFalse(op.permanentlyFailed)
    }

    @Test
    fun `attachment paths are pointed at the restored files`() {
        val edit = JsonParser.parseString(
            """
            {"id":"op-2","type":"UPDATE","memoName":"memos/2","createdAt":1,
             "payload":{"content":"x","attachments":[
               {"name":"attachments/9","filename":"a.png","type":"image/png",
                "localPath":"blob:blob-1"}]}}
            """.trimIndent()
        ).asJsonObject

        val op = livePendingOp("acc-1", edit, root)!!
        val attachment = JsonParser.parseString(op.payloadJson!!)
            .asJsonObject.getAsJsonArray("attachments")[0].asJsonObject

        assertEquals(
            File(root, "blob-1").absolutePath,
            attachment.get("localPath").asString
        )
        // The server name is preserved: the replayed memo references the existing attachment.
        assertEquals("attachments/9", attachment.get("name").asString)
    }

    @Test
    fun `an edit that depends on an unuploaded attachment stays reviewable`() {
        val edit = JsonParser.parseString(
            """
            {"id":"op-3","type":"CREATE","memoName":"offline-client-3","createdAt":1,
             "payload":{"content":"x","attachments":[
               {"filename":"a.png","type":"image/png","localPath":"blob:blob-1"}]}}
            """.trimIndent()
        ).asJsonObject

        // No server name: the upload queue (and its staged bytes) is not part of the archive.
        assertNull(livePendingOp("acc-1", edit, root))
    }

    @Test
    fun `a deletion needs no payload and is always replayable`() {
        val edit = JsonParser.parseString(
            """{"id":"op-4","type":"DELETE","memoName":"memos/4","createdAt":7}"""
        ).asJsonObject

        val op = livePendingOp("acc-1", edit, root)!!

        assertEquals(PendingOpType.DELETE.name, op.type)
        assertNull(op.payloadJson)
    }

    @Test
    fun `an unknown operation type is not replayed`() {
        val edit = JsonParser.parseString(
            """{"id":"op-5","type":"SOMETHING_ELSE","createdAt":1}"""
        ).asJsonObject

        assertNull(livePendingOp("acc-1", edit, root))
    }
}
