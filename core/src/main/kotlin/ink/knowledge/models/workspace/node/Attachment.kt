package ink.knowledge.models.workspace.node

import ink.knowledge.enums.vault.FileFormat
import ink.knowledge.models.vault.Version
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("attachment")
data class Attachment(
    override val path: String,
    override val name: String,
    override val parent: String?,
    override val format: FileFormat,
    val version: Version,
) : Node
