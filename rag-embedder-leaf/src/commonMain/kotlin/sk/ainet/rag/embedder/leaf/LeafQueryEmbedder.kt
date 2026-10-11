package sk.ainet.rag.embedder.leaf

import sk.ainet.cartridge.embedding.leaf.LeafEmbeddingCartridge
import sk.ainet.cartridge.embedding.leaf.LeafEmbeddingEngine
import sk.ainet.rag.core.agent.Embedder
import sk.ainet.rag.core.agent.EmbedderProvider
import sk.ainet.rag.core.bundle.EmbedderDescriptor
import sk.ainet.rag.core.bundle.EmbeddingInfo
import sk.ainet.rag.core.bundle.EmbeddingParity

/**
 * The LEAF query embedder: MongoDB's mdbr-leaf-ir neural encoder, arriving as the
 * `embedding-leaf-skainet` cartridge, behind the skeleton's [Embedder] seam. The first embedder
 * here that is not compiled into this repo — the cartridge comes from the local Maven repository
 * exactly the way the cartridge story says models are distributed.
 *
 * Unlike the weight-free hashed projection, opening this embedder needs the pinned model
 * snapshot on disk (~92 MB, the blueprint's fetch step). [Provider] treats a missing snapshot
 * like any other incompatibility: it reports the reason and retrieval falls back to BM25.
 */
class LeafQueryEmbedder private constructor(
    private val engine: LeafEmbeddingEngine,
) : Embedder {

    override val dimensions: Int get() = engine.dimensions

    /** Null on any failure so retrieval falls back to BM25, matching the seam's contract. */
    override fun embed(text: String): FloatArray? = try {
        engine.embed(text)
    } catch (failure: RuntimeException) {
        null
    }

    override fun close() {
        engine.close()
    }

    /** Pass this in `LocalRag.Config.embedders` to serve bundles embedded with LEAF. */
    object Provider : EmbedderProvider {
        override val modelId: String get() = MODEL_ID

        override suspend fun create(
            manifest: EmbeddingInfo,
            onIncompatible: (String) -> Unit,
        ): Embedder? {
            // Parity first: a space mismatch must be reported as such, not as a load failure,
            // and it must not cost a 92 MB weight load to discover.
            val parity = EmbeddingParity.check(manifest, RUNTIME)
            if (parity is EmbeddingParity.Result.Incompatible) {
                onIncompatible(
                    "bundle vectors do not match the LEAF embedder, retrieving with BM25 " +
                        "only: " + parity.reasons.joinToString("; "),
                )
                return null
            }
            return try {
                LeafQueryEmbedder(LeafEmbeddingCartridge.open())
            } catch (failure: Exception) {
                onIncompatible(
                    "LEAF engine failed to open (model snapshot missing?), retrieving with " +
                        "BM25 only: ${failure.message}",
                )
                null
            }
        }
    }

    companion object {

        /** The manifest model id this embedder serves: the cartridge's space fingerprint. */
        const val MODEL_ID: String = LeafEmbeddingCartridge.SPACE_FINGERPRINT

        /**
         * What this runtime provably does, derived from the cartridge's intrinsic descriptor
         * (pinned to the checkpoint, asserted at open time) — never read from the bundle, which
         * would make the parity check worthless.
         */
        val RUNTIME: EmbedderDescriptor = LeafEmbeddingCartridge.descriptor.let { cartridge ->
            EmbedderDescriptor(
                modelId = cartridge.spaceFingerprint,
                dimensions = cartridge.dimensions,
                normalized = cartridge.normalized,
                queryPrefix = cartridge.queryPrefix,
                documentPrefix = cartridge.documentPrefix,
            )
        }
    }
}
