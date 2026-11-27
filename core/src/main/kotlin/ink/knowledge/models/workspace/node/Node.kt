package ink.knowledge.models.workspace.node

import ink.knowledge.enums.vault.FileFormat
import kotlinx.serialization.Serializable

@Serializable
sealed interface Node {
    val path: String
    val name: String
    val format: FileFormat
    val parent: String?
}
