package ink.knowledge.models.vault

import kotlinx.serialization.Serializable

@Serializable
data class Version(
    val hash: String,
    val size: Long,
    val modified: Long,
)
