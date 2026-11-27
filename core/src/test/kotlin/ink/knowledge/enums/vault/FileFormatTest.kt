package ink.knowledge.enums.vault

import kotlin.test.Test
import kotlin.test.assertEquals

class FileFormatTest {
    @Test
    fun excalidrawMatchesBeforeMarkdown() {
        assertEquals(FileFormat.EXCALIDRAW, FileFormat.of("System Overview.excalidraw.md"))
        assertEquals(FileFormat.MARKDOWN, FileFormat.of("Tablet API.md"))
    }

    @Test
    fun matchesCaseInsensitively() {
        assertEquals(FileFormat.IMAGE_JPG, FileFormat.of("photo.JPEG"))
        assertEquals(FileFormat.BASE, FileFormat.of("Notes.base"))
    }

    @Test
    fun unknownForUnlistedExtensions() {
        assertEquals(FileFormat.UNKNOWN, FileFormat.of("notes.txt"))
        assertEquals(FileFormat.UNKNOWN, FileFormat.of(".DS_Store"))
    }

    @Test
    fun stripSuffixRemovesWholeFormatSuffix() {
        assertEquals("System Overview", FileFormat.stripSuffix("System Overview.excalidraw.md"))
        assertEquals("2.2 Engineering", FileFormat.stripSuffix("2.2 Engineering.md"))
    }
}
