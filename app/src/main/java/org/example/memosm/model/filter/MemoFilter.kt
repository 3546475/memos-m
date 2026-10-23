package org.example.memosm.model.filter

import org.example.memosm.model.Memo

/**
 * The feed's structured filter. A CEL string used to be the only representation the app had; it
 * is now a *compiled* form ([toCel]) while the UI edits this model. Saved views keep their raw
 * CEL in [legacyCel] so a shortcut the app cannot visualise still behaves exactly as before.
 *
 * Tags are stored without the leading `#`. [date] is epoch millis with an exclusive upper bound,
 * mirroring the server's `created_ts` clauses.
 */
data class MemoFilter(
    /** Server shortcut this filter came from, when the user picked a saved view. */
    val viewName: String? = null,
    /** Raw CEL of that view, kept verbatim so unknown syntax keeps working. */
    val legacyCel: List<String> = emptyList(),
    val tags: Set<String> = emptySet(),
    val tagMatch: TagMatch = TagMatch.ALL,
    val date: MemoDateRange? = null,
    /** Ordering of the feed; not a filter condition, but stored and cleared with it. */
    val sort: MemoSort = MemoSort.PINNED_DESC
) {
    /** True when the filter carries an opaque clause that only the server can evaluate. */
    val hasLegacyClause: Boolean
        get() = viewName != null || legacyCel.isNotEmpty()

    /** True when something narrows the result set (sorting alone does not). */
    val hasConditions: Boolean
        get() = hasLegacyClause || tags.isNotEmpty() || date != null

    val isActive: Boolean
        get() = hasConditions || sort != MemoSort.PINNED_DESC

    /**
     * Number of facets in use. A tag set counts once: the chrome badge and the "apply (n)"
     * button describe conditions, not individual tags.
     */
    val activeCount: Int
        get() = listOf(
            hasLegacyClause,
            tags.isNotEmpty(),
            date != null,
            sort != MemoSort.PINNED_DESC
        ).count { it }

    fun withTag(tag: String): MemoFilter {
        val name = normalizeTag(tag)
        return if (name.isEmpty()) this else copy(tags = tags + name)
    }

    fun withoutTag(tag: String): MemoFilter {
        val name = normalizeTag(tag)
        return copy(tags = tags - name)
    }

    /** Drop the saved view and its raw clause, keeping the facets the user added on top. */
    fun withoutView(): MemoFilter = copy(viewName = null, legacyCel = emptyList())

    /**
     * Canonical form stored in the state and persisted: blank tags and stray whitespace in raw
     * clauses would otherwise survive round-trips and compare unequal for no reason.
     */
    fun normalized(): MemoFilter = copy(
        tags = tags.mapNotNull { normalizeTag(it).takeIf(String::isNotEmpty) }.toSet(),
        legacyCel = legacyCel.map { it.trim() }.filter { it.isNotEmpty() }
    )
}

enum class TagMatch { ALL, ANY }

/**
 * Feed ordering. [PINNED_DESC] is the server's default (pinned first, then newest); the other two
 * are plain time order. Kept in the filter so it is persisted and cleared together with it.
 */
enum class MemoSort { PINNED_DESC, NEWEST, OLDEST }

/**
 * A created-time window: [startMillis] inclusive, [endMillisExclusive] exclusive. The open ends
 * are represented by [UNBOUNDED] / zero so a preset ("since last week") needs no second class.
 */
data class MemoDateRange(
    val startMillis: Long = 0L,
    val endMillisExclusive: Long = UNBOUNDED
) {
    fun contains(epochMillis: Long): Boolean =
        epochMillis >= startMillis && epochMillis < endMillisExclusive

    companion object {
        const val UNBOUNDED: Long = Long.MAX_VALUE
    }
}

/** Tags are compared and stored without the `#` the editor types. */
fun normalizeTag(tag: String): String = tag.trim().removePrefix("#").trim()

/**
 * Local evaluation of the filter, used for the cached/prefilled rows and for the offline feed.
 * Opaque clauses are assumed to match: they can only be judged by the server, and hiding rows the
 * device cannot reason about would be worse than showing them until the next fetch.
 */
fun MemoFilter.matches(memo: Memo): Boolean {
    if (hasLegacyClause) return true
    if (tags.isNotEmpty()) {
        val memoTags = memo.tags.orEmpty().mapTo(HashSet(), ::normalizeTag)
        val matched = when (tagMatch) {
            TagMatch.ALL -> tags.all { it in memoTags }
            TagMatch.ANY -> tags.any { it in memoTags }
        }
        if (!matched) return false
    }
    date?.let { range ->
        val created = memo.createTime?.toEpochMilliseconds()
            ?: memo.displayTime?.toEpochMilliseconds()
        if (created != null && !range.contains(created)) return false
    }
    return true
}
