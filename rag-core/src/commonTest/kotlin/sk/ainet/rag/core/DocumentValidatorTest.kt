package sk.ainet.rag.core

import sk.ainet.rag.core.document.DocumentStatus
import sk.ainet.rag.core.document.DocumentValidator
import sk.ainet.rag.core.document.ParsedDocument
import sk.ainet.rag.core.document.ValidationConfig
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocumentValidatorTest {

    private val today = LocalDate.parse("2026-10-11")

    private fun document(
        id: String = "java-21-api",
        category: String = "api",
        screen: String? = null,
        lastReviewed: String = "2026-10-01",
        path: String = "java-21-api.md",
    ) = ParsedDocument(
        id = id,
        title = "Java 21 API changes",
        category = category,
        screen = screen,
        lastReviewed = lastReviewed,
        status = DocumentStatus.PUBLISHED,
        body = "## Virtual Threads\ntext",
        sourcePath = path,
    )

    private val validator = DocumentValidator(
        ValidationConfig(
            categories = listOf("api", "language"),
            screenUriPattern = "^app://[a-z0-9/-]+$",
            staleAfterDays = 180,
        ),
    )

    @Test
    fun `a healthy document raises nothing`() {
        assertTrue(validator.validate(listOf(document()), today).isEmpty())
    }

    @Test
    fun `an id collision points each file at the other`() {
        val issues = validator.validate(
            listOf(document(path = "a.md"), document(path = "b.md")),
            today,
        )
        assertEquals(2, issues.size)
        assertTrue(issues.any { it.sourcePath == "a.md" && "b.md" in it.message })
        assertTrue(issues.any { it.sourcePath == "b.md" && "a.md" in it.message })
    }

    @Test
    fun `an unconfigured category names the valid choices`() {
        val issue = validator.validate(listOf(document(category = "gossip")), today).single()
        assertTrue("api, language" in issue.message, issue.message)
    }

    @Test
    fun `screen links are held to the configured pattern`() {
        assertTrue(validator.validate(listOf(document(screen = "app://docs/java-21")), today).isEmpty())
        val issue = validator.validate(listOf(document(screen = "https://x.com")), today).single()
        assertTrue("does not fit" in issue.message)
    }

    @Test
    fun `staleness reports the actual age`() {
        val issue = validator.validate(listOf(document(lastReviewed = "2025-01-01")), today).single()
        assertTrue("648 days ago" in issue.message, issue.message)
    }

    @Test
    fun `the fixture corpus passes validation whole`() {
        assertTrue(
            DocumentValidator(
                ValidationConfig(
                    categories = listOf("release", "language", "jvm", "api", "tools"),
                    staleAfterDays = 3650,
                ),
            ).validate(Fixture.documents, today).isEmpty(),
        )
    }
}
