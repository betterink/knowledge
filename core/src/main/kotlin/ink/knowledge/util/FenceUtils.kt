package ink.knowledge.util

/** Fenced code block detection per CommonMark fence matching. */
object FenceUtils {
    private val fence = Regex("""^(`{3,}|~{3,})""")

    /** Character ranges of fenced blocks; an unclosed block runs to the end. */
    fun ranges(text: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var open: String? = null
        var start = 0
        var offset = 0
        for (line in text.split('\n')) {
            val marker = fence.find(line)?.value
            if (open == null && marker != null) {
                open = marker
                start = offset
            } else if (open != null && marker != null && marker[0] == open[0] && marker.length >= open.length && line.trimEnd() == marker) {
                ranges += start until offset + line.length
                open = null
            }
            offset += line.length + 1
        }
        if (open != null) ranges += start until text.length
        return ranges
    }
}
