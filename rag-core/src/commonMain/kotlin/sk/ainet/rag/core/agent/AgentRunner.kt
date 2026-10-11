package sk.ainet.rag.core.agent

import sk.ainet.rag.core.agent.RagLog as Log
import sk.ainet.rag.core.answer.OutputGate

/**
 * The tool loop: plan in the [AgentProtocol] grammar, run host tools, paste observations back,
 * and pass the final text through the output gate before anyone sees it.
 *
 * The loop brings mechanism, the host brings meaning — every prompt word and every tool arrives
 * via [AgentConfig]. And it never negotiates with bad output: off-grammar text, an unknown or
 * failing tool, a smuggled directive, a gate rejection — each ends the turn as
 * [AgentOutcome.Unresolved] (or [AgentOutcome.Refused] for advisory language), because the
 * fallback copy is always safer than a repaired guess.
 */
class AgentRunner {

    suspend fun answer(
        generate: suspend (String) -> String?,
        query: String,
        config: AgentConfig,
        history: List<AgentMessage> = emptyList(),
        onEvent: (AgentEvent) -> Unit = {},
    ): AgentOutcome = Turn(generate, query, config, history, onEvent).run()

    /** All mutable state of one turn lives here; the runner itself stays stateless. */
    private class Turn(
        private val generate: suspend (String) -> String?,
        private val query: String,
        private val config: AgentConfig,
        private val history: List<AgentMessage>,
        private val onEvent: (AgentEvent) -> Unit,
    ) {
        private val toolbox = config.tools.associateBy { it.name.lowercase() }
        private val transcript = StringBuilder()
        private val observations = mutableListOf<String>()
        private val sources = mutableListOf<String>()
        private val usedTools = mutableListOf<String>()
        private var previousCall: Pair<String, Map<String, String>>? = null

        private val startedAt = AgentClock.now()
        private var generateMs = 0L
        private var toolMs = 0L
        private var rounds = 0

        suspend fun run(): AgentOutcome {
            Log.d(TAG, "agent turn: \"$query\"")
            prepareTranscript()

            repeat(config.maxToolRounds) { round ->
                onEvent(AgentEvent.Thinking)
                val reply = timedGenerate(transcript.toString() + "\nQuestion: $query\n")
                Log.d(TAG, "round $round: ${reply?.lineSequence()?.firstOrNull().orEmpty().take(160)}")

                when (val step = AgentProtocol.parse(reply, toolbox.keys)) {
                    null -> return handleProse(reply, round)

                    is AgentProtocol.Parsed.ToolCall -> {
                        if (step.name to step.args == previousCall) {
                            // The model wants an observation it already has. The answer can only
                            // come from the transcript as it stands, so skip straight to it
                            // rather than paying another prefill to learn nothing new.
                            Log.w(TAG, "round $round: duplicate call to ${step.name}, going to the answer")
                            return forceAnswer()
                        }
                        previousCall = step.name to step.args
                        if (!executeTool(step, round)) return AgentOutcome.Unresolved(timings())
                    }

                    is AgentProtocol.Parsed.FinalAnswer -> {
                        if (AgentProtocol.containsDirective(step.text)) {
                            Log.w(TAG, "round $round: directive inside the answer text")
                            return AgentOutcome.Unresolved(timings())
                        }
                        return judge(step.text)
                    }
                }
            }
            return forceAnswer()
        }

        /** System prompt, bounded history, then any pre-searched documentation as free evidence. */
        private fun prepareTranscript() {
            transcript.append(config.systemPrompt)
            transcript.append(historyBlock())
            if (config.documentation.isEmpty()) return
            transcript.append(documentationBlock(config.documentation))
            observations += config.documentation.map { it.text }
            sources += config.documentation.map { listOfNotNull(it.title, it.heading).joinToString(" — ") }
        }

        private suspend fun timedGenerate(prompt: String): String? {
            val before = AgentClock.now()
            val reply = generate(prompt)
            generateMs += AgentClock.now() - before
            rounds++
            return reply
        }

        /**
         * Unparseable output is usually fatal — with one carve-out. A first-round reply that is
         * pure prose, cites nothing and hides no directive gets judged as small talk ("hello!"
         * deserves an answer). Once observations exist, prose is an unverifiable claim and ends
         * the turn.
         */
        private fun handleProse(reply: String?, round: Int): AgentOutcome {
            val prose = reply?.trim().orEmpty()
            val acceptable = prose.isNotEmpty() && observations.isEmpty() &&
                !AgentProtocol.containsDirective(prose) && looksConversational(prose)
            if (!acceptable) {
                Log.w(TAG, "round $round: off-grammar reply, turn unresolved")
                return AgentOutcome.Unresolved(timings())
            }
            Log.d(TAG, "round $round: judging plain prose as conversation")
            return judge(prose)
        }

        /** Runs one planned call; false means the turn cannot continue. */
        private suspend fun executeTool(step: AgentProtocol.Parsed.ToolCall, round: Int): Boolean {
            val tool = toolbox.getValue(step.name)
            onEvent(AgentEvent.CallingTool(step.name, step.args))
            val before = AgentClock.now()
            val observation = runCatching { tool.execute(step.args) }.getOrNull()
            val spent = AgentClock.now() - before
            toolMs += spent
            if (observation == null) {
                Log.w(TAG, "round $round: tool ${step.name} returned nothing")
                return false
            }
            Log.d(TAG, "round $round: ${step.name} observed ${observation.text.length} chars in ${spent}ms")
            onEvent(AgentEvent.ToolFinished(step.name, observation.text.length, observation.sourceTitles))
            observations += observation.text
            sources += observation.sourceTitles
            usedTools += observation.tool
            transcript.append("TOOL: ${step.name}\n")
            transcript.append("OBSERVATION [${observation.tool}]:\n${observation.text}\n")
            return true
        }

        /** The round budget is spent (or wasted): demand one ANSWER: line and judge it. */
        private suspend fun forceAnswer(): AgentOutcome {
            onEvent(AgentEvent.Thinking)
            val reply = timedGenerate(transcript.toString() + "\nQuestion: $query\n" + FORCE_ANSWER_SUFFIX)
            Log.d(TAG, "forced: ${reply?.lineSequence()?.firstOrNull().orEmpty().take(160)}")
            val step = AgentProtocol.parse(reply, toolbox.keys)
            if (step !is AgentProtocol.Parsed.FinalAnswer || AgentProtocol.containsDirective(step.text)) {
                Log.w(TAG, "forced answer still off-grammar, turn unresolved")
                return AgentOutcome.Unresolved(timings())
            }
            return judge(step.text)
        }

        /**
         * The shared gate, with tool observations as the evidence. The query loses its digits
         * before joining the evidence pool: a number the user typed may be echoed, but only an
         * observation can introduce one.
         */
        private fun judge(candidate: String): AgentOutcome {
            onEvent(AgentEvent.JudgingAnswer)
            val answer = candidate.trim()
            if (answer.isEmpty()) {
                Log.w(TAG, "gate: empty answer")
                return AgentOutcome.Unresolved(timings())
            }
            val evidence = observations + query.filterNot(Char::isDigit)
            return when (val verdict = OutputGate.check(answer, evidence)) {
                OutputGate.Verdict.Allowed -> {
                    Log.d(TAG, "gate: allowed, ${answer.length} chars on ${observations.size} observations")
                    AgentOutcome.Final(answer, sources.distinct(), usedTools.distinct(), timings())
                }

                is OutputGate.Verdict.Rejected -> when {
                    verdict.reason == OutputGate.Reason.ADVISORY_LANGUAGE -> {
                        Log.w(TAG, "gate: advisory language (${verdict.detail})")
                        AgentOutcome.Refused(timings())
                    }

                    // With no observations and no digits there is nothing to misstate; short,
                    // complete prose passes as conversation. Advisory text was caught above.
                    observations.isEmpty() && looksConversational(answer) -> {
                        Log.d(TAG, "gate: conversational answer admitted")
                        AgentOutcome.Final(answer, emptyList(), emptyList(), timings())
                    }

                    else -> {
                        Log.w(TAG, "gate: rejected, ${verdict.reason}: ${verdict.detail}")
                        AgentOutcome.Unresolved(timings())
                    }
                }
            }
        }

        private fun looksConversational(text: String): Boolean =
            text.length <= config.maxConversationalChars &&
                text.none(Char::isDigit) &&
                text.last() in SENTENCE_ENDINGS

        /**
         * Recent turns under [AgentConfig.maxHistoryChars], dropped oldest-first — except the
         * most recent turn, which always rides along even over budget: a follow-up with no
         * antecedent cannot be answered at any price. History frames the question; it is never
         * evidence.
         */
        private fun historyBlock(): String {
            val kept = mutableListOf<AgentMessage>()
            var budget = 0
            for (message in history.asReversed()) {
                if (message.text.isBlank()) continue
                if (kept.isNotEmpty() && budget + message.text.length > config.maxHistoryChars) break
                kept += message
                budget += message.text.length
            }
            if (kept.isEmpty()) return ""
            return kept.asReversed().joinToString(
                separator = "\n",
                prefix = "Earlier in this conversation:\n",
                postfix = "\n",
            ) { (if (it.role == AgentRole.USER) "User: " else "Assistant: ") + it.text.trim() }
        }

        /** Same title-and-text layout a search tool pastes, so the model sees a familiar shape. */
        private fun documentationBlock(passages: List<Passage>): String = passages.joinToString(
            separator = "\n---\n",
            prefix = "Documentation pre-searched for this question " +
                "(use it if it answers the question, ignore it otherwise):\n",
            postfix = "\n",
        ) { "${it.title} — ${it.heading ?: "overview"}\n${it.text}" }

        private fun timings() = AgentTimings(AgentClock.now() - startedAt, generateMs, toolMs, rounds)
    }

    private companion object {
        const val TAG = "LocalRagAgent"

        const val FORCE_ANSWER_SUFFIX =
            "No more tool calls. Reply now with one ANSWER: line using only the observations above.\n"

        val SENTENCE_ENDINGS = charArrayOf('.', '!', '?')
    }
}
