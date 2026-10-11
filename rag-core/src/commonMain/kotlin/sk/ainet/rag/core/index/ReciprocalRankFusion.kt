package sk.ainet.rag.core.index

/**
 * Reciprocal rank fusion: each list contributes `1 / (k + rank)` per chunk, and the sums decide.
 *
 * Fusing on rank rather than score is the whole point — a BM25 score and a cosine similarity
 * share no scale, and any scale-bridging constant would be tuned to one corpus and wrong on the
 * next. The standard k = 60 damps the top ranks enough that one list cannot dictate the result.
 */
object ReciprocalRankFusion {

    const val DEFAULT_K: Int = 60

    fun fuse(vararg rankings: List<ScoredChunk>, k: Int = DEFAULT_K): List<ScoredChunk> =
        fuse(rankings.toList(), k)

    fun fuse(rankings: List<List<ScoredChunk>>, k: Int = DEFAULT_K): List<ScoredChunk> {
        val contributions = mutableMapOf<Int, Float>()
        for (ranking in rankings) {
            for ((position, hit) in ranking.withIndex()) {
                contributions[hit.chunkIndex] =
                    (contributions[hit.chunkIndex] ?: 0f) + 1f / (k + position + 1).toFloat()
            }
        }
        return contributions
            .map { (chunkIndex, score) -> ScoredChunk(chunkIndex, score) }
            .sortedWith(compareByDescending<ScoredChunk> { it.score }.thenBy { it.chunkIndex })
    }
}
