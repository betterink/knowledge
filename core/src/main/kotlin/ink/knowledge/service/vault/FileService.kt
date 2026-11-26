package ink.knowledge.service.vault

import ink.knowledge.configuration.properties.VaultProperties
import ink.knowledge.enums.vault.FileFormat
import ink.knowledge.exceptions.NotFoundException
import ink.knowledge.models.vault.VaultEntry
import ink.knowledge.models.vault.Version
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest

/**
 * File store parsing service
 * Will walk the root vault to consume & build the structure of the entire workspace
 */
class FileService(
    private val properties: VaultProperties,
) {
    private val root: Path = Path.of(properties.root).toAbsolutePath().normalize()

    // ------ Public read operations ------

    /** Every allowed folder and file, as vault-relative paths, sorted. */
    fun walk(): List<VaultEntry> {
        val entries = mutableListOf<VaultEntry>()
        Files.walkFileTree(
            root,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(
                    dir: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    if (dir == root) return FileVisitResult.CONTINUE
                    if (dir.fileName.toString() in properties.excluded) return FileVisitResult.SKIP_SUBTREE
                    entries += VaultEntry(relativize(dir), directory = true)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    if (attrs.isRegularFile && isAllowed(file.fileName.toString())) {
                        entries += VaultEntry(relativize(file), directory = false)
                    }
                    return FileVisitResult.CONTINUE
                }
            },
        )
        return entries.sortedBy { it.path }
    }

    fun read(path: String): ByteArray = Files.readAllBytes(existingFile(path))

    fun version(path: String): Version {
        val file = existingFile(path)
        val bytes = Files.readAllBytes(file)
        return Version(
            hash = sha256(bytes),
            size = bytes.size.toLong(),
            modified = Files.getLastModifiedTime(file).toMillis(),
        )
    }

    fun rootName(): String = root.fileName?.toString().orEmpty()

    // ------ Private helpers ------

    private fun existingFile(path: String): Path {
        val file = resolve(path)
        if (!Files.isRegularFile(file) || !isAllowed(file.fileName.toString())) {
            throw NotFoundException("File not found: $path")
        }
        return file
    }

    /** Applies the path policy and returns the absolute path. */
    private fun resolve(path: String): Path {
        val segments = path.split('/')
        require(!path.startsWith('/') && !Path.of(path).isAbsolute) { "Absolute paths are not allowed: $path" }
        require(".." !in segments) { "Parent segments are not allowed: $path" }
        require(segments.none { it in properties.excluded }) { "Path is excluded: $path" }
        val resolved = root.resolve(path).normalize()
        require(resolved.startsWith(root)) { "Path escapes the vault: $path" }
        return resolved
    }

    private fun isAllowed(fileName: String): Boolean = fileName !in properties.excluded && FileFormat.of(fileName) != FileFormat.UNKNOWN

    private fun relativize(path: Path): String = root.relativize(path).joinToString("/")

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHexString()
}
