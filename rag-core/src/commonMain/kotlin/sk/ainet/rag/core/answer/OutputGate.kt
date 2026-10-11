package sk.ainet.rag.core.answer

import sk.ainet.rag.core.text.Tokenizer
import kotlin.math.roundToInt

/**
 * The last check between a generated answer and the user's eyes.
 *
 * Generation is buffered and judged whole — streaming a wrong figure and then retracting it is
 * not an option on a help surface. A rejected answer is simply dropped and the caller quotes a
 * retrieved passage instead: less fluent, never invented.
 */
object OutputGate {

    sealed interface Verdict {
        data object Allowed : Verdict

        data class Rejected(val reason: Reason, val detail: String) : Verdict
    }

    enum class Reason {
        UNGROUNDED_DIGIT,
        LOW_OVERLAP,
        ADVISORY_LANGUAGE,
        TRUNCATED,
        TOO_LONG,
        EMPTY,
    }

    /**
     * [minimumOverlap]: the fraction of the answer's content words that must occur somewhere in
     * the passages it claims to be based on.
     */
    data class Config(
        val minimumOverlap: Float = 0.5f,
        val maxCharacters: Int = 1200,
    )

    fun check(
        answer: String,
        passages: List<String>,
        config: Config = Config(),
    ): Verdict {
        val text = answer.trim()
        if (text.isEmpty()) return Verdict.Rejected(Reason.EMPTY, "no text")
        if (text.length > config.maxCharacters) {
            return Verdict.Rejected(Reason.TOO_LONG, "${text.length} characters")
        }
        checkFigures(text, passages)?.let { return it }
        checkAdvisory(text)?.let { return it }
        if (text.last() !in SENTENCE_ENDINGS) {
            return Verdict.Rejected(Reason.TRUNCATED, "does not end a sentence")
        }
        return checkGrounding(text, passages, config.minimumOverlap)
    }

    /**
     * Every figure in the answer must occur verbatim in a passage. Documentation is dense with
     * numbers — fees, limits, versions — and a model restating one approximately is the single
     * most damaging output this pipeline could produce, so rounding counts as inventing.
     */
    private fun checkFigures(text: String, passages: List<String>): Verdict.Rejected? {
        val known = passages.flatMapTo(mutableSetOf()) { FIGURE.findAll(it).map { m -> m.value } }
        val invented = FIGURE.findAll(text).map { it.value }.filter { it !in known }.toList()
        if (invented.isEmpty()) return null
        return Verdict.Rejected(
            Reason.UNGROUNDED_DIGIT,
            "not present in the passages: " + invented.distinct().joinToString(", "),
        )
    }

    private fun checkAdvisory(text: String): Verdict.Rejected? {
        val hit = ADVISORY.firstOrNull { text.contains(it, ignoreCase = true) } ?: return null
        return Verdict.Rejected(Reason.ADVISORY_LANGUAGE, hit)
    }

    private fun checkGrounding(text: String, passages: List<String>, minimumOverlap: Float): Verdict {
        val answerTerms = Tokenizer.tokenize(text)
        if (answerTerms.isEmpty()) return Verdict.Rejected(Reason.EMPTY, "no content words")
        val sourceTerms = passages.flatMapTo(mutableSetOf()) { Tokenizer.tokenize(it) }
        val overlap = answerTerms.count { it in sourceTerms }.toFloat() / answerTerms.size
        if (overlap < minimumOverlap) {
            return Verdict.Rejected(
                Reason.LOW_OVERLAP,
                "${(overlap * 100).roundToInt()}% of terms appear in the passages",
            )
        }
        return Verdict.Allowed
    }

    /**
     * A figure is digits with internal separators, anchored to END on a digit: "1,200" and "4.2"
     * match whole, while the period in "costs $12.50." or the comma in "$10, plus tax" stays
     * punctuation — otherwise every sentence-final figure would read as invented.
     */
    private val FIGURE = Regex("\\d(?:[\\d,.]*\\d)?")

    private val SENTENCE_ENDINGS = setOf('.', '!', '?')

    /**
     * Phrases that advise or predict. The corpus describes; recommendations and forecasts are
     * outside what retrieved documentation can ground.
     */
    val ADVISORY: List<String> = listOf(
        "should you", "you should", "we recommend", "i recommend", "our advice",
        "will rise", "will fall", "will go up", "will go down", "good time to",
        "best time to", "worth buying", "worth selling", "i suggest", "we suggest",
        "you ought to", "is likely to increase", "is likely to decrease", "guaranteed",
    )
}
