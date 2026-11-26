package ink.knowledge.models.workspace

import ink.knowledge.models.workspace.node.Folder
import kotlinx.serialization.Serializable

@Serializable
data class Workspace(
    val name: String,
    val root: Folder,
)
