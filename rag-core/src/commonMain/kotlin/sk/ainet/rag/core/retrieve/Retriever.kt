package sk.ainet.rag.core.retrieve

import sk.ainet.rag.core.agent.MatchSource
import sk.ainet.rag.core.agent.Passage
import sk.ainet.rag.core.bundle.Bundle
import sk.ainet.rag.core.index.ReciprocalRankFusion
import sk.ainet.rag.core.index.ScoredChunk
import sk.ainet.rag.core.index.VectorIndex
import sk.ainet.rag.core.index.VersionFilter
import sk.ainet.rag.core.text.Tokenizer

/**
 * The hybrid retrieval pipeline: BM25 and (when a vector index and a query vector exist) cosine
 * search, fused by rank, then filtered, then cut to size.
 *
 * Filters come last on purpose. Version and category constraints applied before fusion would
 * leave ghost ranks — a discarded chunk still holding a position in one input list — and the
 * fused order below it would be wrong.
 */
class Retriever(
    private val bundle: Bundle,
    private val vectorIndex: VectorIndex?,
    private val appVersion: String,
) {

    fun retrieve(
        query: String,
        topK: Int,
        queryVector: FloatArray?,
        categories: Set<String>,
    ): List<Passage> {
        val terms = Tokenizer.tokenize(query)
        if (terms.isEmpty()) return emptyList()

        // Fetch beyond topK: post-fusion filtering will eat into whatever is retrieved.
        val fetch = topK * CANDIDATE_MULTIPLIER
        val lexical = bundle.bm25.search(terms, fetch)
        val semantic = when {
            vectorIndex != null && queryVector != null -> vectorIndex.search(queryVector, fetch)
            else -> emptyList()
        }
        val fused = if (semantic.isEmpty()) lexical else ReciprocalRankFusion.fuse(lexical, semantic)

        val fromLexical = lexical.mapTo(mutableSetOf()) { it.chunkIndex }
        val fromSemantic = semantic.mapTo(mutableSetOf()) { it.chunkIndex }
        return fused.asSequence()
            .filter { admitted(it, categories) }
            .take(topK)
            .map { toPassage(it, fromLexical, fromSemantic) }
            .toList()
    }

    private fun admitted(hit: ScoredChunk, categories: Set<String>): Boolean {
        val chunk = bundle.chunks[hit.chunkIndex]
        if (categories.isNotEmpty() && chunk.category !in categories) return false
        return VersionFilter.matches(appVersion, chunk.appVersionMin, chunk.appVersionMax)
    }

    private fun toPassage(hit: ScoredChunk, fromLexical: Set<Int>, fromSemantic: Set<Int>): Passage {
        val chunk = bundle.chunks[hit.chunkIndex]
        val source = when {
            hit.chunkIndex in fromLexical && hit.chunkIndex in fromSemantic -> MatchSource.HYBRID
            hit.chunkIndex in fromSemantic -> MatchSource.VECTOR
            else -> MatchSource.BM25
        }
        return Passage(
            chunkId = chunk.chunkId,
            docId = chunk.docId,
            title = chunk.title,
            heading = chunk.heading,
            text = chunk.text,
            screenLink = chunk.screen,
            score = hit.score,
            source = source,
        )
    }

    private companion object {
        const val CANDIDATE_MULTIPLIER = 5
    }
}
