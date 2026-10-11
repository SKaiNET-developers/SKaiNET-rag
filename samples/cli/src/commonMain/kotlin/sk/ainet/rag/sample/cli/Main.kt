package sk.ainet.rag.sample.cli

import sk.ainet.rag.LocalRag
import sk.ainet.rag.SkaiNetQueryEmbedder
import sk.ainet.rag.core.agent.LocalRagState
import sk.ainet.rag.core.agent.RagLog
import sk.ainet.rag.embedder.leaf.LeafQueryEmbedder

/** Reads a whole file. Expect/actual because no file API exists in common Kotlin. */
expect fun readFileBytes(path: String): ByteArray

/**
 * The smallest possible consumer: load a bundle, answer queries with hybrid retrieval. The same
 * source runs as a JVM application and as a native macosArm64 binary — which is the point: one
 * retrieval stack, every target, no interpreter anywhere.
 *
 * Usage: cli <bundle-file> [query...]  — with no query args it reads queries line by line.
 */
suspend fun runCli(args: Array<String>) {
    if (args.isEmpty()) {
        println("usage: cli <bundle-file> [query...]")
        return
    }
    RagLog.sink = { level, tag, message -> println("[$level] $tag: $message") }

    val rag = LocalRag(
        LocalRag.Config(
            loadBundle = { readFileBytes(args[0]) },
            appVersion = "1.0",
            topK = 4,
            // Both compiled-in embedders; the bundle manifest's model id picks one. The LEAF
            // cartridge needs its model snapshot on disk — when it is missing, that bundle
            // degrades to BM25 with a logged reason, like any other parity failure.
            embedders = listOf(SkaiNetQueryEmbedder.Provider, LeafQueryEmbedder.Provider),
        ),
    )
    rag.initialize()
    when (val state = rag.state.value) {
        is LocalRagState.Ready -> println(
            "ready: ${state.chunkCount} chunks, vectors=${state.usingVectors}, " +
                "generator=${state.canGenerate}",
        )
        is LocalRagState.Failed -> {
            println("failed: ${state.reason}")
            return
        }
        else -> Unit
    }

    suspend fun answer(query: String) {
        val passages = rag.retrieveOnly(query)
        if (passages.isEmpty()) {
            println("  no passages")
            return
        }
        for (passage in passages) {
            println("  [${passage.source}] ${passage.title} — ${passage.heading ?: "overview"} (${passage.score})")
        }
    }

    val queries = args.drop(1)
    if (queries.isNotEmpty()) {
        for (query in queries) {
            println("query: $query")
            answer(query)
        }
    } else {
        while (true) {
            print("> ")
            val query = readlnOrNull()?.trim().takeUnless { it.isNullOrEmpty() } ?: break
            answer(query)
        }
    }
    rag.release()
}
