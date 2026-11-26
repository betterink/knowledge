package ink.knowledge.util

import ink.knowledge.enums.workspace.LinkKind
import ink.knowledge.models.workspace.link.Link
import ink.knowledge.models.workspace.link.ParsedLink

/** `[[wikilink]]` & `![[embeds]]` extraction & helpers */
object LinkUtils {
    private val wikiLink = Regex("""(!?)\[\[([^\[\]\n]+?)]]""")
    private val fence = Regex("""^(```|~~~).*?^\1""", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))

    /** Links with `target` unresolved; `raw` holds the text inside the brackets. */
    fun parse(
        source: String,
        text: String,
    ): List<ParsedLink> {
        val fenced = fence.findAll(text).map { it.range }.toList()
        return wikiLink
            .findAll(text)
            .filter { match -> fenced.none { match.range.first in it } }
            .map { toParsedLink(source, it) }
            .toList()
    }

    private fun toParsedLink(
        source: String,
        match: MatchResult,
    ): ParsedLink {
        val raw = match.groupValues[2]
        val destination = raw.substringBefore('|').removeSuffix("\\")
        val alias = raw.substringAfter('|', missingDelimiterValue = "").ifEmpty { null }
        val link =
            Link(
                source = source,
                target = null,
                raw = raw,
                subpath = destination.substringAfter('#', missingDelimiterValue = "").ifEmpty { null },
                alias = alias,
                kind = if (match.groupValues[1] == "!") LinkKind.EMBED else LinkKind.LINK,
                start = match.range.first,
                end = match.range.last + 1,
            )
        return ParsedLink(linkpath = destination.substringBefore('#').trim(), link = link)
    }
}
