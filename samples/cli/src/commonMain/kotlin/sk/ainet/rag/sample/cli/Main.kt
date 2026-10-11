package sk.ainet.rag.sample.cli

import sk.ainet.rag.LocalRag
import sk.ainet.rag.SkaiNetQueryEmbedder
import sk.ainet.rag.core.agent.AgentConfig
import sk.ainet.rag.core.agent.AgentOutcome
import sk.ainet.rag.core.agent.LocalRagState
import sk.ainet.rag.core.agent.RagLog
import sk.ainet.rag.core.agent.ToolDefinition
import sk.ainet.rag.core.agent.ToolResult
import sk.ainet.rag.embedder.leaf.LeafQueryEmbedder
import sk.ainet.rag.generator.smollm2.SmolLm2Generator

/** Reads a whole file. Expect/actual because no file API exists in common Kotlin. */
expect fun readFileBytes(path: String): ByteArray

/**
 * The smallest possible consumer: load a bundle, answer queries with hybrid retrieval — and,
 * when the SmolLM2 snapshot is on disk, run the full agent loop on top: grammar, tools, output
 * gate, all on this machine. The same source runs as a JVM application and as a native
 * macosArm64 binary — which is the point: one retrieval stack, every target, no interpreter
 * anywhere.
 *
 * Usage: cli <bundle-file> [query...]  — with no query args it reads queries line by line.
 */
suspend fun runCli(args: Array<String>) {
    if (args.isEmpty()) {
        println("usage: cli <bundle-file> [query...]")
        return
    }
    RagLog.sink = { level, tag, message -> println("[$level] $tag: $message") }

    // Optional by design: a missing snapshot logs its reason and the CLI stays a search tool,
    // the same degradation ladder the library promises (generation → vectors → BM25).
    val generator = SmolLm2Generator.open { reason -> println("[W] cli: $reason") }

    val rag = LocalRag(
        LocalRag.Config(
            loadBundle = { readFileBytes(args[0]) },
            appVersion = "1.0",
            topK = 4,
            generator = generator,
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
        if (generator == null) return

        val outcome = rag.runAgent(query, agentConfig(rag, documentation = passages))
        when (outcome) {
            is AgentOutcome.Final -> {
                println("  answer: ${outcome.text}")
                if (outcome.sources.isNotEmpty()) {
                    println("  sources: ${outcome.sources.joinToString()}")
                }
                println(
                    "  (${outcome.timings.rounds} rounds, ${outcome.timings.generateMs} ms " +
                        "generating, ${outcome.timings.totalMs} ms total)",
                )
            }
            // The designed fallback: never improvise, quote the best retrieved passage.
            is AgentOutcome.Unresolved -> {
                val best = passages.first()
                println("  no generated answer; best passage:")
                println("  ${best.title} — ${best.heading ?: "overview"}")
                println("  ${best.text.lineSequence().firstOrNull().orEmpty()}")
            }
            is AgentOutcome.Refused -> println("  the documentation describes features, not advice.")
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

/**
 * Every word the model reads is host copy (the loop owns only the grammar), so the CLI states
 * its domain plainly and teaches the two directives by example. Retrieval for the question has
 * already run — it rides along as [AgentConfig.documentation] — and the one tool re-searches
 * with different words when that evidence misses.
 */
private fun agentConfig(rag: LocalRag, documentation: List<sk.ainet.rag.core.agent.Passage>) =
    AgentConfig(
        // A 135M model follows patterns, not instructions: the examples below do the real work,
        // ending exactly where the model must continue — after "Question: …" comes "ANSWER:".
        systemPrompt = """
            You answer questions about Java releases using only the documentation shown to you.
            Every reply is one line starting with ANSWER: followed by one or two sentences
            copied from the documentation. To look up documentation first, reply instead with
            one line: TOOL: search|query=<keywords>
            Copy names, versions and numbers exactly as the documentation writes them. If the
            documentation does not contain the answer, reply: ANSWER: The documentation does
            not cover that.

            Question: which release made the HTTP Client API standard?
            ANSWER: The HTTP Client API became standard in Java 11, specified by JEP 321.

            Question: when were sealed classes finalized?
            ANSWER: Sealed Classes were finalized in Java 17, specified by JEP 409.
        """.trimIndent(),
        tools = listOf(
            ToolDefinition(
                name = "search",
                description = "search the documentation",
                argSpec = "query=keywords to look up",
                execute = { toolArgs ->
                    val query = toolArgs["query"]
                    if (query.isNullOrBlank()) {
                        null
                    } else {
                        // Safe re-entry: retrieveOnly takes no turn lock, only runAgent does.
                        val found = rag.retrieveOnly(query)
                        ToolResult(
                            tool = "search",
                            text = found.joinToString("\n---\n") {
                                "${it.title} — ${it.heading ?: "overview"}\n${it.text}"
                            }.ifEmpty { "nothing found" },
                            sourceTitles = found.map { it.title },
                        )
                    }
                },
            ),
        ),
        documentation = documentation,
    )
