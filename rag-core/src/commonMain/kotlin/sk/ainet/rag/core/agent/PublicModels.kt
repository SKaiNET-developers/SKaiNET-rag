package sk.ainet.rag.core.agent

/** Which retrieval path produced a passage. */
enum class MatchSource { BM25, VECTOR, HYBRID, PRECOMPUTED }

/**
 * A capability the host lends the agent for one turn. The split of responsibilities is strict:
 * the loop decides *when* to call and judges what comes back; what the function actually reads,
 * formats or computes is entirely the host's business.
 *
 * @param argSpec one line of usage shown to the model, e.g. "category=release, language or api".
 * @param execute performs the call; returning null (bad arguments, failed read) ends the turn
 *   honestly instead of letting the model improvise around a hole.
 */
data class ToolDefinition(
    val name: String,
    val description: String,
    val argSpec: String,
    val execute: suspend (args: Map<String, String>) -> ToolResult?,
)

/** What one tool call observed, in the exact text the transcript will carry. */
data class ToolResult(
    val tool: String,
    val text: String,
    val sourceTitles: List<String> = emptyList(),
)

/**
 * Host-supplied configuration for a turn. The division of labor: the loop owns the grammar, the
 * round budget and the gate; every word the model reads — system prompt, tool copy — comes from
 * here.
 */
data class AgentConfig(
    val systemPrompt: String,
    val tools: List<ToolDefinition>,
    val maxToolRounds: Int = 3,
    val maxConversationalChars: Int = 200,
    /**
     * Character budget for earlier turns riding along in the prompt, trimmed oldest-first.
     * A measured turn costs roughly 500 tokens, so this default carries several follow-ups
     * while capping the prompt no matter how long the conversation runs.
     */
    val maxHistoryChars: Int = 1500,
    /**
     * Passages the host already retrieved for this question, best first. They enter the
     * transcript above the question and count as grounding evidence, exactly as if a search
     * tool had fetched them — fresh retrieval, zero model rounds spent. Empty means none.
     */
    val documentation: List<Passage> = emptyList(),
)

/**
 * One earlier exchange, handed in per call. The loop holds no state between turns: history is
 * input for resolving follow-ups ("and in older releases?"), never evidence — the gate only
 * accepts facts from this turn's observations.
 */
enum class AgentRole { USER, MODEL }

data class AgentMessage(val role: AgentRole, val text: String)

/**
 * Wall-clock breakdown of one turn, all in milliseconds. [generateMs] sums the model calls —
 * dominated on CPU by prompt re-reading, since each round starts from an empty KV cache.
 * [toolMs] sums the host functions, which read local data and usually cost nothing visible.
 */
data class AgentTimings(
    val totalMs: Long,
    val generateMs: Long,
    val toolMs: Long,
    /** Model calls made, counting a forced final answer when one was needed. */
    val rounds: Int,
)

/** How a turn ended. Hosts render [Final]; the other two map to fixed copy of the host's choosing. */
sealed interface AgentOutcome {
    data class Final(
        val text: String,
        val sources: List<String>,
        val usedTools: List<String>,
        val timings: AgentTimings,
    ) : AgentOutcome

    /** Off-grammar output, a failed tool, or a gate rejection — nothing safe to show. */
    data class Unresolved(val timings: AgentTimings) : AgentOutcome

    /** The answer gave advice or predictions; the host shows its refusal text. */
    data class Refused(val timings: AgentTimings) : AgentOutcome
}

/**
 * One retrieved section, display-ready: [text] keeps its Markdown. [screenLink] is the optional
 * deep link to the screen this documentation describes, for hosts that can navigate.
 */
data class Passage(
    val chunkId: String,
    val docId: String,
    val title: String,
    val heading: String?,
    val text: String,
    val screenLink: String?,
    val score: Float,
    val source: MatchSource,
)

sealed interface LocalRagState {
    data object Idle : LocalRagState

    data object Loading : LocalRagState

    /**
     * Answering. [usingVectors] false = hybrid degraded to BM25-only, either because no embedder
     * is configured or because the bundle's vectors failed parity — a supported state, not an
     * error.
     */
    data class Ready(
        val chunkCount: Int,
        val contentVersion: Int,
        val usingVectors: Boolean,
        val canGenerate: Boolean,
    ) : LocalRagState

    data class Failed(val reason: String) : LocalRagState
}
