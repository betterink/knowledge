package ink.knowledge.models.workspace.node

import ink.knowledge.enums.vault.FileFormat
import ink.knowledge.models.vault.Version
import ink.knowledge.models.workspace.PageReference
import ink.knowledge.models.workspace.link.Link
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Node {
    val path: String
    val name: String
    val format: FileFormat
    val parent: String?
}




