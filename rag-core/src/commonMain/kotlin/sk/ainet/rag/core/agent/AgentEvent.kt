package sk.ainet.rag.core.agent

/**
 * What an agent turn is doing right now, surfaced so a host can animate progress instead of
 * freezing on a spinner. Purely observational — a host that drops every event gets the same
 * outcome. Events arrive on the calling coroutine, in execution order: [Thinking], a
 * [CallingTool]/[ToolFinished] pair per executed tool, and [JudgingAnswer] last.
 */
sealed interface AgentEvent {
    /** A model call is in flight: planning the next step or writing the final text. */
    data object Thinking : AgentEvent

    /** The plan named a tool; it is about to run with the arguments the model wrote. */
    data class CallingTool(val name: String, val args: Map<String, String>) : AgentEvent

    /** The tool came back. [sourceTitles] credits the documents a search tool consulted. */
    data class ToolFinished(
        val name: String,
        val observationChars: Int,
        val sourceTitles: List<String>,
    ) : AgentEvent

    /** The candidate answer exists and the output gate is weighing it against the evidence. */
    data object JudgingAnswer : AgentEvent
}
