package sk.ainet.rag

import sk.ainet.rag.core.agent.Embedder
import sk.ainet.rag.core.agent.EmbedderProvider
import sk.ainet.rag.core.bundle.EmbedderDescriptor
import sk.ainet.rag.core.bundle.EmbeddingInfo
import sk.ainet.rag.core.bundle.EmbeddingParity
import sk.ainet.rag.embedder.skainet.SkaiNetHashedProjectionEmbedder

/**
 * The SKaiNET-backed query embedder: the same compiled code that embedded the corpus at build
 * time (rag-bundler runs it in-process). No model file, no engine process — creating it needs
 * no path from the host.
 *
 * The descriptor below is intrinsic to the implementation, never read from the bundle: reading
 * the manifest into the runtime descriptor would make the parity check worthless.
 */
class SkaiNetQueryEmbedder private constructor(
    private val delegate: SkaiNetHashedProjectionEmbedder,
) : Embedder {

    override val dimensions: Int get() = delegate.dimensions

    /** Null on any failure so retrieval falls back to BM25, matching the seam's contract. */
    override fun embed(text: String): FloatArray? = try {
        delegate.embed(text)
    } catch (failure: RuntimeException) {
        null
    }

    override fun close() {
        delegate.close()
    }

    /** The default entry in [LocalRag.Config.embedders]: weight-free, so it always opens. */
    object Provider : EmbedderProvider {
        override val modelId: String get() = MODEL_ID

        override suspend fun create(
            manifest: EmbeddingInfo,
            onIncompatible: (String) -> Unit,
        ): Embedder? = createIfCompatible(manifest, onIncompatible)
    }

    companion object {

        const val MODEL_ID: String = SkaiNetHashedProjectionEmbedder.MODEL_ID

        /** What this runtime provably does; compared field by field against the manifest. */
        private val RUNTIME = EmbedderDescriptor(
            modelId = SkaiNetHashedProjectionEmbedder.MODEL_ID,
            dimensions = SkaiNetHashedProjectionEmbedder.DEFAULT_DIMENSIONS,
            normalized = true,
            queryPrefix = "",
            documentPrefix = "",
        )

        fun createIfCompatible(
            manifest: EmbeddingInfo,
            onIncompatible: (String) -> Unit,
        ): SkaiNetQueryEmbedder? {
            val parity = EmbeddingParity.check(manifest, RUNTIME)
            if (parity is EmbeddingParity.Result.Incompatible) {
                onIncompatible(
                    "bundle vectors do not match the SKaiNET embedder, retrieving with BM25 " +
                        "only: " + parity.reasons.joinToString("; "),
                )
                return null
            }
            return SkaiNetQueryEmbedder(SkaiNetHashedProjectionEmbedder(RUNTIME.dimensions))
        }
    }
}
