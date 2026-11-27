package ink.knowledge.models.vault

import kotlinx.serialization.Serializable

@Serializable
data class VaultIndex(
    val folders: List<String>, // vault-relative, sorted, excludes root
    val files: Map<String, Version>, // vault-relative path -> Version, sorted by path
)
