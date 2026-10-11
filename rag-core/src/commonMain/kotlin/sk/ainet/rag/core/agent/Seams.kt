package sk.ainet.rag.core.agent

import sk.ainet.rag.core.bundle.EmbeddingInfo

/**
 * The model-backed query capability, behind an interface so retrieval degrades to BM25 when no
 * embedder is configured rather than failing. Implementations arrive as cartridges; the parity
 * check against the bundle manifest decides whether one may serve a given index.
 */
interface Embedder : AutoCloseable {
    val dimensions: Int

    /** Null when the query cannot be embedded, so retrieval falls back rather than failing. */
    fun embed(text: String): FloatArray?
}

/**
 * One installable embedder implementation. The facade matches the bundle manifest's model id
 * against [modelId] and asks the winning provider to build the [Embedder]; the provider runs the
 * parity check and may still return null (space mismatch, missing model files), in which case
 * retrieval degrades to BM25. This is the runtime half of the cartridge story: each embedding
 * cartridge ships one provider, and the host lists the providers it compiled in.
 */
interface EmbedderProvider {
    /** The embedding-space id this provider serves — the manifest selects by equality on it. */
    val modelId: String

    suspend fun create(manifest: EmbeddingInfo, onIncompatible: (String) -> Unit): Embedder?
}

/**
 * The generation seam. One prompt in, the raw model text out; null signals the model could not
 * answer (failure, cancellation, no model present) and the agent loop ends the turn honestly.
 *
 * This is the surface a generator cartridge must fill (llm/v1 task profile): the agent loop
 * above it owns the TOOL:/ANSWER: grammar, the rounds, and the output gate, so a cartridge
 * only brings tokens. No engine type crosses this boundary.
 */
interface Generator : AutoCloseable {
    suspend fun generate(prompt: String): String?
}
