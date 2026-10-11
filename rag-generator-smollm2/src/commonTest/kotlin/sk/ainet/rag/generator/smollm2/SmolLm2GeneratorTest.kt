package sk.ainet.rag.generator.smollm2

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class SmolLm2GeneratorTest {

    /**
     * Weight loading plus two greedy turns on CPU: minutes, not runTest's default seconds.
     * Like the LEAF tests, a missing snapshot skips rather than fails — CI machines without
     * the blueprint's fetch step still run every other test.
     */
    @Test
    fun generatesDeterministicallyWhenSnapshotPresent() = runTest(timeout = 10.minutes) {
        var reason: String? = null
        val generator = SmolLm2Generator.open(maxOutputTokens = 24) { reason = it }
        if (generator == null) {
            println("SKIPPED generatesDeterministicallyWhenSnapshotPresent: $reason")
            return@runTest
        }
        try {
            val prompt = "Answer in one short sentence: what is a database index for?"
            val first = generator.generate(prompt)
            assertNotNull(first, "generate returned null with the snapshot present")
            assertTrue(first.isNotBlank())

            val again = generator.generate(prompt)
            assertEquals(first, again, "greedy decoding must replay to the identical text")
        } finally {
            generator.close()
        }
    }
}
