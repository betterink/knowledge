package ink.knowledge.models.workspace.node

import ink.knowledge.enums.vault.FileFormat
import ink.knowledge.models.workspace.PageReference
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("folder")
data class Folder(
    override val path: String,
    override val name: String,
    override val parent: String?,
    val children: List<Node>,
    val folderNote: PageReference?,
) : Node {
    override val format = FileFormat.FOLDER
}