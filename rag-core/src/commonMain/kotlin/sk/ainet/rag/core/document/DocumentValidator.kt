package sk.ainet.rag.core.document

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

data class ValidationConfig(
    val categories: List<String> = emptyList(),
    val screenUriPattern: String? = null,
    val staleAfterDays: Int = 180,
)

/**
 * Corpus-level checks that stop a build before it ships a broken bundle: id collisions, unknown
 * categories, malformed screen URIs, and documentation nobody has re-read within the freshness
 * window. All of them are errors — each one degrades retrieval or routing in a way no runtime
 * check can repair.
 */
class DocumentValidator(private val config: ValidationConfig) {

    fun validate(documents: List<ParsedDocument>, today: LocalDate): List<DocumentIssue> {
        val issues = mutableListOf<DocumentIssue>()
        reportDuplicateIds(documents, issues)
        for (document in documents) {
            checkCategory(document, issues)
            checkScreen(document, issues)
            checkFreshness(document, today, issues)
        }
        return issues
    }

    private fun reportDuplicateIds(documents: List<ParsedDocument>, issues: MutableList<DocumentIssue>) {
        for ((id, holders) in documents.groupBy { it.id }) {
            if (holders.size < 2) continue
            val paths = holders.map { it.sourcePath }.sorted()
            for (document in holders) {
                val elsewhere = paths.filter { it != document.sourcePath }.joinToString(", ")
                issues += error(
                    document,
                    "id `$id` is also used by $elsewhere. An id is a stable key — give the " +
                        "newer document its own",
                )
            }
        }
    }

    private fun checkCategory(document: ParsedDocument, issues: MutableList<DocumentIssue>) {
        if (config.categories.isEmpty() || document.category in config.categories) return
        issues += error(
            document,
            "category `${document.category}` is not configured; pick one of " +
                config.categories.joinToString(", "),
        )
    }

    private fun checkScreen(document: ParsedDocument, issues: MutableList<DocumentIssue>) {
        val screen = document.screen ?: return
        val pattern = config.screenUriPattern?.let(::Regex) ?: return
        if (pattern.matches(screen)) return
        issues += error(
            document,
            "screen `$screen` does not fit the configured pattern `${config.screenUriPattern}`",
        )
    }

    private fun checkFreshness(document: ParsedDocument, today: LocalDate, issues: MutableList<DocumentIssue>) {
        val reviewed = runCatching { LocalDate.parse(document.lastReviewed) }.getOrNull()
        if (reviewed == null) {
            issues += error(document, "last_reviewed `${document.lastReviewed}` cannot be parsed as a date")
            return
        }
        val ageDays = reviewed.daysUntil(today)
        if (ageDays > config.staleAfterDays) {
            issues += error(
                document,
                "last reviewed $ageDays days ago; the freshness window is ${config.staleAfterDays} " +
                    "days. Re-read the document and bump last_reviewed",
            )
        }
    }

    /** Front matter starts the file, so line 1 is where the offending field lives. */
    private fun error(document: ParsedDocument, message: String) =
        DocumentIssue(document.sourcePath, 1, message, DocumentIssue.Severity.ERROR)
}
