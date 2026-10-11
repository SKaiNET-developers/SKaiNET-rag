package sk.ainet.rag.core.index

/**
 * Decides whether a chunk's declared `app_version_min`/`app_version_max` range admits the
 * installed app version.
 *
 * Runs after fusion by design: dropping a chunk before fusion would leave its rank slot occupied
 * in one list and absent in another, skewing the fused order for everything below it.
 */
object VersionFilter {

    fun matches(appVersion: String, minimum: String?, maximum: String?): Boolean {
        val installed = numbersOf(appVersion) ?: return true
        minimum?.let(::numbersOf)?.let { if (compare(installed, it) < 0) return false }
        maximum?.let(::numbersOf)?.let { if (compare(installed, it) > 0) return false }
        return true
    }

    /**
     * `1.2.3-beta+42` reads as `[1, 2, 3]`; anything unparseable reads as "no constraint".
     * Being lenient is deliberate — a bad version string must never make documentation vanish.
     */
    private fun numbersOf(version: String): List<Int>? {
        val numeric = version.trim().substringBefore('-').substringBefore('+')
        if (numeric.isEmpty()) return null
        val parts = numeric.split('.').map { it.toIntOrNull() ?: return null }
        return parts.ifEmpty { null }
    }

    /** Component-wise, with missing components reading as zero: 1.2 == 1.2.0. */
    private fun compare(left: List<Int>, right: List<Int>): Int {
        for (index in 0 until maxOf(left.size, right.size)) {
            val difference = left.getOrElse(index) { 0 }.compareTo(right.getOrElse(index) { 0 })
            if (difference != 0) return difference
        }
        return 0
    }
}
