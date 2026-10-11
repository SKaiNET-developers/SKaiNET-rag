package sk.ainet.rag.core

import sk.ainet.rag.core.document.DocumentIssue
import sk.ainet.rag.core.document.DocumentStatus
import sk.ainet.rag.core.document.FrontMatterParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FrontMatterParserTest {

    private val wellFormed = """
        ---
        id: java-21-api
        title: Java 21 API changes
        category: api
        aliases: [JEP 444, JEP 431]
        screen: app://docs/java-21
        app_version_min: "4.2"
        app_version_max: null
        last_reviewed: 2026-10-10
        status: published
        ---

        ## Virtual Threads
        Lightweight threads arrive as a final feature.
    """.trimIndent()

    @Test
    fun `reads every declared field`() {
        val result = FrontMatterParser.parse("java-21-api.md", wellFormed)
        val document = assertNotNull(result.document)
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertEquals("java-21-api", document.id)
        assertEquals("Java 21 API changes", document.title)
        assertEquals("api", document.category)
        assertEquals(listOf("JEP 444", "JEP 431"), document.aliases)
        assertEquals("app://docs/java-21", document.screen)
        assertEquals("4.2", document.appVersionMin)
        assertNull(document.appVersionMax)
        assertEquals(DocumentStatus.PUBLISHED, document.status)
        assertTrue(document.body.startsWith("## Virtual Threads"))
    }

    @Test
    fun `a file without front matter fails at line 1`() {
        val result = FrontMatterParser.parse("bad.md", "# Just a heading\n")
        assertNull(result.document)
        assertEquals(1, result.issues.single().line)
        assertTrue("no front matter" in result.issues.single().message)
    }

    @Test
    fun `an unterminated block is reported where it opened`() {
        val result = FrontMatterParser.parse("bad.md", "---\nid: x\ntitle: y\n")
        assertNull(result.document)
        assertTrue("never closed" in result.issues.single().message)
    }

    @Test
    fun `a missing required field is named`() {
        val result = FrontMatterParser.parse(
            "bad.md",
            "---\nid: x\ntitle: y\nlast_reviewed: 2026-01-01\nstatus: published\n---\nbody",
        )
        assertNull(result.document)
        assertTrue(result.issues.any { "`category`" in it.message }, result.issues.toString())
    }

    @Test
    fun `an invalid status points at its own line`() {
        val result = FrontMatterParser.parse(
            "bad.md",
            "---\nid: x\ntitle: y\ncategory: api\nlast_reviewed: 2026-01-01\nstatus: live\n---\nbody",
        )
        val issue = result.issues.single { "status `live`" in it.message }
        assertEquals(6, issue.line)
    }

    @Test
    fun `an id must be kebab-case`() {
        val result = FrontMatterParser.parse(
            "bad.md",
            "---\nid: Java_21\ntitle: y\ncategory: api\nlast_reviewed: 2026-01-01\nstatus: published\n---\nbody",
        )
        assertTrue(result.issues.any { "kebab-case" in it.message })
    }

    @Test
    fun `a review date must be ISO formatted`() {
        val result = FrontMatterParser.parse(
            "bad.md",
            "---\nid: x\ntitle: y\ncategory: api\nlast_reviewed: yesterday\nstatus: published\n---\nbody",
        )
        assertTrue(result.issues.any { "ISO date" in it.message })
    }

    @Test
    fun `issues print as path line severity message`() {
        val issue = DocumentIssue("docs/a.md", 7, "boom", DocumentIssue.Severity.ERROR)
        assertEquals("docs/a.md:7: error: boom", issue.toString())
    }
}
