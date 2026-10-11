package sk.ainet.rag.core.bundle

/**
 * The host's declaration of its own embedder. Deliberately typed by the host rather than copied
 * from the bundle: parity checking only means something when the two sides state their
 * configuration independently and a referee compares them.
 */
data class EmbedderDescriptor(
    val modelId: String,
    val dimensions: Int,
    val normalized: Boolean = true,
    val queryPrefix: String,
    val documentPrefix: String,
)

object EmbeddingParity {

    sealed interface Result {
        data object Compatible : Result

        /** The vectors are untrustworthy; retrieval continues on BM25 alone. */
        data class Incompatible(val reasons: List<String>) : Result
    }

    /**
     * Field-by-field, no shortcuts. Each of these mismatches searches the wrong vector space
     * without raising anything, so the comparison is the only guard there is — and the reasons
     * are spelled out because "incompatible" alone sends someone diffing JSON by hand.
     */
    fun check(manifest: EmbeddingInfo, runtime: EmbedderDescriptor): Result {
        val reasons = mutableListOf<String>()
        fun mismatch(field: String, inBundle: Any?, inRuntime: Any?) {
            if (inBundle != inRuntime) reasons += "$field: bundle $inBundle, runtime $inRuntime"
        }
        mismatch("model id", manifest.modelId, runtime.modelId)
        mismatch("dimensions", manifest.dimensions, runtime.dimensions)
        mismatch("normalized", manifest.normalized, runtime.normalized)
        mismatch("query prefix", "\"${manifest.queryPrefix}\"", "\"${runtime.queryPrefix}\"")
        mismatch("document prefix", "\"${manifest.documentPrefix}\"", "\"${runtime.documentPrefix}\"")
        return if (reasons.isEmpty()) Result.Compatible else Result.Incompatible(reasons)
    }
}
