package ink.knowledge.models.workspace.node

import ink.knowledge.enums.vault.FileFormat
import ink.knowledge.models.vault.Version
import ink.knowledge.models.workspace.link.Link
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("diagram")
data class Diagram(
    override val path: String,
    override val name: String,
    override val parent: String?,
    val version: Version,
    val links: List<Link>,
) : Node {
    override val format = FileFormat.EXCALIDRAW
}