package io.github.naharaoss.skpd.resource

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
sealed interface LibraryItem {
    val id: Long
    val name: String
    val creationTime: Instant
    val lastModified: Instant

    @Serializable
    data class Folder(
        override val id: Long,
        override val name: String,
        override val creationTime: Instant,
        override val lastModified: Instant
    ) : LibraryItem

    @Serializable
    data class Document(
        override val id: Long,
        override val name: String,
        override val creationTime: Instant,
        override val lastModified: Instant
    ) : LibraryItem

    companion object {
        val Root = Folder(
            id = -1,
            name = "(root)",
            creationTime = Instant.fromEpochMilliseconds(0L),
            lastModified = Instant.fromEpochMilliseconds(0L)
        )
    }
}