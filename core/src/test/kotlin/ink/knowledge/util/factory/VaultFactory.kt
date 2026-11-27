package ink.knowledge.util.factory

import ink.knowledge.configuration.properties.VaultProperties
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.getAs
import java.nio.file.Files
import java.nio.file.Path

object VaultFactory {
    /** Shipped `vault` config with the root pointed at [root]. */
    fun properties(root: Path = create()): VaultProperties =
        ApplicationConfig("application.conf").property("vault").getAs<VaultProperties>().copy(root = root.toString())

    /** Writes a small vault covering every node kind and link case. */
    fun create(root: Path = Files.createTempDirectory("vault")): Path {
        write(root, "Home.md", "---\ntitle: x\n# not a title\n---\n# Home Page\n[[Notes.base#Table]] [[Missing]] [[#Local]]\n")
        write(
            root,
            "Projects/Projects.md",
            "[[Home|Start]] ![[Diagram.excalidraw]] ![[image.png|300]]\n```\n[[Ignored]]\n```\n| [[Home\\|Escaped]] |\n",
        )
        write(root, "Projects/Diagram.excalidraw.md", "## Text Elements\n[[Home]]\n")
        write(root, "Projects/image.png", "png")
        write(root, "Notes.base", "views: []\n")
        write(root, "A/Same.md", "a")
        write(root, "A/Other.md", "[[Same]]")
        write(root, "B/Same.md", "b")
        write(root, ".obsidian/app.json", "{}")
        write(root, "notes.txt", "unknown format")
        Files.createDirectories(root.resolve("Empty"))
        return root
    }

    private fun write(
        root: Path,
        path: String,
        text: String,
    ) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, text)
    }
}
