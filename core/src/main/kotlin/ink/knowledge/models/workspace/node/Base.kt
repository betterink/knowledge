package ink.knowledge.models.workspace.node

import ink.knowledge.enums.vault.FileFormat
import ink.knowledge.models.vault.Version
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("base")
data class Base(
    override val path: String,
    override val name: String,
    override val parent: String?,
    val version: Version,
) : Node {
    override val format = FileFormat.BASE
}
