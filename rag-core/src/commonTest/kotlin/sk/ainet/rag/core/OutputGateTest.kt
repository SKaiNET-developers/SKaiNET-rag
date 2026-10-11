package sk.ainet.rag.core

import sk.ainet.rag.core.answer.OutputGate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OutputGateTest {

    private val passages = listOf(
        "Java 21 reached general availability on 2023-09-19 as a long-term support release.",
        "The compiler in Java 21 emits class files with major version 65.0. It bundles Unicode 15.0.0.",
    )

    private fun check(answer: String) = OutputGate.check(answer, passages)

    @Test
    fun `an answer drawn from the passages passes`() {
        assertEquals(
            OutputGate.Verdict.Allowed,
            check("Java 21 reached general availability on 2023-09-19 as a long-term support release."),
        )
    }

    @Test
    fun `a figure the passages never stated is rejected`() {
        val verdict = assertIs<OutputGate.Verdict.Rejected>(
            check("The compiler in Java 21 emits class files with major version 66.0."),
        )
        assertEquals(OutputGate.Reason.UNGROUNDED_DIGIT, verdict.reason)
        assertTrue("66.0" in verdict.detail)
    }

    @Test
    fun `a figure the passages do state is fine`() {
        assertEquals(
            OutputGate.Verdict.Allowed,
            check("The compiler in Java 21 emits class files with major version 65.0."),
        )
    }

    @Test
    fun `a sentence-final figure is not glued to its period`() {
        // Regression guard: if the figure pattern swallowed trailing punctuation, "15.0.0."
        // would differ from the passage's "15.0.0" and a correct answer would be rejected.
        assertEquals(
            OutputGate.Verdict.Allowed,
            OutputGate.check(
                "Java 21 bundles Unicode 15.0.0.",
                listOf("Java 21 bundles Unicode version 15.0.0"),
            ),
        )
    }

    @Test
    fun `a figure before a comma stays a figure`() {
        assertEquals(
            OutputGate.Verdict.Allowed,
            OutputGate.check(
                "Class files use major version 65.0, and the bundled Unicode is 15.0.0.",
                listOf("class files with major version 65.0\nbundled Unicode version 15.0.0"),
            ),
        )
    }

    @Test
    fun `a rounded figure is an invented figure`() {
        val verdict = assertIs<OutputGate.Verdict.Rejected>(
            OutputGate.check(
                "Class files use major version 65.",
                listOf("class files with major version 65.0"),
            ),
        )
        assertEquals(OutputGate.Reason.UNGROUNDED_DIGIT, verdict.reason)
    }

    @Test
    fun `advice and predictions are refused`() {
        listOf(
            "You should migrate to Java 21 now.",
            "We recommend adopting Java 21 for every service.",
            "This is a good time to move to Java 21.",
        ).forEach { answer ->
            val verdict = assertIs<OutputGate.Verdict.Rejected>(check(answer), answer)
            assertEquals(OutputGate.Reason.ADVISORY_LANGUAGE, verdict.reason)
        }
    }

    @Test
    fun `an answer about something else entirely is rejected`() {
        val verdict = assertIs<OutputGate.Verdict.Rejected>(
            check("Espresso extraction depends mostly on grind size and water temperature."),
        )
        assertEquals(OutputGate.Reason.LOW_OVERLAP, verdict.reason)
    }

    @Test
    fun `an unfinished sentence is rejected`() {
        val verdict = assertIs<OutputGate.Verdict.Rejected>(
            check("Java 21 reached general availability on 2023-09-19 as a"),
        )
        assertEquals(OutputGate.Reason.TRUNCATED, verdict.reason)
    }

    @Test
    fun `whitespace is not an answer`() {
        assertEquals(
            OutputGate.Reason.EMPTY,
            assertIs<OutputGate.Verdict.Rejected>(check("   ")).reason,
        )
    }

    @Test
    fun `a wall of text is rejected`() {
        val wall = "Java 21 reached general availability on 2023-09-19. ".repeat(40)
        assertEquals(
            OutputGate.Reason.TOO_LONG,
            assertIs<OutputGate.Verdict.Rejected>(check(wall)).reason,
        )
    }

    @Test
    fun `a question mark ends a sentence too`() {
        assertEquals(
            OutputGate.Verdict.Allowed,
            check("Did you know Java 21 reached general availability on 2023-09-19?"),
        )
    }
}
