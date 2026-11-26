package ink.knowledge.service.workspace

/**
 * Resolves a linkpath to a vault path.
 * Ties: exact path, then the source's folder, then the shortest path.
 */
class LinkResolverService(
    paths: Collection<String>,
) {
    private val candidates = paths.map { it to keys(it) }

    fun resolve(
        source: String,
        linkpath: String,
    ): String? {
        if (linkpath.isEmpty()) return source
        val wanted = linkpath.lowercase()
        val matches = candidates.filter { (_, keys) -> keys.any { it == wanted || it.endsWith("/$wanted") } }
        if (matches.size <= 1) return matches.singleOrNull()?.first
        matches.firstOrNull { (_, keys) -> wanted in keys }?.let { return it.first }
        val sourceFolder = source.substringBeforeLast('/', missingDelimiterValue = "")
        matches.firstOrNull { (path, _) -> path.substringBeforeLast('/', "") == sourceFolder }?.let { return it.first }
        return matches.minBy { it.first.length }.first
    }

    private fun keys(path: String): Set<String> {
        val lower = path.lowercase()
        return setOf(lower, lower.removeSuffix(".md"))
    }
}
