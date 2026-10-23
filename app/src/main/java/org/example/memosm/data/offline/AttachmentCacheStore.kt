package org.example.memosm.data.offline

import org.example.memosm.data.cache.CacheListType
import org.example.memosm.data.cache.MemoCacheRepository
import org.example.memosm.data.media.AttachmentCacheManager
import org.example.memosm.data.store.RoomAttachmentMetaStore
import org.example.memosm.model.Attachment

/**
 * Cache-abstraction facade for attachments. Business code calls this; the
 * pieces below it keep the on-disk/on-db location an implementation detail:
 * [AttachmentCacheManager] owns the downloaded binaries, [RoomAttachmentMetaStore]
 * the metadata table, and [MemoCacheRepository] the cached memos the metadata
 * backfill harvests from.
 *
 * The metadata half powers the offline Resources page: successful online
 * list fetches are persisted here, and when the table is still empty (e.g.
 * right after the v8 migration) it is backfilled by harvesting the
 * attachments embedded in the cached memos.
 */
class AttachmentCacheStore(
    private val memoCacheRepository: MemoCacheRepository,
    private val meta: RoomAttachmentMetaStore,
    private val attachmentCacheManager: AttachmentCacheManager
) {

    /** Accounts whose first page has been seen: their meta table is authoritative. */
    private val syncedAccounts: MutableSet<String> =
        java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap())

    /** Whether the attachment is available locally. */
    suspend fun exists(accountId: String, attachmentName: String): Boolean =
        attachmentCacheManager.getLocalFile(accountId, attachmentName) != null

    // --- Attachment metadata ---

    /**
     * Persist the metadata of a fetched attachment page. Always merges
     * (upsert): server pages do not say anything about attachments outside
     * the page, so a refresh must not wipe the rows of attachments that
     * simply fell off the first page.
     */
    suspend fun cacheMeta(accountId: String, attachments: List<Attachment>) =
        meta.upsertAll(accountId, attachments)

    /**
     * Store a fresh first page and forget the rows inside its window that the server dropped.
     * The merge-only [cacheMeta] cannot notice a deletion, which is why the Resources page kept
     * showing attachments that no longer existed.
     */
    suspend fun replaceRecentMeta(accountId: String, attachments: List<Attachment>) {
        syncedAccounts += accountId
        meta.replaceRecent(accountId, attachments)
    }

    /**
     * Cached attachment metadata for [accountId], newest first. When the
     * meta table has no rows for the account yet (fresh install or right
     * after the v8 migration), it is backfilled from the attachments
     * embedded in the cached memos and the harvest is persisted so the
     * next read hits the table directly.
     */
    suspend fun getCachedMeta(accountId: String, limit: Int? = null): List<Attachment> {
        // Only backfill before the server has told us what exists: harvesting afterwards would
        // resurrect the attachments the fetch just pruned (cached memos still reference them).
        if (accountId !in syncedAccounts && meta.count(accountId) == 0) {
            val harvested = harvestFromCachedMemos(accountId)
            if (harvested.isNotEmpty()) meta.upsertAll(accountId, harvested)
        }
        return meta.getAll(accountId, limit)
    }

    /** Forget one attachment's metadata after it was deleted on the server. */
    suspend fun removeMeta(accountId: String, attachmentName: String) =
        meta.deleteOne(accountId, attachmentName)

    /** Remove all attachment metadata of an account (when it is removed). */
    suspend fun clearMeta(accountId: String) {
        syncedAccounts -= accountId
        meta.clear(accountId)
    }

    /**
     * Collect attachment metadata from the cached memos of an account (the
     * `attachments` list embedded in each memo's stored JSON). Across the
     * USER/ARCHIVED/EXPLORE buckets the same attachment can appear more
     * than once; dedupe by attachment name, then order newest first.
     */
    private suspend fun harvestFromCachedMemos(accountId: String): List<Attachment> {
        val byName = LinkedHashMap<String, Attachment>()
        for (listType in listOf(CacheListType.USER, CacheListType.ARCHIVED, CacheListType.EXPLORE)) {
            for (memo in memoCacheRepository.getCachedMemos(accountId, listType)) {
                for (attachment in memo.attachments.orEmpty()) {
                    val name = attachment.name ?: continue
                    byName.putIfAbsent(name, attachment)
                }
            }
        }
        return byName.values.sortedByDescending { it.createTime?.toEpochMilliseconds() ?: 0L }
    }
}
