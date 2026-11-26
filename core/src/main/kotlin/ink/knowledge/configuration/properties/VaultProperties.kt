package ink.knowledge.configuration.properties

import kotlinx.serialization.Serializable

@Serializable
data class VaultProperties(
    val root: String,
    val excluded: Set<String>,
)
