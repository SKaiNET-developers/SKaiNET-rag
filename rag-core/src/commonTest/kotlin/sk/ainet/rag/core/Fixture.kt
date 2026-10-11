package sk.ainet.rag.core

import sk.ainet.rag.core.document.Chunk
import sk.ainet.rag.core.document.Chunker
import sk.ainet.rag.core.document.DocumentStatus
import sk.ainet.rag.core.document.FrontMatterParser
import sk.ainet.rag.core.document.ParsedDocument

/**
 * [TestCorpus] parsed and chunked once, shared by every retrieval-shaped test.
 *
 * Loading goes through the real parser and the real chunker on purpose — the fixture should
 * break when those change behavior, because the shipped bundles would break the same way.
 */
object Fixture {

    val documents: List<ParsedDocument> by lazy {
        TestCorpus.files.entries
            .sortedBy { it.key }
            .map { (name, content) ->
                val parsed = FrontMatterParser.parse("corpus/$name", content)
                check(parsed.issues.isEmpty()) { "fixture corpus must parse clean: ${parsed.issues}" }
                requireNotNull(parsed.document)
            }
    }

    val published: List<ParsedDocument> by lazy {
        documents.filter { it.status == DocumentStatus.PUBLISHED }
    }

    val chunks: List<Chunk> by lazy {
        val chunker = Chunker()
        published.flatMap { chunker.chunk(it).chunks }
    }
}
