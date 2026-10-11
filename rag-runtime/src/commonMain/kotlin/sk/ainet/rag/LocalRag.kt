package sk.ainet.rag

import sk.ainet.rag.core.agent.AgentConfig
import sk.ainet.rag.core.agent.AgentEvent
import sk.ainet.rag.core.agent.AgentMessage
import sk.ainet.rag.core.agent.AgentOutcome
import sk.ainet.rag.core.agent.AgentRunner
import sk.ainet.rag.core.agent.AgentTimings
import sk.ainet.rag.core.agent.Embedder
import sk.ainet.rag.core.agent.EmbedderProvider
import sk.ainet.rag.core.agent.Generator
import sk.ainet.rag.core.agent.Passage
import sk.ainet.rag.core.agent.LocalRagState
import sk.ainet.rag.core.agent.RagLog
import sk.ainet.rag.core.bundle.Bundle
import sk.ainet.rag.core.bundle.BundleCodec
import sk.ainet.rag.core.index.VectorIndex
import sk.ainet.rag.core.retrieve.Retriever
import sk.ainet.rag.core.retrieve.SingleFlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * The library facade, platform-free: no Context, no engine type, no file API crosses this
 * boundary. The host supplies the bundle as bytes (assets on Android, NSBundle on Apple
 * platforms, classpath or file on the JVM) and, optionally, a [Generator] — which is where a
 * generator cartridge plugs in. The embedder is selected from the bundle manifest: the embedding
 * block's model id picks one of the configured [EmbedderProvider]s (the weight-free SKaiNET
 * hashed-projection embedder by default; embedding cartridges add theirs). No provider for the
 * id, a parity mismatch, or no block at all means BM25-only retrieval — a supported state, not
 * an error.
 */
class LocalRag(private val config: Config) {

    data class Config(
        /** Reads the bundle bytes. Called once on [initialize]. */
        val loadBundle: suspend () -> ByteArray,
        /** The host app version, matched against each chunk's appVersionMin/Max. */
        val appVersion: String = "0",
        val topK: Int = 4,
        /** The generation seam. Null means retrieval-only; the agent answers "no information". */
        val generator: Generator? = null,
        /**
         * The embedders this host compiled in, matched against the manifest's model id. Order
         * matters only when two providers claim the same id (first wins).
         */
        val embedders: List<EmbedderProvider> = listOf(SkaiNetQueryEmbedder.Provider),
    )

    private val stateFlow = MutableStateFlow<LocalRagState>(LocalRagState.Idle)
    val state: StateFlow<LocalRagState> get() = stateFlow

    private var bundle: Bundle? = null
    private var embedder: Embedder? = null
    private var retriever: Retriever? = null
    private val singleFlight = SingleFlight()

    suspend fun initialize() {
        stateFlow.value = LocalRagState.Loading
        try {
            val loaded = withContext(Dispatchers.Default) { BundleCodec.decode(config.loadBundle()) }
            val chosenEmbedder = createEmbedder(loaded)
            val vectorIndex = if (chosenEmbedder != null && loaded.vectors != null) {
                VectorIndex(loaded.vectors!!, loaded.manifest.embedding!!.dimensions)
            } else {
                null
            }
            bundle = loaded
            embedder = chosenEmbedder
            retriever = Retriever(loaded, vectorIndex, config.appVersion)
            stateFlow.value = LocalRagState.Ready(
                chunkCount = loaded.manifest.chunkCount,
                contentVersion = loaded.manifest.contentVersion,
                usingVectors = vectorIndex != null,
                canGenerate = config.generator != null,
            )
        } catch (failure: Exception) {
            RagLog.w(TAG, "initialize failed: ${failure.message}")
            stateFlow.value = LocalRagState.Failed(failure.message ?: "bundle load failed")
        }
    }

    /** Raw retrieval, for search screens and for pre-searching agent documentation. */
    suspend fun retrieveOnly(
        query: String,
        categories: Set<String> = emptySet(),
    ): List<Passage> = withContext(Dispatchers.Default) {
        val retriever = retriever ?: return@withContext emptyList()
        val vector = embedder?.embed(query)
        retriever.retrieve(query, config.topK, vector, categories)
    }

    /**
     * One agent turn. With no [Config.generator] this resolves immediately as Unresolved —
     * the host shows its fixed fallback while retrieval keeps working.
     */
    suspend fun runAgent(
        query: String,
        agentConfig: AgentConfig,
        history: List<AgentMessage> = emptyList(),
        onEvent: (AgentEvent) -> Unit = {},
    ): AgentOutcome = singleFlight.run {
        val generator = config.generator
            ?: return@run AgentOutcome.Unresolved(AgentTimings(0, 0, 0, 0))
        withContext(Dispatchers.Default) {
            AgentRunner().answer(
                generate = { prompt -> generator.generate(prompt) },
                query = query,
                config = agentConfig,
                history = history,
                onEvent = onEvent,
            )
        }
    }

    fun release() {
        embedder?.close()
        embedder = null
        retriever = null
        bundle = null
        config.generator?.close()
        stateFlow.value = LocalRagState.Idle
    }

    private suspend fun createEmbedder(bundle: Bundle): Embedder? {
        val embedding = bundle.manifest.embedding ?: return null
        val provider = config.embedders.firstOrNull { it.modelId == embedding.modelId }
        if (provider == null) {
            RagLog.w(TAG, "no embedder for model id `${embedding.modelId}`, retrieving with BM25 only")
            return null
        }
        return provider.create(embedding) { reason -> RagLog.w(TAG, reason) }
    }

    private companion object {
        const val TAG = "LocalRag"
    }
}
