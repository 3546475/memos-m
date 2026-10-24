package org.example.moxia.data.media

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Durable local source for an attachment upload that has not been acknowledged. */
@Entity(
    tableName = "attachment_uploads",
    indices = [Index("accountId"), Index(value = ["accountId", "clientId"], unique = true)]
)
data class AttachmentUpload(
    @PrimaryKey val id: String,
    val accountId: String,
    /** Stable id sent as the API attachmentId for retry-safe reconciliation. */
    val clientId: String,
    val localPath: String,
    val filename: String,
    val mimeType: String,
    val size: Long,
    val createdAt: Long,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val lastAttemptAt: Long = 0L,
    /**
     * True once a replay gave up (server rejection or too many attempts). The row and its staged
     * bytes are kept so the sync panel can show what was not uploaded and discard it explicitly;
     * a background pass never retries it.
     */
    val permanentlyFailed: Boolean = false
)
