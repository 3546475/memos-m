package org.example.memosm.model.filter

import com.google.gson.Gson
import kotlin.time.Instant
import org.example.memosm.model.Memo
import org.example.memosm.model.MemoContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoFilterTest {

    private fun memo(tags: List<String> = emptyList(), created: Long = 1_700_000_000_000L) = Memo(
        content = "hello",
        tags = tags,
        createTime = Instant.fromEpochMilliseconds(created)
    )

    @Test
    fun `an empty filter compiles to the creator clause alone`() {
        assertEquals("creator == \"users/1\"", MemoFilter().toCel("creator == \"users/1\""))
        assertNull(MemoFilter().toCel(null))
        assertNull(MemoFilter().toCel("   "))
    }

    @Test
    fun `a saved view keeps its raw clause after the creator`() {
        val view = MemoFilter(viewName = "users/1/shortcuts/a", legacyCel = listOf("\"work\" in tags"))
        assertEquals(
            "creator == \"users/1\" && \"work\" in tags && \"home\" in tags",
            view.copy(tags = setOf("home")).toCel("creator == \"users/1\"")
        )
    }

    @Test
    fun `tag clauses escape quotes and backslashes and respect the match mode`() {
        assertEquals(
            "\"a\\\"b\" in tags && \"c\\\\d\" in tags",
            MemoFilter(tags = setOf("a\"b", "c\\d")).toCel(null)
        )
        assertEquals(
            "(\"a\\\"b\" in tags || \"c\\\\d\" in tags)",
            MemoFilter(tags = setOf("a\"b", "c\\d"), tagMatch = TagMatch.ANY).toCel(null)
        )
        assertEquals(
            "\"work\" in tags",
            MemoFilter(tags = setOf("work"), tagMatch = TagMatch.ANY).toCel(null)
        )
    }

    @Test
    fun `a date window compiles to created timestamps in seconds`() {
        val filter = MemoFilter(
            date = MemoDateRange(
                startMillis = 1_700_000_000_000L,
                endMillisExclusive = 1_700_086_400_000L
            )
        )
        assertEquals("created_ts >= 1700000000 && created_ts < 1700086400", filter.toCel(null))
    }

    @Test
    fun `an open-ended window omits its missing side`() {
        assertEquals(
            "created_ts >= 1700000000",
            MemoFilter(date = MemoDateRange(startMillis = 1_700_000_000_000L)).toCel(null)
        )
        assertEquals(
            "created_ts < 1700086400",
            MemoFilter(date = MemoDateRange(endMillisExclusive = 1_700_086_400_000L)).toCel(null)
        )
    }

    @Test
    fun `local matching mirrors the tag mode`() {
        val target = memo(tags = listOf("work", "#type/book"))
        assertTrue(MemoFilter(tags = setOf("work", "type/book")).matches(target))
        assertFalse(MemoFilter(tags = setOf("work", "home")).matches(target))
        assertTrue(
            MemoFilter(tags = setOf("work", "home"), tagMatch = TagMatch.ANY).matches(target)
        )
    }

    @Test
    fun `local matching respects the created time window`() {
        val target = memo(created = 1_700_000_000_000L)
        val day = MemoDateRange(
            startMillis = 1_699_999_000_000L,
            endMillisExclusive = 1_700_001_000_000L
        )
        assertTrue(MemoFilter(date = day).matches(target))
        assertFalse(MemoFilter(date = MemoDateRange(startMillis = 1_700_000_000_001L)).matches(target))
        assertFalse(MemoFilter(date = MemoDateRange(endMillisExclusive = 1_700_000_000_000L)).matches(target))
    }

    @Test
    fun `an opaque view clause is never filtered locally`() {
        val view = MemoFilter(viewName = "users/1/shortcuts/a", legacyCel = listOf("pinned"))
        assertTrue(view.matches(memo(tags = listOf("other"))))
    }

    @Test
    fun `active facets count once per kind`() {
        assertFalse(MemoFilter().isActive)
        assertEquals(0, MemoFilter().activeCount)
        val filter = MemoFilter(
            viewName = "v",
            legacyCel = listOf("x"),
            tags = setOf("a", "b"),
            date = MemoDateRange(startMillis = 1L),
            sort = MemoSort.OLDEST
        )
        assertTrue(filter.isActive)
        assertEquals(4, filter.activeCount)
        assertEquals(MemoFilter(tags = filter.tags, date = filter.date, sort = filter.sort), filter.withoutView())
    }

    @Test
    fun `sort is a preference that never reaches the expression`() {
        val filter = MemoFilter(sort = MemoSort.OLDEST)
        assertNull(filter.toCel(null))
        assertTrue(filter.isActive)
        assertEquals(1, filter.activeCount)
        assertTrue(filter.matches(memo()))
        assertEquals(MemoFilter(), filter.copy(sort = MemoSort.PINNED_DESC))
    }

    @Test
    fun `content types compile to server clauses and match locally`() {
        val link = Memo(content = "see https://example.com", createTime = Instant.fromEpochMilliseconds(1L))
        val todo = Memo(content = "- [ ] task", createTime = Instant.fromEpochMilliseconds(1L))
        val plain = memo()

        assertEquals("has_link", MemoFilter(types = setOf(MemoContentType.LINK)).toCel(null))
        assertEquals(
            "(has_link || has_task_list)",
            MemoFilter(types = setOf(MemoContentType.LINK, MemoContentType.TODO)).toCel(null)
        )
        assertEquals(
            "content.contains(\"`\")",
            MemoFilter(types = setOf(MemoContentType.CODE)).toCel(null)
        )

        assertTrue(MemoFilter(types = setOf(MemoContentType.LINK)).matches(link))
        assertTrue(MemoFilter(types = setOf(MemoContentType.TODO)).matches(todo))
        assertFalse(MemoFilter(types = setOf(MemoContentType.LINK)).matches(plain))
        assertTrue(
            MemoFilter(types = setOf(MemoContentType.LINK, MemoContentType.TODO)).matches(todo)
        )
    }

    @Test
    fun `a date window excludes memos with no known time`() {
        val unknown = Memo(content = "no timestamps")
        assertFalse(
            MemoFilter(date = MemoDateRange(startMillis = 1L)).matches(unknown)
        )
    }

    @Test
    fun `tags lose their hash and whitespace`() {
        assertEquals("work", normalizeTag(" #work "))
        assertEquals("type/book", normalizeTag("# type/book "))
        assertEquals(setOf("a", "b"), MemoFilter(tags = setOf(" #a ", "", "b")).normalized().tags)
    }

    @Test
    fun `gson round trip keeps the filter`() {
        val filter = MemoFilter(
            viewName = "users/1/shortcuts/a",
            legacyCel = listOf("pinned"),
            tags = setOf("a", "b"),
            tagMatch = TagMatch.ANY,
            date = MemoDateRange(1L, 2L),
            sort = MemoSort.NEWEST
        )
        val gson = Gson()
        assertEquals(filter, gson.fromJson(gson.toJson(filter), MemoFilter::class.java))
    }
}
