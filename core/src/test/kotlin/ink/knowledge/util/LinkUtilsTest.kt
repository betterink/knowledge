package ink.knowledge.util

import ink.knowledge.enums.workspace.LinkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinkUtilsTest {
    @Test
    fun parsesSubpathAliasAndKind() {
        val parsed = LinkUtils.parse("a.md", "x ![[Notes.base#Project Notes|Notes]] y").single()
        assertEquals("Notes.base", parsed.linkpath)
        assertEquals("Project Notes", parsed.link.subpath)
        assertEquals("Notes", parsed.link.alias)
        assertEquals(LinkKind.EMBED, parsed.link.kind)
        assertEquals(2, parsed.link.start)
        assertEquals(37, parsed.link.end)
    }

    @Test
    fun plainLinkHasNoSubpathOrAlias() {
        val link = LinkUtils.parse("a.md", "[[Tablet API]]").single()
        assertEquals("Tablet API", link.linkpath)
        assertEquals(LinkKind.LINK, link.link.kind)
        assertNull(link.link.subpath)
        assertNull(link.link.alias)
    }

    @Test
    fun handlesTableEscapedPipe() {
        val link = LinkUtils.parse("a.md", "| [[Home\\|Start]] |").single()
        assertEquals("Home", link.linkpath)
        assertEquals("Start", link.link.alias)
    }

    @Test
    fun skipsFencedCode() {
        val text = "[[A]]\n```\n[[B]]\n```\n~~~\n[[C]]\n~~~\n[[D]]"
        assertEquals(listOf("A", "D"), LinkUtils.parse("a.md", text).map { it.linkpath })
    }
}
