package sk.ainet.rag.sitesearch.indexer

import sk.ainet.rag.core.document.Chunk
import sk.ainet.rag.core.text.Tokenizer

/**
 * Turns one page's section records into retrieval chunks. The rules differ from rag-core's
 * Markdown [sk.ainet.rag.core.document.Chunker] where an extension's reality differs from a
 * curated corpus:
 *
 * - **Code**: stays in the display text — people search identifiers, and BM25 indexes the
 *   display view — but is dropped from the *embedded* text when it dominates the section
 *   (> half the characters), where it would drown the prose the vector should represent.
 * - **Oversize sections**: a bundler can tell an author to restructure; an extension indexing
 *   someone else's rendered site cannot. Past the token budget it warns and hard-splits at
 *   paragraph boundaries, every part keeping the section's anchor (they all scroll to the same
 *   heading).
 */
class SiteChunker(private val config: SiteIndexConfig) {

    class Result(val chunks: List<Chunk>, val warnings: List<String>)

    fun chunk(page: PageRecord): Result {
        val chunks = mutableListOf<Chunk>()
        val warnings = mutableListOf<String>()
        for (section in page.sections) {
            chunks += chunksOf(page, section, warnings)
        }
        return Result(chunks, warnings)
    }

    private fun chunksOf(
        page: PageRecord,
        section: SectionRecord,
        warnings: MutableList<String>,
    ): List<Chunk> {
        val prose = section.text.trim()
        val code = section.codeText.trim()
        val displayText = listOf(prose, code).filter { it.isNotEmpty() }.joinToString("\n\n")
        if (displayText.isEmpty()) return emptyList()

        // Retrieval scope rides in front, as in the Markdown chunker: "Title — H2 › H3".
        val scope = buildString {
            append(page.title)
            if (section.headingPath.isNotEmpty()) {
                append(" — ")
                append(section.headingPath.joinToString(" › "))
            }
        }
        val codeDominates = code.length > prose.length
        val embeddedBody = if (codeDominates) prose.ifEmpty { code } else displayText
        val embeddedText = scope + "\n\n" + embeddedBody

        val category = when (config.categoryFrom) {
            SiteIndexConfig.CategoryFrom.COMPONENT -> page.component ?: DEFAULT_CATEGORY
            SiteIndexConfig.CategoryFrom.MODULE -> page.module ?: DEFAULT_CATEGORY
        }
        val baseId = page.docId + "#" + (section.anchor ?: "intro")

        fun chunk(id: String, display: String, embedded: String, tokens: Int) = Chunk(
            chunkId = id,
            docId = page.docId,
            title = page.title,
            heading = section.headingPath.lastOrNull(),
            category = category,
            aliases = emptyList(),
            screen = null,
            appVersionMin = null,
            appVersionMax = null,
            embeddedText = embedded,
            displayText = display,
            tokenCount = tokens,
            urlPath = page.urlPath,
            anchor = section.anchor,
            component = page.component,
            componentVersion = page.version,
            headingPath = section.headingPath.ifEmpty { null },
        )

        val tokenCount = Tokenizer.tokenize(embeddedText).size
        if (tokenCount <= config.maxChunkTokens) {
            return listOf(chunk(baseId, displayText, embeddedText, tokenCount))
        }

        val parts = splitAtParagraphs(displayText, scope)
        warnings += "${page.urlPath}#${section.anchor ?: ""}: section has $tokenCount tokens, " +
            "past the ${config.maxChunkTokens}-token budget — split into ${parts.size} chunks " +
            "at paragraph boundaries"
        return parts.mapIndexed { index, part ->
            val partEmbedded = scope + "\n\n" + (if (codeDominates) stripFrom(part, code) else part)
            chunk(
                id = if (index == 0) baseId else "$baseId~${index + 1}",
                display = part,
                embedded = partEmbedded,
                tokens = Tokenizer.tokenize(partEmbedded).size,
            )
        }
    }

    /** Greedy paragraph packing: each part stays within budget unless a single paragraph exceeds it. */
    private fun splitAtParagraphs(text: String, scope: String): List<String> {
        val scopeTokens = Tokenizer.tokenize(scope).size
        val budget = (config.maxChunkTokens - scopeTokens).coerceAtLeast(1)
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var currentTokens = 0
        for (paragraph in text.split("\n\n")) {
            val tokens = Tokenizer.tokenize(paragraph).size
            if (currentTokens > 0 && currentTokens + tokens > budget) {
                parts += current.toString()
                current.setLength(0)
                currentTokens = 0
            }
            if (current.isNotEmpty()) current.append("\n\n")
            current.append(paragraph)
            currentTokens += tokens
        }
        if (current.isNotEmpty()) parts += current.toString()
        return parts
    }

    private fun stripFrom(part: String, code: String): String =
        part.replace(code, "").trim().ifEmpty { part }

    private companion object {
        const val DEFAULT_CATEGORY = "site"
    }
}
