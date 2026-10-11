package sk.ainet.rag.core.document

/**
 * Parses the `---`-delimited front matter heading a source Markdown file.
 *
 * The accepted grammar is a fixed sliver of YAML: `key: value` scalars (bare or quoted), inline
 * `[a, b]` lists, `null`/`~`, comments and blank lines. The document schema never needs more, and
 * pulling in a YAML parser would dwarf the library it serves. Anything the sliver cannot read is
 * an error naming the file and the line — never a silent guess.
 */
object FrontMatterParser {

    data class ParseResult(val document: ParsedDocument?, val issues: List<DocumentIssue>)

    fun parse(sourcePath: String, content: String): ParseResult {
        val lines = content.lines()

        val opening = lines.indexOfFirst { it.isNotBlank() }
        if (opening == -1 || lines[opening].trim() != FENCE) {
            return failure(sourcePath, 1, "no front matter found: the first line must be ---")
        }
        val closing = (opening + 1 until lines.size).firstOrNull { lines[it].trim() == FENCE }
            ?: return failure(sourcePath, opening + 1, "front matter opened here but never closed with ---")

        val issues = mutableListOf<DocumentIssue>()
        val fields = readFields(sourcePath, lines, opening + 1, closing, issues)
        val body = lines.drop(closing + 1).joinToString("\n").trim()
        return ParseResult(assemble(sourcePath, fields, body, issues), issues)
    }

    /** A field's raw value plus the line it was declared on, for error reporting. */
    private data class Field(val raw: String, val line: Int)

    private fun readFields(
        sourcePath: String,
        lines: List<String>,
        from: Int,
        until: Int,
        issues: MutableList<DocumentIssue>,
    ): Map<String, Field> {
        val fields = mutableMapOf<String, Field>()
        for (index in from until until) {
            val line = lines[index]
            val lineNumber = index + 1
            if (line.isBlank() || line.trimStart().startsWith("#")) continue

            val colon = line.indexOf(':')
            if (colon <= 0) {
                issues += error(sourcePath, lineNumber, "not a `key: value` line: `${line.trim()}`")
                continue
            }
            val key = line.take(colon).trim()
            if (key in fields) {
                issues += error(sourcePath, lineNumber, "field `$key` declared twice")
                continue
            }
            fields[key] = Field(line.substring(colon + 1).trim(), lineNumber)
        }
        return fields
    }

    private fun assemble(
        sourcePath: String,
        fields: Map<String, Field>,
        body: String,
        issues: MutableList<DocumentIssue>,
    ): ParsedDocument? {
        fun require(key: String): String? {
            val field = fields[key]
            val value = field?.let { scalar(it.raw) }
            if (value == null) {
                issues += error(sourcePath, field?.line ?: 1, "required field `$key` is missing")
            }
            return value
        }

        val id = require("id")
        val title = require("title")
        val category = require("category")
        val lastReviewed = require("last_reviewed")
        val status = require("status")?.let { raw ->
            DocumentStatus.from(raw) ?: run {
                issues += error(
                    sourcePath, fields.getValue("status").line,
                    "status `$raw` is not one of draft, published, deprecated",
                )
                null
            }
        }

        if (id != null && !KEBAB_ID.matches(id)) {
            issues += error(
                sourcePath, fields.getValue("id").line,
                "id `$id` must be kebab-case (lowercase letters, digits, single hyphens)",
            )
        }
        if (lastReviewed != null && !ISO_DATE.matches(lastReviewed)) {
            issues += error(
                sourcePath, fields.getValue("last_reviewed").line,
                "last_reviewed `$lastReviewed` is not an ISO date like 2026-09-01",
            )
        }

        if (id == null || title == null || category == null || lastReviewed == null || status == null) {
            return null
        }
        return ParsedDocument(
            id = id,
            title = title,
            category = category,
            aliases = fields["aliases"]?.let { inlineList(it.raw) } ?: emptyList(),
            screen = fields["screen"]?.let { scalar(it.raw) },
            appVersionMin = fields["app_version_min"]?.let { scalar(it.raw) },
            appVersionMax = fields["app_version_max"]?.let { scalar(it.raw) },
            lastReviewed = lastReviewed,
            status = status,
            body = body,
            sourcePath = sourcePath,
        )
    }

    /** Strips quotes; an empty value, `null` or `~` reads as absent. */
    private fun scalar(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty() || value == "null" || value == "~") return null
        return value.removeSurrounding("\"").removeSurrounding("'")
    }

    private fun inlineList(raw: String): List<String> {
        val value = raw.trim()
        if (value.isEmpty() || value == "null" || value == "[]") return emptyList()
        return value.removeSurrounding("[", "]").split(",").mapNotNull { scalar(it) }
    }

    private fun failure(sourcePath: String, line: Int, message: String) =
        ParseResult(null, listOf(error(sourcePath, line, message)))

    private fun error(sourcePath: String, line: Int, message: String) =
        DocumentIssue(sourcePath, line, message, DocumentIssue.Severity.ERROR)

    private const val FENCE = "---"
    private val KEBAB_ID = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    private val ISO_DATE = Regex("^\\d{4}-\\d{2}-\\d{2}$")
}
