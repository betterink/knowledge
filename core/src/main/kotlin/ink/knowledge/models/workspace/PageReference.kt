package ink.knowledge.models.workspace

import ink.knowledge.models.vault.Version
import kotlinx.serialization.Serializable

@Serializable
data class PageReference(
    val path: String,
    val name: String,
    val version: Version,
)
