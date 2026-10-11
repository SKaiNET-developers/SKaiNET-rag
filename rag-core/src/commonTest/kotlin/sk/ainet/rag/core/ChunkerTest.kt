package sk.ainet.rag.core

import sk.ainet.rag.core.document.Chunker
import sk.ainet.rag.core.document.ChunkerConfig
import sk.ainet.rag.core.document.DocumentIssue
import sk.ainet.rag.core.document.DocumentStatus
import sk.ainet.rag.core.document.ParsedDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChunkerTest {

    private fun document(body: String) = ParsedDocument(
        id = "java-21-api",
        title = "Java 21 API changes",
        category = "api",
        lastReviewed = "2026-10-10",
        status = DocumentStatus.PUBLISHED,
        body = body,
        sourcePath = "java-21-api.md",
    )

    @Test
    fun `only level-two headings cut chunks`() {
        val chunks = Chunker().chunk(
            document(
                """
                ## Virtual Threads
                Lightweight threads arrive.

                ### Scheduling details
                Still the same section.

                ## Sequenced Collections
                A uniform first and last.
                """.trimIndent(),
            ),
        ).chunks

        assertEquals(listOf("Virtual Threads", "Sequenced Collections"), chunks.map { it.heading })
        assertTrue("Scheduling details" in chunks[0].displayText)
    }

    @Test
    fun `a preamble becomes the intro chunk`() {
        val chunks = Chunker().chunk(
            document("What shipped in this release.\n\n## Virtual Threads\nLightweight threads."),
        ).chunks

        assertNull(chunks.first().heading)
        assertEquals("java-21-api#intro", chunks.first().chunkId)
    }

    @Test
    fun `the embedded view gains context the display view keeps markdown`() {
        val chunk = Chunker().chunk(document("## Virtual Threads\nUse **Thread.ofVirtual**.")).chunks.single()

        assertEquals("Java 21 API changes — Virtual Threads\n\nUse Thread.ofVirtual.", chunk.embeddedText)
        assertEquals("Use **Thread.ofVirtual**.", chunk.displayText)
    }

    @Test
    fun `slugs are deterministic and url-shaped`() {
        val chunk = Chunker().chunk(document("## Why virtual threads?\nScalability.")).chunks.single()
        assertEquals("java-21-api#why-virtual-threads", chunk.chunkId)
    }

    @Test
    fun `a section over budget warns and points at itself`() {
        val body = "## Growing section\n" + "word ".repeat(30)
        val issue = Chunker(ChunkerConfig(maxChunkTokens = 20)).chunk(document(body)).issues.single()
        assertEquals(DocumentIssue.Severity.WARNING, issue.severity)
        assertTrue("Growing section" in issue.message, issue.message)
        assertEquals("java-21-api.md", issue.sourcePath)
    }

    @Test
    fun `double the budget is an error and still never auto-splits`() {
        val body = "## Runaway section\n" + "word ".repeat(60)
        val result = Chunker(ChunkerConfig(maxChunkTokens = 20)).chunk(document(body))
        assertEquals(DocumentIssue.Severity.ERROR, result.issues.single().severity)
        assertEquals(1, result.chunks.size)
    }

    @Test
    fun `a heading with no body produces no chunk`() {
        val chunks = Chunker().chunk(document("## Placeholder\n\n## Shipped\nContent.")).chunks
        assertEquals(listOf("Shipped"), chunks.map { it.heading })
    }

    @Test
    fun `the chunker version is part of the bundle contract`() {
        assertEquals(3, Chunker.VERSION)
    }
}
