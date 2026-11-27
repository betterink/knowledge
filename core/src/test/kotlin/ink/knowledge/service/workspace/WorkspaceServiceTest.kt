package ink.knowledge.service.workspace

import ink.knowledge.models.workspace.Workspace
import ink.knowledge.models.workspace.node.Attachment
import ink.knowledge.models.workspace.node.Base
import ink.knowledge.models.workspace.node.Diagram
import ink.knowledge.models.workspace.node.Folder
import ink.knowledge.models.workspace.node.Node
import ink.knowledge.models.workspace.node.Page
import ink.knowledge.service.vault.FileService
import ink.knowledge.util.factory.VaultFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class WorkspaceServiceTest {
    private val workspace = load(VaultFactory.create())

    private fun load(root: Path): Workspace {
        val fileService = FileService(VaultFactory.properties(root))
        return WorkspaceService(fileService).load(fileService.index())
    }

    private fun node(path: String): Node = flatten(workspace.root).single { it.path == path }

    private fun flatten(folder: Folder): List<Node> = folder.children.flatMap { if (it is Folder) listOf(it) + flatten(it) else listOf(it) }

    @Test
    fun rootListsFoldersFirstThenFilesByName() {
        assertEquals(listOf("A", "B", "Empty", "Projects", "Home", "Notes"), workspace.root.children.map { it.name })
        assertNull(workspace.root.parent)
        assertEquals("", node("Home.md").parent)
    }

    @Test
    fun mapsFormatsToNodeKinds() {
        assertIs<Page>(node("Home.md"))
        assertIs<Diagram>(node("Projects/Diagram.excalidraw.md"))
        assertIs<Base>(node("Notes.base"))
        assertIs<Attachment>(node("Projects/image.png"))
        assertEquals("Diagram", node("Projects/Diagram.excalidraw.md").name)
    }

    @Test
    fun titleIsFirstHeadingOutsideFrontmatter() {
        assertEquals("Home Page", (node("Home.md") as Page).title)
        assertEquals("Same", (node("A/Same.md") as Page).title)
    }

    @Test
    fun titleSkipsHeadingInsideMismatchedFence() {
        val root = VaultFactory.create()
        Files.writeString(root.resolve("Fenced.md"), "```\n~~~\n# Hidden\n```\n# Actual\n")
        val loaded = load(root)
        assertEquals("Actual", (flatten(loaded.root).single { it.path == "Fenced.md" } as Page).title)
    }

    @Test
    fun folderNoteIsSameNamePage() {
        assertEquals("Projects/Projects.md", (node("Projects") as Folder).folderNote?.path)
        assertNull((node("A") as Folder).folderNote)
    }

    @Test
    fun linksAreResolved() {
        val home = (node("Home.md") as Page).links.map { it.target }
        assertEquals(listOf("Notes.base", null, "Home.md"), home)

        val projects = (node("Projects/Projects.md") as Page).links.map { it.target }
        assertEquals(listOf("Home.md", "Projects/Diagram.excalidraw.md", "Projects/image.png", "Home.md"), projects)

        assertEquals("A/Same.md", (node("A/Other.md") as Page).links.single().target)
        assertEquals("Home.md", (node("Projects/Diagram.excalidraw.md") as Diagram).links.single().target)
    }
}
