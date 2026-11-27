package ink.knowledge.service.vault

import ink.knowledge.configuration.properties.VaultProperties
import ink.knowledge.enums.vault.FileFormat
import ink.knowledge.exceptions.NotFoundException
import ink.knowledge.models.vault.VaultIndex
import ink.knowledge.models.vault.Version
import java.io.OutputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.security.DigestInputStream
import java.security.MessageDigest

/**
 * File store parsing service
 * Will walk the root vault to consume & build the structure of the entire workspace
 */
class FileService(
    private val properties: VaultProperties,
) {
    private val root: Path = validateVaultRoot(Path.of(properties.root).toAbsolutePath().normalize())

    // ------ Public read operations ------

    /** Every allowed folder and file version, vault-relative, sorted. */
    fun index(): VaultIndex {
        val folders = mutableListOf<String>()
        val files = mutableMapOf<String, Version>()
        Files.walkFileTree(
            root,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(
                    dir: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    if (dir == root) return FileVisitResult.CONTINUE
                    if (dir.fileName.toString() in properties.excluded) return FileVisitResult.SKIP_SUBTREE
                    folders += relativize(dir)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    if (attrs.isRegularFile && isAllowed(file.fileName.toString())) {
                        val path = relativize(file)
                        files[path] = version(path)
                    }
                    return FileVisitResult.CONTINUE
                }
            },
        )
        return VaultIndex(folders.sorted(), files.toSortedMap())
    }

    fun read(path: String): ByteArray = Files.readAllBytes(existingFile(path))

    fun version(path: String): Version {
        val file = existingFile(path)
        val digest = MessageDigest.getInstance("SHA-256")
        DigestInputStream(Files.newInputStream(file), digest).use { it.transferTo(OutputStream.nullOutputStream()) }
        return Version(
            hash = digest.digest().toHexString(),
            size = Files.size(file),
            modified = Files.getLastModifiedTime(file).toMillis(),
        )
    }

    fun rootName(): String = root.fileName?.toString().orEmpty()

    // Validates a vault based on expected characteristics (ie. .obsidian file)
    private fun validateVaultRoot(path: Path): Path {
        require(Files.isDirectory(path)) { "vault.root (VAULT_ROOT) is not an existing directory: $path" }
        require(
            Files.isDirectory(path.resolve(".obsidian")),
        ) { "vault.root (VAULT_ROOT) is not an Obsidian vault, no .obsidian folder: $path" }
        return path.toRealPath()
    }

    private fun existingFile(path: String): Path {
        val file = resolve(path)
        if (!Files.isRegularFile(file) || !isAllowed(file.fileName.toString())) {
            throw NotFoundException("File not found: $path")
        }
        val realRoot = root.toRealPath()
        val real = file.toRealPath()
        require(real.startsWith(realRoot)) { "Path escapes the vault: $path" }
        require(realRoot.relativize(real).none { it.toString() in properties.excluded }) { "Path is excluded: $path" }
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
}
