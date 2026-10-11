package sk.ainet.rag.core.answer

import sk.ainet.rag.core.bundle.Cluster
import sk.ainet.rag.core.document.Chunk
import sk.ainet.rag.core.index.Bm25Index
import sk.ainet.rag.core.text.Tokenizer

/**
 * Serves answers that were written ahead of time, when the query genuinely asks their question.
 *
 * Candidate selection is BM25 over the clusters' question variants — the same scoring the rest of
 * retrieval uses, so precomputed answers work on every tier including a device with no model. The
 * acceptance test is term coverage of the query, not a score cutoff: BM25 magnitudes shift with
 * corpus size, and a fixed score threshold would rot as clusters were added.
 */
class ClusterMatcher(
    private val clusters: List<Cluster>,
    /** How much of the query's vocabulary the matched questions must contain. */
    private val minimumCoverage: Float = 0.6f,
    /** Single-word queries express too little intent to deserve a canned answer. */
    private val minimumQueryTerms: Int = 2,
) {

    private val index: Bm25Index? =
        if (clusters.isEmpty()) null else Bm25Index.build(clusters.map(::questionChunk))

    fun match(query: String): Cluster? {
        val index = this.index ?: return null
        val terms = Tokenizer.tokenize(query)
        if (terms.size < minimumQueryTerms) return null

        val top = index.search(terms, 1).firstOrNull() ?: return null
        val cluster = clusters[top.chunkIndex]
        return if (coverage(terms, cluster) >= minimumCoverage) cluster else null
    }

    private fun coverage(queryTerms: List<String>, cluster: Cluster): Float {
        val vocabulary = cluster.questions.flatMapTo(mutableSetOf()) { Tokenizer.tokenize(it) }
        return queryTerms.count { it in vocabulary }.toFloat() / queryTerms.size
    }

    /**
     * A cluster rides through the normal chunk/index machinery: first question as title, the rest
     * as aliases, all of them as body. The answer text itself is never indexed — only questions
     * should attract queries.
     */
    private fun questionChunk(cluster: Cluster) = Chunk(
        chunkId = cluster.id,
        docId = cluster.id,
        title = cluster.questions.firstOrNull().orEmpty(),
        heading = null,
        category = "",
        aliases = cluster.questions.drop(1),
        screen = null,
        appVersionMin = null,
        appVersionMax = null,
        embeddedText = cluster.questions.joinToString(" "),
        displayText = cluster.questions.joinToString(" "),
        tokenCount = 0,
    )
}
