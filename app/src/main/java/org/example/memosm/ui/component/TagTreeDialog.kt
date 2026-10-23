package org.example.memosm.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.example.memosm.R
import org.example.memosm.ui.theme.AppDialog
import org.example.memosm.ui.theme.AppTextButton

/** One tag in the tree: [path] is the full `type/book` route, [label] the last segment. */
data class TagNode(
    val path: String,
    val label: String,
    val count: Int,
    val depth: Int,
    val children: List<TagNode>
)

/**
 * Turns the flat `tag -> count` map into a tree on `/`. A parent that only exists because of its
 * children is materialised as a branch with the summed count, so `type/book` still nests under
 * `type` even when no memo carries a bare `type` tag.
 */
internal fun buildTagTree(tags: Map<String, Int>): List<TagNode> {
    // Group by path segments first, then fold the counts up from the leaves.
    val leafCounts = mutableMapOf<String, Int>()
    tags.forEach { (tag, count) ->
        val path = tag.removePrefix("#").trim()
        if (path.isEmpty()) return@forEach
        leafCounts[path] = (leafCounts[path] ?: 0) + count
    }

    // Materialise every ancestor: `a/b/c` implies `a` and `a/b`, which may not be tags of their
    // own. Without this a nested tag fell out of the tree entirely.
    val paths = linkedSetOf<String>()
    leafCounts.keys.forEach { path ->
        var slash = path.indexOf('/')
        while (slash > 0) {
            paths += path.substring(0, slash)
            slash = path.indexOf('/', slash + 1)
        }
        paths += path
    }

    fun node(path: String, depth: Int): TagNode {
        val children = paths
            .asSequence()
            .filter { it.startsWith("$path/") && it.count { ch -> ch == '/' } == depth + 1 }
            .sorted()
            .map { node(it, depth + 1) }
            .toList()
        val ownCount = leafCounts[path] ?: children.sumOf { it.count }
        return TagNode(
            path = path,
            label = path.substringAfterLast('/'),
            count = ownCount,
            depth = depth,
            children = children
        )
    }

    return paths
        .filter { it.count { ch -> ch == '/' } == 0 }
        .sorted()
        .map { node(it, 0) }
}

/** Depth-first rows, skipping the subtrees of any path in [collapsed]. */
internal fun flattenTagTree(nodes: List<TagNode>, collapsed: Set<String>): List<TagNode> {
    val rows = mutableListOf<TagNode>()
    fun visit(node: TagNode) {
        rows += node
        if (node.path !in collapsed) node.children.forEach(::visit)
    }
    nodes.forEach(::visit)
    return rows
}

/** Every path that has children, i.e. the rows "collapse all" should fold. */
internal fun collapsibleTagPaths(nodes: List<TagNode>): Set<String> =
    nodes.flatMap { node ->
        if (node.children.isEmpty()) emptyList() else listOf(node.path) + collapsibleTagPaths(node.children)
    }.toSet()

/**
 * The user's tags as a tree: a tag written `type/book` nests under `type`. Branches fold, and the
 * two buttons fold or unfold the whole tree at once. Picking a tag filters the feed by it.
 */
@Composable
fun TagTreeDialog(
    tags: Map<String, Int>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tree = remember(tags) { buildTagTree(tags) }
    val collapsible = remember(tree) { collapsibleTagPaths(tree) }
    var collapsed by rememberSaveable { mutableStateOf(setOf<String>()) }
    val rows = remember(tree, collapsed) { flattenTagTree(tree, collapsed) }

    AppDialog(
        show = true,
        title = stringResource(R.string.profile_stats_tags),
        onDismissRequest = onDismiss,
        buttons = {
            AppTextButton(
                text = stringResource(
                    if (collapsed.size >= collapsible.size && collapsible.isNotEmpty()) {
                        R.string.tags_expand_all
                    } else {
                        R.string.tags_collapse_all
                    }
                ),
                modifier = Modifier.weight(1f),
                onClick = {
                    collapsed = if (collapsed.size >= collapsible.size && collapsible.isNotEmpty()) {
                        emptySet()
                    } else {
                        collapsible
                    }
                }
            )
            AppTextButton(
                text = stringResource(R.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = onDismiss
            )
        }
    ) {
        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.common_not_available),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(rows, key = { it.path }) { row ->
                    val expandable = row.children.isNotEmpty()
                    val expanded = row.path !in collapsed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // The chevron folds; the label selects. A leaf keeps the same indent with a
                        // blank slot so every tag lines up.
                        Row(
                            modifier = Modifier.width(36.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (expandable) {
                                IconButton(
                                    onClick = {
                                        collapsed = if (expanded) collapsed + row.path
                                        else collapsed - row.path
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChevronRight,
                                        contentDescription = stringResource(
                                            if (expanded) R.string.tags_collapse
                                            else R.string.tags_expand
                                        ),
                                        modifier = Modifier.rotate(if (expanded) 90f else 0f),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelect(row.path) }
                                .padding(
                                    start = (row.depth * 12).dp,
                                    end = 8.dp,
                                    top = 10.dp,
                                    bottom = 10.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${row.path}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = row.count.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
