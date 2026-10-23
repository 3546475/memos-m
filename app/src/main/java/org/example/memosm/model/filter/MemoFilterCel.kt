package org.example.memosm.model.filter

/**
 * Compiles [this] filter into the server's CEL dialect, ANDed with the creator clause that scopes
 * the feed to the signed-in user. Returns null when nothing would be filtered, which keeps the
 * request indistinguishable from an unfiltered feed.
 */
fun MemoFilter.toCel(creatorFilter: String? = null): String? {
    val clauses = buildList {
        creatorFilter?.takeIf { it.isNotBlank() }?.let(::add)
        legacyCel.filter { it.isNotBlank() }.forEach(::add)
        tagClause()?.let(::add)
        date?.toCelClause()?.let(::add)
    }
    return clauses.joinToString(" && ").takeIf { it.isNotBlank() }
}

/**
 * One list-membership clause per tag for ALL, ORed statements for ANY. The `tags` list is used
 * rather than the `tag` variable: it is the form the server documents (`"work" in tags`) and it
 * stays correct when a memo carries several tags, which a scalar `tag` clause would not.
 */
private fun MemoFilter.tagClause(): String? {
    if (tags.isEmpty()) return null
    val clauses = tags.sorted().map { "${celString(it)} in tags" }
    return when (tagMatch) {
        TagMatch.ALL -> clauses.joinToString(" && ")
        TagMatch.ANY -> if (clauses.size == 1) clauses.single() else clauses.joinToString(" || ", "(", ")")
    }
}

private fun MemoDateRange.toCelClause(): String? {
    val parts = buildList {
        if (startMillis > 0L) add("created_ts >= ${startMillis / 1000}")
        if (endMillisExclusive != MemoDateRange.UNBOUNDED) {
            add("created_ts < ${endMillisExclusive / 1000}")
        }
    }
    return parts.joinToString(" && ").takeIf { it.isNotBlank() }
}

/**
 * A CEL string literal. Tags come from user content, so a quote or a backslash inside one used to
 * break the whole expression and the feed failed with a generic load error.
 */
fun celString(value: String): String {
    val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"$escaped\""
}
