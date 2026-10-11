package sk.ainet.rag.generator.smollm2

import sk.ainet.cartridge.generator.smollm2.SmolLm2GeneratorCartridge
import sk.ainet.cartridge.profiles.llm.LlmEngine
import sk.ainet.cartridge.profiles.llm.LlmEvent
import sk.ainet.cartridge.profiles.llm.LlmMessage
import sk.ainet.cartridge.profiles.llm.LlmRole
import sk.ainet.cartridge.profiles.llm.LlmSessionConfig
import sk.ainet.rag.core.agent.Generator
import sk.ainet.rag.core.agent.RagLog

/**
 * The SmolLM2 generator binding: the `generator-smollm2-skainet` cartridge (llm/v1) behind
 * rag-core's [Generator] seam — the first generation cartridge to fill it, completing the loop
 * the architecture promised: retrieval, agent grammar and output gate all running on-device.
 *
 * Two deliberate choices live here, not in the cartridge and not in the loop:
 *
 * - **Greedy decoding.** `temperature = 0` selects argmax sampling, so a frozen bundle plus a
 *   frozen prompt replays to the identical answer — the whitepaper's deterministic-replay
 *   property, bought at the only layer that owns sampling parameters.
 * - **The transcript rides as the SYSTEM message.** [AgentRunner] hands one complete transcript
 *   (grammar rules, observations, question); rendering it as a user turn would let the cartridge
 *   prepend the checkpoint's default "You are SmolLM" system prompt, which competes with the
 *   agent grammar for a 135M model's limited obedience.
 */
class SmolLm2Generator private constructor(
    private val engine: LlmEngine,
    private val maxOutputTokens: Int,
) : Generator {

    /** One session per call — the engine's KV cache is per-turn state, never shared. */
    override suspend fun generate(prompt: String): String? {
        val session = engine.session(
            LlmSessionConfig(temperature = 0f, maxOutputTokens = maxOutputTokens),
        )
        return try {
            session.prompt(listOf(LlmMessage(LlmRole.SYSTEM, prompt)))
            var text: String? = null
            session.events.collect { event ->
                when (event) {
                    is LlmEvent.Final -> text = event.text
                    is LlmEvent.Failed -> RagLog.w(TAG, "generation failed: ${event.error.message}")
                    else -> Unit
                }
            }
            text?.trim()?.takeIf { it.isNotEmpty() }
        } catch (failure: Exception) {
            RagLog.w(TAG, "generation failed: ${failure.message}")
            null
        } finally {
            session.close()
        }
    }

    override fun close() {
        engine.close()
    }

    companion object {
        private const val TAG = "SmolLm2Generator"

        /**
         * A short leash by default: the grammar wants one `TOOL:` line or one `ANSWER:` block,
         * and every extra token is CPU time spent after the useful output already ended.
         */
        const val DEFAULT_MAX_OUTPUT_TOKENS = 192

        /**
         * Load the pinned snapshot and wrap the engine, or null with a reported reason — a
         * missing snapshot degrades the host to retrieval-only, mirroring how a missing
         * embedding snapshot degrades retrieval to BM25. Loading is real I/O (~260 MB of
         * weights), so open once and keep it for the app's lifetime.
         */
        suspend fun open(
            modelDir: String? = null,
            maxOutputTokens: Int = DEFAULT_MAX_OUTPUT_TOKENS,
            onUnavailable: (String) -> Unit = {},
        ): SmolLm2Generator? = try {
            SmolLm2Generator(SmolLm2GeneratorCartridge.open(modelDir), maxOutputTokens)
        } catch (failure: Exception) {
            onUnavailable(
                "SmolLM2 engine failed to open (model snapshot missing?), answering is " +
                    "disabled: ${failure.message}",
            )
            null
        }
    }
}
