package ink.knowledge.service.vault

import ink.knowledge.exceptions.NotFoundException
import ink.knowledge.util.factory.VaultFactory
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FileServiceTest {
    private val service = FileService(VaultFactory.properties())

    @Test
    fun indexSkipsExcludedUnknownAndSymlinkedFiles() {
        val root = VaultFactory.create()
        val outside = Files.createTempDirectory("outside")
        Files.writeString(outside.resolve("secret.md"), "secret")
        Files.createSymbolicLink(root.resolve("Leak.md"), outside.resolve("secret.md"))
        Files.createSymbolicLink(root.resolve("Linked"), outside)
        val index = FileService(VaultFactory.properties(root)).index()
        assertEquals(listOf("A", "B", "Empty", "Projects"), index.folders)
        assertEquals(
            listOf(
                "A/Other.md",
                "A/Same.md",
                "B/Same.md",
                "Home.md",
                "Notes.base",
                "Projects/Diagram.excalidraw.md",
                "Projects/Projects.md",
                "Projects/image.png",
            ),
            index.files.keys.toList(),
        )
    }

    @Test
    fun indexVersionsMatchVersion() {
        val files = service.index().files
        assertEquals("ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb", files.getValue("A/Same.md").hash)
        files.forEach { (path, version) -> assertEquals(service.version(path), version) }
    }

    @Test
    fun rootMustBeExistingDirectory() {
        val parent = Files.createTempDirectory("parent")
        assertFailsWith<IllegalArgumentException> { FileService(VaultFactory.properties(parent.resolve("missing"))) }
        assertFailsWith<IllegalArgumentException> { FileService(VaultFactory.properties(Files.createFile(parent.resolve("file.md")))) }
    }

    @Test
    fun rootMustBeObsidianVault() {
        val plain = Files.createTempDirectory("plain")
        Files.writeString(plain.resolve("Note.md"), "x")
        assertFailsWith<IllegalArgumentException> { FileService(VaultFactory.properties(plain)) }
    }

    @Test
    fun symlinkedRootIndexesTheVault() {
        val vault = VaultFactory.create()
        val link = Files.createSymbolicLink(Files.createTempDirectory("link").resolve("vault"), vault)
        assertEquals(FileService(VaultFactory.properties(vault)).index(), FileService(VaultFactory.properties(link)).index())
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

    @Test
    fun rejectsSymlinksOutsideVault() {
        val root = VaultFactory.create()
        val outside = Files.createTempDirectory("outside")
        Files.writeString(outside.resolve("secret.md"), "secret")
        Files.createSymbolicLink(root.resolve("Leak.md"), outside.resolve("secret.md"))
        Files.createSymbolicLink(root.resolve("Linked"), outside)
        val service = FileService(VaultFactory.properties(root))
        assertFailsWith<IllegalArgumentException> { service.read("Leak.md") }
        assertFailsWith<IllegalArgumentException> { service.read("Linked/secret.md") }
    }
}
