package org.example.moxia.data.cache

/** The list type a cached memo is filed under, used by archive-state reconciliation. */
data class CachedMemoState(
    val name: String,
    val listType: String
)
