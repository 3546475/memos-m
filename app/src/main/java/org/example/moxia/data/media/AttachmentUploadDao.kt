package org.example.moxia.data.media

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AttachmentUploadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(upload: AttachmentUpload)

    @Query("SELECT * FROM attachment_uploads WHERE accountId = :accountId ORDER BY createdAt ASC")
    suspend fun getForAccount(accountId: String): List<AttachmentUpload>

    /** Rows a replay pass may still upload; permanently failed rows are excluded. */
    @Query("SELECT * FROM attachment_uploads WHERE accountId = :accountId AND permanentlyFailed = 0 ORDER BY createdAt ASC")
    suspend fun getPendingForAccount(accountId: String): List<AttachmentUpload>

    /** Rows a replay pass gave up on, kept so the sync panel can show them. */
    @Query("SELECT * FROM attachment_uploads WHERE accountId = :accountId AND permanentlyFailed = 1 ORDER BY createdAt ASC")
    suspend fun getFailedForAccount(accountId: String): List<AttachmentUpload>

    @Query("SELECT * FROM attachment_uploads WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AttachmentUpload?

    @Query("DELETE FROM attachment_uploads WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE attachment_uploads SET attemptCount = :attemptCount, lastError = :lastError, lastAttemptAt = :lastAttemptAt WHERE id = :id")
    suspend fun markFailed(id: String, attemptCount: Int, lastError: String?, lastAttemptAt: Long)

    @Query(
        "UPDATE attachment_uploads SET attemptCount = :attemptCount, lastError = :lastError, " +
            "lastAttemptAt = :lastAttemptAt, permanentlyFailed = 1 WHERE id = :id"
    )
    suspend fun markPermanentlyFailed(id: String, attemptCount: Int, lastError: String?, lastAttemptAt: Long)

    @Query("DELETE FROM attachment_uploads WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}
