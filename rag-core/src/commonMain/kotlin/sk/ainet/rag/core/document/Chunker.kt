package sk.ainet.rag.core.document

import sk.ainet.rag.core.text.Markdown
import sk.ainet.rag.core.text.Tokenizer

data class ChunkerConfig(
    val maxChunkTokens: Int = 400,
)

/**
 * Produces one [Chunk] per level-two heading.
 *
 * The `##` boundaries are authored, and an author breaking a document where the topic changes
 * beats any token-window heuristic. `###` and deeper stay inside their section; text above the
 * first `##` becomes a leading chunk with no heading.
 *
 * A section past the token budget raises an issue instead of being auto-split: the split point
 * belongs to the author, and a silent one would drift out from under them on the next edit.
 */
class Chunker(private val config: ChunkerConfig = ChunkerConfig()) {

    data class ChunkResult(val chunks: List<Chunk>, val issues: List<DocumentIssue>)

    fun chunk(document: ParsedDocument): ChunkResult {
        val issues = mutableListOf<DocumentIssue>()
        val chunks = mutableListOf<Chunk>()
        for (section in sectionsOf(document.body)) {
            val displayText = section.body.trim()
            if (displayText.isEmpty()) continue

            // Retrieval context rides in front of the stripped body: "Title — Heading".
            val scope = listOfNotNull(document.title, section.heading).joinToString(" — ")
            val embeddedText = scope + "\n\n" + Markdown.strip(displayText)
            val tokenCount = Tokenizer.tokenize(embeddedText).size
            checkBudget(document, section, tokenCount, issues)

            chunks += Chunk(
                chunkId = document.id + "#" + slugOf(section.heading),
                docId = document.id,
                title = document.title,
                heading = section.heading,
                category = document.category,
                aliases = document.aliases,
                screen = document.screen,
                appVersionMin = document.appVersionMin,
                appVersionMax = document.appVersionMax,
                embeddedText = embeddedText,
                displayText = displayText,
                tokenCount = tokenCount,
            )
        }
        return ChunkResult(chunks, issues)
    }

    private fun checkBudget(
        document: ParsedDocument,
        section: Section,
        tokenCount: Int,
        issues: MutableList<DocumentIssue>,
    ) {
        val budget = config.maxChunkTokens
        val place = section.heading?.let { "\"$it\"" } ?: "before the first heading"
        when {
            tokenCount > budget * 2 -> issues += DocumentIssue(
                document.sourcePath, section.line,
                "section $place has $tokenCount tokens — more than double the $budget-token " +
                    "budget. Break it up with its own `##` headings.",
                DocumentIssue.Severity.ERROR,
            )

            tokenCount > budget -> issues += DocumentIssue(
                document.sourcePath, section.line,
                "section $place has $tokenCount tokens, past the $budget-token budget; " +
                    "a split would help retrieval.",
                DocumentIssue.Severity.WARNING,
            )
        }
    }

    private fun sectionsOf(body: String): List<Section> {
        val sections = mutableListOf<Section>()
        var heading: String? = null
        var headingLine = 1
        val buffer = StringBuilder()

        for ((index, line) in body.lines().withIndex()) {
            val found = HEADING_TWO.matchEntire(line)
            if (found == null) {
                buffer.appendLine(line)
                continue
            }
            sections += Section(heading, buffer.toString(), headingLine)
            buffer.setLength(0)
            heading = found.groupValues[1].trim()
            headingLine = index + 1
        }
        sections += Section(heading, buffer.toString(), headingLine)
        return sections
    }

    /** Deterministic: rebuilding an unchanged document reproduces every chunk id. */
    private fun slugOf(heading: String?): String {
        if (heading == null) return "intro"
        val slug = buildString {
            for (c in heading.lowercase()) append(if (c.isLetterOrDigit()) c else '-')
        }.trim('-').replace(SQUEEZED_DASHES, "-")
        return slug.ifEmpty { "section" }
    }

    private data class Section(val heading: String?, val body: String, val line: Int)

    companion object {
        /** Recorded in the bundle manifest; a runtime can tell an index cut by older rules. */
        const val VERSION: Int = 3

        private val HEADING_TWO = Regex("^##\\s+(.+?)\\s*$")
        private val SQUEEZED_DASHES = Regex("-+")
    }
}
