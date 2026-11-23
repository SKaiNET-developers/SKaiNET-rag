package sk.ainet.rag.core.document

/**
 * Minimal ISO-8601 calendar-date arithmetic: exactly what the freshness check needs, nothing
 * more. It exists so rag-core carries no datetime dependency — kotlinx-datetime's wasm artifact
 * imports `@js-joda/core` as a bare JS specifier, which only a bundler can resolve, and the
 * site-search engine ships the Kotlin compiler's raw ES-module output. One validator check does
 * not justify that cost on every target.
 */
internal object IsoDate {

    /** Days since 1970-01-01, or null when [iso] is not a valid `YYYY-MM-DD` calendar date. */
    fun epochDay(iso: String): Long? {
        val match = PATTERN.matchEntire(iso) ?: return null
        val (y, m, d) = match.destructured
        val year = y.toInt()
        val month = m.toInt()
        val day = d.toInt()
        if (month !in 1..12 || day !in 1..daysInMonth(year, month)) return null
        // Howard Hinnant's days-from-civil: count in 400-year eras of a March-based year, where
        // the leap day falls last and the day-of-year formula is a single linear expression.
        val shifted = year - if (month <= 2) 1 else 0
        val era = shifted.floorDiv(400)
        val yearOfEra = shifted - era * 400
        val dayOfYear = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return era * 146097L + dayOfEra - 719468L
    }

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 31
    }

    private val PATTERN = Regex("""(\d{4})-(\d{2})-(\d{2})""")
}
