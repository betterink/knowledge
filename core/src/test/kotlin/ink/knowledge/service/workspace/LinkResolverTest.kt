package ink.knowledge.service.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinkResolverTest {
    private val resolver =
        LinkResolver(
            listOf(
                "Home.md",
                "Notes.base",
                "Projects/Diagram.excalidraw.md",
                "A/Same.md",
                "B/Same.md",
                "Deep/Nested/Same.md",
            ),
        )

    @Test
    fun resolvesByNameWithOrWithoutExtension() {
        assertEquals("Home.md", resolver.resolve("x.md", "Home"))
        assertEquals("Home.md", resolver.resolve("x.md", "home.md"))
        assertEquals("Notes.base", resolver.resolve("x.md", "Notes.base"))
        assertEquals("Projects/Diagram.excalidraw.md", resolver.resolve("x.md", "Diagram.excalidraw"))
    }

    @Test
    fun unresolvedReturnsNull() {
        assertNull(resolver.resolve("x.md", "Missing"))
    }

    @Test
    fun emptyLinkpathIsTheSource() {
        assertEquals("x.md", resolver.resolve("x.md", ""))
    }

    @Test
    fun tiesPreferExactPathThenSourceFolderThenShortest() {
        assertEquals("B/Same.md", resolver.resolve("A/Other.md", "B/Same"))
        assertEquals("A/Same.md", resolver.resolve("A/Other.md", "Same"))
        assertEquals("B/Same.md", resolver.resolve("B/Other.md", "Same"))
        assertEquals("A/Same.md", resolver.resolve("Other.md", "Same"))
    }
}
