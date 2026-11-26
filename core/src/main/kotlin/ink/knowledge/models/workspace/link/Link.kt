package ink.knowledge.models.workspace.link

import ink.knowledge.enums.workspace.LinkKind
import kotlinx.serialization.Serializable

@Serializable
data class Link(
    val source: String,
    val target: String?,
    val raw: String,
    val subpath: String?,
    val alias: String?,
    val kind: LinkKind,
    val start: Int,
    val end: Int,
)
