package sk.ainet.rag.core.text

/**
 * Reduces Markdown to the plain text the index and the embedder should see. Display always uses
 * the original source; this is only the indexing view.
 *
 * Handles the syntax technical docs actually contain — headings, emphasis, inline and fenced
 * code, links, images, list markers, blockquotes — and nothing speculative beyond that. Fenced
 * code keeps its contents (identifiers and values inside fences are exactly what users search
 * for); images disappear entirely; links keep their label and lose their target.
 */
object Markdown {

    fun strip(markdown: String): String {
        // Unwrap fences first so their contents flow through the per-line rules like any text.
        val unfenced = CODE_FENCE.replace(markdown) { it.groupValues[1] }
        return unfenced
            .lineSequence()
            .map { line -> LINE_RULES.fold(line) { acc, (regex, out) -> regex.replace(acc, out) }.trim() }
            .filter(String::isNotEmpty)
            .joinToString("\n")
            .trim()
    }

    // `(?s)` inline because RegexOption.DOT_MATCHES_ALL does not exist on Kotlin/Native.
    private val CODE_FENCE = Regex("(?s)```[a-zA-Z0-9]*\\n(.*?)```")

    /** Applied in order per line; the image rule must precede the link rule it would otherwise match. */
    private val LINE_RULES: List<Pair<Regex, String>> = listOf(
        Regex("^#{1,6}\\s+") to "",
        Regex("^>\\s?") to "",
        Regex("^\\s*([-*+]|\\d+\\.)\\s+") to "",
        Regex("!\\[[^\\]]*\\]\\([^)]*\\)") to "",
        Regex("\\[([^\\]]*)\\]\\([^)]*\\)") to "$1",
        Regex("\\*{1,3}([^*]+)\\*{1,3}") to "$1",
        Regex("_{1,3}([^_]+)_{1,3}") to "$1",
        Regex("`([^`]*)`") to "$1",
    )
}
