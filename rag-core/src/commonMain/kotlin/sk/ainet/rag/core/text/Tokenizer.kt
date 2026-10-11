package sk.ainet.rag.core.text

/**
 * One tokenizer for both sides of the parity contract: the bundler indexes through this object
 * and the on-device runtime queries through the same compiled code. Any split tokenization would
 * fail silently — scores sag, nothing throws — so this stays a single dependency-free object.
 *
 * Rules: case-fold to lowercase, keep letters, digits and the two symbols that carry meaning in
 * documentation (`&`, `%`), treat every other character as a separator, drop stopwords. No
 * stemming; BM25's idf already demotes ubiquitous terms, and stemming would be one more thing to
 * keep in lockstep across build and device.
 */
object Tokenizer {

    fun tokenize(text: String): List<String> {
        val words = ArrayList<String>()
        var start = -1
        for (i in text.indices) {
            val c = text[i]
            if (c.isLetterOrDigit() || c == '&' || c == '%') {
                if (start < 0) start = i
            } else if (start >= 0) {
                collect(text, start, i, words)
                start = -1
            }
        }
        if (start >= 0) collect(text, start, text.length, words)
        return words
    }

    private fun collect(text: String, from: Int, until: Int, into: MutableList<String>) {
        val word = text.substring(from, until).lowercase()
        if (word !in STOPWORDS) into.add(word)
    }

    /**
     * Kept intentionally small: only glue words with no retrieval value on their own. Longer
     * lists start deleting words that distinguish documents in technical prose.
     */
    val STOPWORDS: Set<String> = setOf(
        "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is",
        "it", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they",
        "this", "to", "was", "will", "with",
    )
}
