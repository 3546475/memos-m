package org.example.memosm.model

/**
 * The kinds of memo the server counts in `userStats.memoTypeStats`. The server does not expose a
 * filter for them, so a list of one kind has to be picked out of the memos this device has loaded
 * — see [matchesContentType]. The counts on the stat card come from the server and can therefore be
 * higher than the number of rows such a list can show.
 */
enum class MemoContentType { LINK, CODE, TODO }

private val LinkPattern = Regex("""(https?://|www\.)\S+""")
private val CodeFencePattern = Regex("```")
private val InlineCodePattern = Regex("""`[^`\n]+`""")
private val TodoPattern = Regex("""(?m)^\s*[-*+]\s+\[[ xX]]""")

fun Memo.matchesContentType(type: MemoContentType): Boolean = when (type) {
    MemoContentType.LINK -> LinkPattern.containsMatchIn(content)
    MemoContentType.CODE -> CodeFencePattern.containsMatchIn(content) ||
        InlineCodePattern.containsMatchIn(content)
    MemoContentType.TODO -> TodoPattern.containsMatchIn(content)
}
