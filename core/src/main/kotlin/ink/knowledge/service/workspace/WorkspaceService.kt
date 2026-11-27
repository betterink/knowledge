package ink.knowledge.service.workspace

import ink.knowledge.enums.vault.FileFormat
import ink.knowledge.models.vault.VaultIndex
import ink.knowledge.models.vault.Version
import ink.knowledge.models.workspace.PageReference
import ink.knowledge.models.workspace.Workspace
import ink.knowledge.models.workspace.link.Link
import ink.knowledge.models.workspace.node.Attachment
import ink.knowledge.models.workspace.node.Base
import ink.knowledge.models.workspace.node.Diagram
import ink.knowledge.models.workspace.node.Folder
import ink.knowledge.models.workspace.node.Node
import ink.knowledge.models.workspace.node.Page
import ink.knowledge.service.vault.FileService
import ink.knowledge.util.FenceUtils
import ink.knowledge.util.LinkUtils

/** Builds the workspace tree and resolved links from the vault files. */
class WorkspaceService(
    private val fileService: FileService,
) {
    // ------ Public read operations ------

    /** Builds the full tree from the vault index. */
    fun load(index: VaultIndex): Workspace {
        val resolver = LinkResolverService(index.files.keys)
        val files = index.files.map { (path, version) -> buildFileNode(path, version, resolver) }
        val name = fileService.rootName()
        return Workspace(name = name, root = buildFolder(path = "", name = name, parent = null, index.folders, files))
    }

    // ------ Private helpers ------

    private fun buildFileNode(
        path: String,
        version: Version,
        resolver: LinkResolverService,
    ): Node {
        val fileName = path.substringAfterLast('/')
        val name = FileFormat.stripSuffix(fileName)
        val parent = parentOf(path)
        return when (val format = FileFormat.of(fileName)) {
            FileFormat.MARKDOWN -> {
                val text = readText(path)
                Page(path, name, parent, version, titleOf(text) ?: name, links(path, text, resolver))
            }

            FileFormat.EXCALIDRAW -> {
                Diagram(path, name, parent, version, links(path, readText(path), resolver))
            }

            FileFormat.BASE -> {
                Base(path, name, parent, version)
            }

            else -> {
                Attachment(path, name, parent, format, version)
            }
        }
    }

    private fun buildFolder(
        path: String,
        name: String,
        parent: String?,
        folders: List<String>,
        files: List<Node>,
    ): Folder {
        val subfolders =
            folders
                .filter { parentOf(it) == path }
                .map { buildFolder(it, it.substringAfterLast('/'), path, folders, files) }
        val ownFiles = files.filter { it.parent == path }
        val folderNote =
            ownFiles
                .filterIsInstance<Page>()
                .firstOrNull { parent != null && it.name == name }
                ?.let { PageReference(it.path, it.name, it.version) }
        return Folder(path, name, parent, sortChildren(subfolders) + sortChildren(ownFiles), folderNote)
    }

    private fun links(
        path: String,
        text: String,
        resolver: LinkResolverService,
    ): List<Link> =
        LinkUtils.parse(path, text).map { parsed ->
            parsed.link.copy(target = resolver.resolve(path, parsed.linkpath))
        }

    private fun readText(path: String): String = fileService.read(path).decodeToString()

    /** First H1 outside frontmatter and fenced code. */
    private fun titleOf(text: String): String? {
        val lines = text.split('\n')
        val bodyStart = if (lines.firstOrNull() == "---") lines.drop(1).indexOf("---") + 2 else 0
        val fenced = FenceUtils.ranges(text)
        var offset = 0
        for ((index, line) in lines.withIndex()) {
            val inFence = fenced.any { offset in it }
            if (index >= bodyStart && !inFence && line.startsWith("# ")) return line.removePrefix("# ").trim()
            offset += line.length + 1
        }
        return null
    }

    private fun parentOf(path: String): String = path.substringBeforeLast('/', missingDelimiterValue = "")

    private fun sortChildren(nodes: List<Node>): List<Node> = nodes.sortedBy { it.name.lowercase() }
}
