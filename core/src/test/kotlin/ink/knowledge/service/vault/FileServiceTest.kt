package ink.knowledge.service.vault

import ink.knowledge.exceptions.NotFoundException
import ink.knowledge.util.factory.VaultFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FileServiceTest {
    private val service = FileService(VaultFactory.properties())

    @Test
    fun walkSkipsExcludedAndUnknownFiles() {
        val paths = service.walk().map { it.path }
        assertTrue(paths.none { it.startsWith(".obsidian") })
        assertTrue("notes.txt" !in paths)
        assertTrue("Empty" in paths)
        assertTrue("Projects/Diagram.excalidraw.md" in paths)
    }

    @Test
    fun rejectsPathsOutsidePolicy() {
        assertFailsWith<IllegalArgumentException> { service.read("../etc/passwd") }
        assertFailsWith<IllegalArgumentException> { service.read("A/../../x.md") }
        assertFailsWith<IllegalArgumentException> { service.read("/etc/passwd") }
        assertFailsWith<IllegalArgumentException> { service.read(".obsidian/app.json") }
    }

    @Test
    fun missingOrUnknownFilesAreNotFound() {
        assertFailsWith<NotFoundException> { service.read("Nope.md") }
        assertFailsWith<NotFoundException> { service.read("notes.txt") }
        assertFailsWith<NotFoundException> { service.read("Projects") }
    }

    @Test
    fun versionIsSha256OfContent() {
        val version = service.version("A/Same.md")
        assertEquals("ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb", version.hash)
        assertEquals(1, version.size)
    }
}
