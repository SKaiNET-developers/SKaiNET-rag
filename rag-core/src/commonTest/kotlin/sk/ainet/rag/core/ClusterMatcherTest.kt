package sk.ainet.rag.core

import sk.ainet.rag.core.answer.ClusterMatcher
import sk.ainet.rag.core.bundle.Cluster
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClusterMatcherTest {

    private val clusters = listOf(
        Cluster(
            id = "lts-cadence",
            questions = listOf(
                "how often do lts releases come out",
                "when is the next lts release",
                "lts release schedule",
            ),
            answer = "Since Java 17, a new LTS release ships every two years.",
            chunkIds = listOf("java-lts-overview#release-timeline"),
        ),
        Cluster(
            id = "virtual-threads-availability",
            questions = listOf("which release has virtual threads", "are virtual threads final"),
            answer = "Virtual threads shipped as a final feature in Java 21.",
            chunkIds = listOf("java-21-api#virtual-threads"),
        ),
    )

    private val matcher = ClusterMatcher(clusters)

    @Test
    fun `a rephrased known question gets its canned answer`() {
        assertEquals("lts-cadence", matcher.match("when is the next lts release")?.id)
        assertEquals("virtual-threads-availability", matcher.match("which release has virtual threads")?.id)
    }

    @Test
    fun `an off-topic question matches no cluster`() {
        assertNull(matcher.match("how do i configure the garbage collector"))
    }

    @Test
    fun `one vague word is not an intent`() {
        assertNull(matcher.match("when"))
    }

    @Test
    fun `no clusters no matches`() {
        assertNull(ClusterMatcher(emptyList()).match("when is the next lts release"))
    }
}
