package ink.knowledge.service.workspace

import ink.knowledge.enums.vault.FileFormat
import ink.knowledge.models.vault.VaultEntry
import ink.knowledge.models.workspace.node.Attachment
import ink.knowledge.models.workspace.node.Base
import ink.knowledge.models.workspace.node.Diagram
import ink.knowledge.models.workspace.node.Folder
import ink.knowledge.models.workspace.link.Link
import ink.knowledge.models.workspace.node.Node
import ink.knowledge.models.workspace.node.Page
import ink.knowledge.models.workspace.PageReference
import ink.knowledge.models.workspace.Workspace
import ink.knowledge.service.vault.FileService
import ink.knowledge.util.LinkUtils

/** Builds the workspace tree and resolved links from the vault files. */
class WorkspaceService(
    private val fileService: FileService,
) {
    // ------ Public read operations ------

    /** Scans the vault and returns the full tree. */
    fun load(): Workspace {
        val entries = fileService.walk()
        val filePaths = entries.filterNot { it.directory }.map { it.path }
        val resolver = LinkResolverService(filePaths)
        val files = filePaths.map { buildFileNode(it, resolver) }
        val name = fileService.rootName()
        return Workspace(name = name, root = buildFolder(path = "", name = name, parent = null, entries, files))
    }

    // ------ Private helpers ------

    private fun buildFileNode(
        path: String,
        resolver: LinkResolverService,
    ): Node {
        val fileName = path.substringAfterLast('/')
        val name = FileFormat.stripSuffix(fileName)
        val parent = parentOf(path)
        val version = fileService.version(path)
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
        entries: List<VaultEntry>,
        files: List<Node>,
    ): Folder {
        val subfolders =
            entries
                .filter { it.directory && parentOf(it.path) == path }
                .map { buildFolder(it.path, it.path.substringAfterLast('/'), path, entries, files) }
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
        val lines = text.lines()
        val bodyStart = if (lines.firstOrNull() == "---") lines.drop(1).indexOf("---") + 2 else 0
        var inFence = false
        for (line in lines.drop(bodyStart)) {
            if (line.startsWith("```") || line.startsWith("~~~")) inFence = !inFence
            if (!inFence && line.startsWith("# ")) return line.removePrefix("# ").trim()
        }
        return null
    }

    private fun parentOf(path: String): String = path.substringBeforeLast('/', missingDelimiterValue = "")

    private fun sortChildren(nodes: List<Node>): List<Node> = nodes.sortedBy { it.name.lowercase() }
}
