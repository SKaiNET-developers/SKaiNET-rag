package sk.ainet.rag.core.agent

import kotlin.time.TimeSource

/**
 * Logging seam for the agent machinery. The core has no platform logger; the host installs a
 * sink once (Android: android.util.Log, JVM: stdout or slf4j) and silence is the default so
 * commonTest runs stay quiet.
 */
object RagLog {
    enum class Level { DEBUG, WARN }

    var sink: (level: Level, tag: String, message: String) -> Unit = { _, _, _ -> }

    fun d(tag: String, message: String) = sink(Level.DEBUG, tag, message)
    fun w(tag: String, message: String) = sink(Level.WARN, tag, message)
}

/**
 * Monotonic wall-clock substitute for timing agent turns. Only deltas are ever reported, so a
 * monotonic origin is strictly better than epoch time here (immune to clock adjustments).
 */
internal object AgentClock {
    private val origin = TimeSource.Monotonic.markNow()
    fun now(): Long = origin.elapsedNow().inWholeMilliseconds
}
