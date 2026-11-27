package ink.knowledge.enums.vault

enum class FileFormat(
    val suffixes: List<String>,
    val mimeType: String,
) {
    FOLDER(emptyList(), ""),
    MARKDOWN(listOf(".md"), "text/markdown"),
    EXCALIDRAW(listOf(".excalidraw.md"), "text/markdown"),
    CANVAS(listOf(".canvas"), "application/json"),
    BASE(listOf(".base"), "application/yaml"),
    PDF(listOf(".pdf"), "application/pdf"),
    IMAGE_JPG(listOf(".jpg", ".jpeg"), "image/jpeg"),
    IMAGE_PNG(listOf(".png"), "image/png"),
    VIDEO_MP4(listOf(".mp4"), "video/mp4"),
    VIDEO_MOV(listOf(".mov"), "video/quicktime"),
    UNKNOWN(emptyList(), "application/octet-stream"),
    ;

    companion object {
        // Longest first so .excalidraw.md wins over .md
        private val bySuffix =
            entries
                .flatMap { format -> format.suffixes.map { it to format } }
                .sortedByDescending { it.first.length }

        fun of(fileName: String): FileFormat = matchSuffix(fileName)?.second ?: UNKNOWN

        /** File name without its format suffix. */
        fun stripSuffix(fileName: String): String {
            val suffix = matchSuffix(fileName)?.first ?: return fileName.substringBeforeLast('.')
            return fileName.dropLast(suffix.length)
        }

        private fun matchSuffix(fileName: String): Pair<String, FileFormat>? {
            val lower = fileName.lowercase()
            return bySuffix.firstOrNull { lower.endsWith(it.first) }
        }
    }
}
