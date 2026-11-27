package ink.knowledge.service.vault

import ink.knowledge.enums.vault.FileFormat
import ink.knowledge.exceptions.NotFoundException
import ink.knowledge.util.factory.VaultFactory
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

/** Every request path up to [DEPTH] segments over [TOKENS] obeys the vault policy. */
class FileServicePathPolicyTest {
    private val parent = Files.createTempDirectory("policy")
    private val root = VaultFactory.create(Files.createDirectories(parent.resolve("vault")))
    private val properties = VaultFactory.properties(root)
    private val service = FileService(properties)

    init {
        val outside = Files.createDirectories(parent.resolve("outside"))
        Files.writeString(outside.resolve("secret.md"), "secret")
        Files.createDirectories(root.resolve(".claude"))
        Files.writeString(root.resolve(".claude/CLAUDE.md"), "excluded")
        Files.createSymbolicLink(root.resolve("Escape.md"), outside.resolve("secret.md"))
        Files.createSymbolicLink(root.resolve("LinkDir"), outside)
        Files.createSymbolicLink(root.resolve("Hidden"), root.resolve(".claude"))
        Files.createSymbolicLink(root.resolve("Up"), parent)
    }

    @Test
    fun servesOnlyAllowedVaultFiles() {
        val allowed = allowedFiles()
        val violations = mutableListOf<String>()
        for (path in paths()) {
            val bytes =
                try {
                    service.read(path)
                } catch (_: NotFoundException) {
                    continue
                } catch (_: IllegalArgumentException) {
                    continue
                }
            val served = root.resolve(path).normalize()
            if (allowed.none { Files.isSameFile(it, served) }) violations += "$path -> ${served.toRealPath()} (${bytes.size} bytes)"
        }
        assertTrue(violations.isEmpty(), "Served outside policy:\n" + violations.take(20).joinToString("\n"))
    }

    /** Regular non-symlink files under the vault, outside excluded folders, in a known format. */
    private fun allowedFiles(): List<Path> =
        Files.walk(root).use { stream ->
            stream
                .filter { Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS) }
                .filter { file -> root.relativize(file).none { it.toString() in properties.excluded } }
                .filter { FileFormat.of(it.fileName.toString()) != FileFormat.UNKNOWN }
                .toList()
        }

    private fun paths(): Sequence<String> =
        (1..DEPTH)
            .asSequence()
            .flatMap { depth ->
                (1..depth).fold(sequenceOf(emptyList<String>())) { acc, _ -> acc.flatMap { prefix -> TOKENS.map { prefix + it } } }
            }.map { it.joinToString("/") }

    private companion object {
        const val DEPTH = 4
        val TOKENS =
            listOf(
                "",
                ".",
                "..",
                "A",
                "Same.md",
                ".claude",
                ".CLAUDE",
                "CLAUDE.md",
                "Escape.md",
                "LinkDir",
                "secret.md",
                "Hidden",
                "Up",
                "vault",
            )
    }
}
