package org.example.memosm.ui.component.filter

import org.example.memosm.ui.component.TagNode

/**
 * Prunes [nodes] down to the branches that contain [query]. A node whose path matches keeps its
 * whole subtree (the tree shows it expanded while searching); a non-matching ancestor is kept only
 * as the way down to its matching descendants. An empty query returns the tree untouched.
 */
internal fun filterTagTree(nodes: List<TagNode>, query: String): List<TagNode> {
    val needle = query.trim()
    if (needle.isEmpty()) return nodes

    fun prune(node: TagNode): TagNode? {
        val children = node.children.mapNotNull(::prune)
        return when {
            node.path.contains(needle, ignoreCase = true) -> node
            children.isNotEmpty() -> node.copy(children = children)
            else -> null
        }
    }

    return nodes.mapNotNull(::prune)
}

/**
 * True when [tags] contains a path below [path]. The tree uses it to mark a branch that is not
 * itself selected but whose subtree has selections, without bringing checkboxes back.
 */
internal fun hasSelectedDescendant(tags: Set<String>, path: String): Boolean =
    tags.any { it.startsWith("$path/") }
