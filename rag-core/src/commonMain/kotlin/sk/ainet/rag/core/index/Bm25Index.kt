package sk.ainet.rag.core.index

import sk.ainet.rag.core.document.Chunk
import sk.ainet.rag.core.text.Tokenizer
import kotlin.math.ln
import kotlinx.serialization.Serializable

/**
 * An in-memory inverted index scored with BM25 (Okapi, probabilistic idf floored at zero).
 *
 * Deliberately hand-rolled instead of bridging to a platform full-text engine: this exact code
 * runs in the bundler and on every target, so the index a CI machine writes and the queries a
 * device asks can never disagree about tokenization or scoring. That property is worth more than
 * any engine feature, and at corpus sizes measured in hundreds of chunks BM25 needs nothing more.
 *
 * Term frequencies are weighted by where the term appears — alias, title/heading, body — because
 * front-matter aliases are how a corpus teaches the index its users' vocabulary.
 */
class Bm25Index(
    private val postings: Map<String, List<Posting>>,
    private val chunkLengths: FloatArray,
    private val averageChunkLength: Float,
    private val k1: Float = DEFAULT_K1,
    private val b: Float = DEFAULT_B,
) {

    @Serializable
    data class Posting(val chunkIndex: Int, val weightedFrequency: Float)

    /** The index reduced to data. Codecs persist this and nothing else. */
    @Serializable
    data class Snapshot(
        val k1: Float,
        val b: Float,
        val averageChunkLength: Float,
        val chunkLengths: List<Float>,
        val postings: Map<String, List<Posting>>,
    )

    val chunkCount: Int get() = chunkLengths.size

    fun search(queryTerms: List<String>, topK: Int): List<ScoredChunk> {
        if (topK <= 0 || chunkCount == 0) return emptyList()
        val scores = FloatArray(chunkCount)
        for (term in queryTerms.distinct()) accumulate(term, scores)
        return rank(scores, topK)
    }

    private fun accumulate(term: String, scores: FloatArray) {
        val hits = postings[term] ?: return
        val idf = idfOf(hits.size)
        for (hit in hits) {
            val tf = hit.weightedFrequency
            val lengthNorm = 1f - b + b * chunkLengths[hit.chunkIndex] / averageChunkLength
            scores[hit.chunkIndex] += idf * (tf * (k1 + 1f)) / (tf + k1 * lengthNorm)
        }
    }

    /** Probabilistic idf with the usual +0.5 smoothing; never negative, so ubiquity cannot punish. */
    private fun idfOf(documentFrequency: Int): Float {
        val ratio = (chunkCount - documentFrequency + 0.5) / (documentFrequency + 0.5)
        return maxOf(ln(1.0 + ratio), 0.0).toFloat()
    }

    private fun rank(scores: FloatArray, topK: Int): List<ScoredChunk> {
        val ranked = ArrayList<ScoredChunk>()
        for (chunkIndex in scores.indices) {
            val score = scores[chunkIndex]
            if (score > 0f) ranked += ScoredChunk(chunkIndex, score)
        }
        ranked.sortWith(compareByDescending<ScoredChunk> { it.score }.thenBy { it.chunkIndex })
        return if (ranked.size <= topK) ranked else ArrayList(ranked.subList(0, topK))
    }

    fun snapshot(): Snapshot = Snapshot(
        k1 = k1,
        b = b,
        averageChunkLength = averageChunkLength,
        chunkLengths = chunkLengths.toList(),
        postings = postings,
    )

    companion object {
        const val DEFAULT_K1 = 1.2f
        const val DEFAULT_B = 0.75f

        const val ALIAS_WEIGHT = 3.0f
        const val TITLE_WEIGHT = 2.0f
        const val BODY_WEIGHT = 1.0f

        fun build(
            chunks: List<Chunk>,
            k1: Float = DEFAULT_K1,
            b: Float = DEFAULT_B,
        ): Bm25Index {
            val postings = mutableMapOf<String, MutableList<Posting>>()
            val lengths = FloatArray(chunks.size)

            for ((chunkIndex, chunk) in chunks.withIndex()) {
                // Insertion-ordered on purpose: the length sum below must be reproducible.
                val frequencies = mutableMapOf<String, Float>()
                fun count(text: String, weight: Float) {
                    for (term in Tokenizer.tokenize(text)) {
                        frequencies[term] = (frequencies[term] ?: 0f) + weight
                    }
                }
                for (alias in chunk.aliases) count(alias, ALIAS_WEIGHT)
                count(chunk.title, TITLE_WEIGHT)
                chunk.heading?.let { count(it, TITLE_WEIGHT) }
                count(chunk.displayText, BODY_WEIGHT)

                lengths[chunkIndex] = frequencies.values.sum()
                for ((term, frequency) in frequencies) {
                    postings.getOrPut(term) { mutableListOf() }.add(Posting(chunkIndex, frequency))
                }
            }

            val mean = if (lengths.isEmpty()) 1f else lengths.average().toFloat()
            return Bm25Index(
                postings = postings.mapValues { it.value.toList() },
                chunkLengths = lengths,
                averageChunkLength = if (mean <= 0f) 1f else mean, // empty corpus: no zero division
                k1 = k1,
                b = b,
            )
        }

        fun from(snapshot: Snapshot): Bm25Index = Bm25Index(
            postings = snapshot.postings,
            chunkLengths = snapshot.chunkLengths.toFloatArray(),
            averageChunkLength = snapshot.averageChunkLength,
            k1 = snapshot.k1,
            b = snapshot.b,
        )
    }
}
