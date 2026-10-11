package sk.ainet.rag.core.bundle

import sk.ainet.rag.core.document.Chunk
import sk.ainet.rag.core.index.Bm25Index
import kotlinx.serialization.Serializable

/**
 * The embedding half of the manifest: a record of how the vectors in this bundle were produced.
 *
 * Every field is load-bearing for parity. A device whose embedder disagrees on any of them is
 * searching a different vector space, and that failure mode produces no exception — only quietly
 * wrong rankings. The runtime therefore compares all of it against its own [EmbedderDescriptor]
 * before trusting `vectors.bin`.
 */
@Serializable
data class EmbeddingInfo(
    val modelId: String,
    val dimensions: Int,
    val normalized: Boolean,
    val queryPrefix: String,
    val documentPrefix: String,
)

@Serializable
data class Bm25Params(val k1: Float, val b: Float, val avgDocLength: Float)

@Serializable
data class BundleManifest(
    val bundleFormatVersion: Int = CURRENT_FORMAT_VERSION,
    val contentVersion: Int,
    val builtAt: String,
    val chunkCount: Int,
    val chunkerVersion: Int,
    val bm25: Bm25Params,
    val embedding: EmbeddingInfo? = null,
) {
    companion object {
        /** 2 = the LRB2 section container with the binary BM25 index; 1 was the upstream ZIP. */
        const val CURRENT_FORMAT_VERSION: Int = 2
    }
}

/**
 * The persisted shape of a chunk: what retrieval needs at runtime, nothing the build can
 * recompute. In particular [text] is the display text — the embedded view already did its job
 * producing the index and the vectors.
 */
@Serializable
data class StoredChunk(
    val chunkId: String,
    val docId: String,
    val title: String,
    val heading: String? = null,
    val category: String,
    val aliases: List<String> = emptyList(),
    val screen: String? = null,
    val appVersionMin: String? = null,
    val appVersionMax: String? = null,
    val text: String,
) {
    companion object {
        fun from(chunk: Chunk) = StoredChunk(
            chunkId = chunk.chunkId,
            docId = chunk.docId,
            title = chunk.title,
            heading = chunk.heading,
            category = chunk.category,
            aliases = chunk.aliases,
            screen = chunk.screen,
            appVersionMin = chunk.appVersionMin,
            appVersionMax = chunk.appVersionMax,
            text = chunk.displayText,
        )
    }
}

/** A precomputed answer to known phrasings of a question. BM25-matched, so no model is required. */
@Serializable
data class Cluster(
    val id: String,
    val questions: List<String>,
    val answer: String,
    val chunkIds: List<String>,
)

/** An opened bundle. A build that produced no embeddings yields a null [vectors] — valid output. */
class Bundle(
    val manifest: BundleManifest,
    val chunks: List<StoredChunk>,
    val bm25: Bm25Index,
    val vectors: FloatArray?,
    val clusters: List<Cluster>,
)

object BundleEntries {
    const val MANIFEST = "manifest.json"
    const val CHUNKS = "chunks.json"
    const val BM25 = "bm25.bin"
    const val VECTORS = "vectors.bin"
    const val CLUSTERS = "clusters.json"
}
