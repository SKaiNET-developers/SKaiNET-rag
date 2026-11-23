package sk.ainet.rag.core.document

/**
 * The one slug rule shared by everything that must agree on anchors: the chunker (chunk ids and
 * [Chunk.anchor]) and any page renderer emitting `id=` attributes. One implementation, so links
 * are correct by construction rather than by two algorithms staying coincidentally equal.
 */
object Slugs {

    /** Deterministic: rebuilding an unchanged document reproduces every slug. */
    fun of(heading: String?): String {
        if (heading == null) return "intro"
        val slug = buildString {
            for (c in heading.lowercase()) append(if (c.isLetterOrDigit()) c else '-')
        }.trim('-').replace(SQUEEZED_DASHES, "-")
        return slug.ifEmpty { "section" }
    }

    private val SQUEEZED_DASHES = Regex("-+")
}
