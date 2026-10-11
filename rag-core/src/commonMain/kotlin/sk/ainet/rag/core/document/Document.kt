package sk.ainet.rag.core.document

/** Lifecycle of a source document. Bundles are built from [PUBLISHED] documents only. */
enum class DocumentStatus {
    DRAFT,
    PUBLISHED,
    DEPRECATED,
    ;

    companion object {
        fun from(raw: String): DocumentStatus? =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
    }
}

/**
 * A source Markdown file with its front matter lifted into fields.
 *
 * [body] is everything below the closing front-matter delimiter, exactly as written — chunking
 * and Markdown stripping happen later, each on its own copy.
 */
data class ParsedDocument(
    val id: String,
    val title: String,
    val category: String,
    val aliases: List<String> = emptyList(),
    val screen: String? = null,
    val appVersionMin: String? = null,
    val appVersionMax: String? = null,
    val lastReviewed: String,
    val status: DocumentStatus,
    val body: String,
    val sourcePath: String,
)

/**
 * A problem found in a source document, addressed to the person who can fix it: the content
 * author. That is why the position is a file and a line, never an internal index.
 */
data class DocumentIssue(
    val sourcePath: String,
    val line: Int,
    val message: String,
    val severity: Severity,
) {
    enum class Severity { WARNING, ERROR }

    override fun toString(): String = "$sourcePath:$line: ${severity.name.lowercase()}: $message"
}
