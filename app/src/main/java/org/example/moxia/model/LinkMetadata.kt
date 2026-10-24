package org.example.moxia.model

/** Metadata fetched by the Memos server for a webpage. */
data class LinkMetadata(
    val url: String = "",
    val title: String = "",
    val description: String = "",
    val image: String = ""
)
