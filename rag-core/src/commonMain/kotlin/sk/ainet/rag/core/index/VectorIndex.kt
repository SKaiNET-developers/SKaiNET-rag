package sk.ainet.rag.core.index

import kotlin.math.sqrt

/**
 * Exhaustive cosine search over pre-normalized vectors: every score is a single dot product.
 *
 * Exhaustive is a feature at this scale. A few thousand chunks times a few hundred dimensions
 * scans in low single-digit milliseconds on a phone, and an ANN structure would trade that
 * simplicity for recall loss and another artifact to version. The moment a profile disagrees,
 * this class is the seam to replace.
 */
class VectorIndex(private val vectors: FloatArray, val dimensions: Int) {

    val chunkCount: Int = if (dimensions == 0) 0 else vectors.size / dimensions

    init {
        require(dimensions >= 0) { "dimensions must not be negative" }
        require(dimensions == 0 || vectors.size % dimensions == 0) {
            "vector data of ${vectors.size} floats does not divide into $dimensions dimensions"
        }
    }

    /**
     * Scores below [minimumScore] are discarded before ranking. The floor matters to fusion:
     * without it every chunk occupies a vector rank, and rank fusion will happily promote a
     * passage whose only merit is existing.
     */
    fun search(query: FloatArray, topK: Int, minimumScore: Float = DEFAULT_MINIMUM_SCORE): List<ScoredChunk> {
        if (topK <= 0 || chunkCount == 0) return emptyList()
        require(query.size == dimensions) {
            "query has ${query.size} dimensions, index has $dimensions"
        }

        val hits = ArrayList<ScoredChunk>(chunkCount)
        for (chunkIndex in 0 until chunkCount) {
            val base = chunkIndex * dimensions
            var similarity = 0f
            for (d in 0 until dimensions) {
                similarity += vectors[base + d] * query[d]
            }
            if (similarity > minimumScore) hits += ScoredChunk(chunkIndex, similarity)
        }
        hits.sortWith(compareByDescending<ScoredChunk> { it.score }.thenBy { it.chunkIndex })
        return if (hits.size <= topK) hits else ArrayList(hits.subList(0, topK))
    }

    companion object {
        /** Near-orthogonal is noise: below this cosine there is no weak match to salvage. */
        const val DEFAULT_MINIMUM_SCORE = 0.03f

        /** Normalization happens once, at build time; queries then stay a pure dot product. */
        fun normalize(vector: FloatArray): FloatArray {
            var squares = 0.0
            for (value in vector) squares += value.toDouble() * value
            val norm = sqrt(squares).toFloat()
            if (norm == 0f) return vector.copyOf()
            return FloatArray(vector.size) { vector[it] / norm }
        }
    }
}
