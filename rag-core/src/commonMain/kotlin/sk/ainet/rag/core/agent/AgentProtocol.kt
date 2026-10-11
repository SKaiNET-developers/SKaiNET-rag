package sk.ainet.rag.core.agent

/**
 * The two-keyword wire format between the loop and the model: a turn is either one
 * `TOOL: name|key=value;…` line or an `ANSWER: …` block, and nothing else.
 *
 * Output that fails to parse is discarded, never patched up — lenient parsing of small-model
 * output is a hallucination amplifier. The sole tolerance is case: logs show 135M–270M models
 * emitting "Tool:" and "Answer:", so keywords, tool names and argument keys fold to lowercase
 * while argument values keep theirs (they may be search text).
 */
internal object AgentProtocol {

    sealed interface Parsed {
        data class ToolCall(val name: String, val args: Map<String, String>) : Parsed

        data class FinalAnswer(val text: String) : Parsed
    }

    fun parse(raw: String?, allowedTools: Set<String>): Parsed? {
        val directive = raw?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim() ?: return null
        return when {
            directive.startsWith(TOOL_PREFIX, ignoreCase = true) ->
                readToolCall(directive.substringAfter(":"), allowedTools)

            directive.startsWith(ANSWER_PREFIX, ignoreCase = true) ->
                raw.substringAfter(":").trim().takeIf { it.isNotEmpty() }?.let(Parsed::FinalAnswer)

            else -> null
        }
    }

    /**
     * Does any line of [text] look like a directive? The prose path needs to know: a "chatty"
     * reply hiding a `TOOL:` line further down is a protocol violation trying to reach the UI,
     * not conversation.
     */
    fun containsDirective(text: String): Boolean = text.lineSequence().any { line ->
        val candidate = line.trim()
        candidate.startsWith(TOOL_PREFIX, ignoreCase = true) ||
            candidate.startsWith(ANSWER_PREFIX, ignoreCase = true)
    }

    private fun readToolCall(body: String, allowedTools: Set<String>): Parsed.ToolCall? {
        val name = body.substringBefore("|").trim().lowercase()
        if (name.isEmpty() || name !in allowedTools) return null

        val args = mutableMapOf<String, String>()
        val argList = body.substringAfter("|", "")
        if (argList.isNotBlank()) {
            for (pair in argList.split(";")) {
                if (pair.isBlank()) continue
                val key = pair.substringBefore("=").trim().lowercase()
                val value = pair.substringAfter("=", "").trim()
                if (key.isEmpty() || value.isEmpty()) return null
                args[key] = value
            }
        }
        return Parsed.ToolCall(name, args)
    }

    private const val TOOL_PREFIX = "TOOL:"
    private const val ANSWER_PREFIX = "ANSWER:"
}
