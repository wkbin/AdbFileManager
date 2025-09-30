package model

import kotlinx.serialization.Serializable

@Serializable
data class Bookmark(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val path: String,
    val createdAt: Long = System.currentTimeMillis()
)
