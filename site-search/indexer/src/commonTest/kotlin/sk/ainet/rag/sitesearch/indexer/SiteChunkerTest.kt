package sk.ainet.rag.sitesearch.indexer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SiteChunkerTest {

    private val config = SiteIndexConfig(contentVersion = 1, builtAt = "2026-11-23T00:00:00Z")

    private fun page(sections: List<SectionRecord>) = PageRecord(
        docId = "skainet:ROOT:install.adoc",
        title = "Install SKaiNET",
        urlPath = "/skainet/0.57/install.html",
        component = "skainet",
        version = "0.57",
        module = "ROOT",
        sections = sections,
    )

    @Test
    fun `a section becomes one chunk carrying page and heading identity`() {
        val chunk = SiteChunker(config).chunk(
            page(
                listOf(
                    SectionRecord(
                        anchor = "_prerequisites",
                        headingPath = listOf("Prerequisites"),
                        text = "You need a JDK.",
                    ),
                ),
            ),
        ).chunks.single()

        assertEquals("skainet:ROOT:install.adoc#_prerequisites", chunk.chunkId)
        assertEquals("/skainet/0.57/install.html", chunk.urlPath)
        assertEquals("_prerequisites", chunk.anchor)
        assertEquals("Prerequisites", chunk.heading)
        assertEquals(listOf("Prerequisites"), chunk.headingPath)
        assertEquals("skainet", chunk.component)
        assertEquals("skainet", chunk.category)
        assertEquals("0.57", chunk.componentVersion)
        assertEquals("Install SKaiNET — Prerequisites\n\nYou need a JDK.", chunk.embeddedText)
    }

    @Test
    fun `the preamble has no anchor and nested headings read as a trail`() {
        val chunks = SiteChunker(config).chunk(
            page(
                listOf(
                    SectionRecord(text = "SKaiNET installs from Maven Central."),
                    SectionRecord(
                        anchor = "_linux",
                        headingPath = listOf("Install", "Linux"),
                        text = "Use the tarball.",
                    ),
                ),
            ),
        ).chunks

        val preamble = chunks.first()
        assertNull(preamble.anchor)
        assertNull(preamble.heading)
        assertNull(preamble.headingPath)
        assertTrue(preamble.chunkId.endsWith("#intro"))

        val nested = chunks.last()
        assertEquals("Linux", nested.heading)
        assertTrue(nested.embeddedText.startsWith("Install SKaiNET — Install › Linux"))
    }

    @Test
    fun `code stays searchable but leaves the embedding when it dominates`() {
        val prose = "Run the generator."
        val code = "./gradlew :samples:corpus-java:run --args=\"--data x --out y\" # plus enough " +
            "characters to clearly dominate the prose of this section record"
        val chunk = SiteChunker(config).chunk(
            page(listOf(SectionRecord(anchor = "_run", headingPath = listOf("Run"), text = prose, codeText = code))),
        ).chunks.single()

        // Display text (what BM25 indexes) keeps the identifiers; the embedded view drops them.
        assertTrue("gradlew" in chunk.displayText)
        assertTrue("gradlew" !in chunk.embeddedText)
        assertTrue(prose in chunk.embeddedText)
    }

    @Test
    fun `balanced code rides along into the embedding`() {
        val chunk = SiteChunker(config).chunk(
            page(
                listOf(
                    SectionRecord(
                        anchor = "_api",
                        headingPath = listOf("API"),
                        text = "Call the facade with a configuration object and await the result.",
                        codeText = "rag.retrieveOnly(query)",
                    ),
                ),
            ),
        ).chunks.single()
        assertTrue("retrieveOnly" in chunk.embeddedText)
    }

    @Test
    fun `module can be the facet instead of component`() {
        val chunk = SiteChunker(config.copy(categoryFrom = SiteIndexConfig.CategoryFrom.MODULE)).chunk(
            page(listOf(SectionRecord(anchor = "_a", headingPath = listOf("A"), text = "text"))),
        ).chunks.single()
        assertEquals("ROOT", chunk.category)
    }

    @Test
    fun `an oversize section warns and splits at paragraph boundaries`() {
        val paragraph = (1..60).joinToString(" ") { "word$it" }
        val huge = (1..12).joinToString("\n\n") { paragraph }
        val result = SiteChunker(config.copy(maxChunkTokens = 200)).chunk(
            page(listOf(SectionRecord(anchor = "_big", headingPath = listOf("Big"), text = huge))),
        )

        assertTrue(result.chunks.size > 1, "expected a split, got ${result.chunks.size} chunk(s)")
        assertTrue(result.warnings.single().contains("past the 200-token budget"))
        // Every part keeps the section's anchor; ids stay unique; nothing is lost.
        assertTrue(result.chunks.all { it.anchor == "_big" })
        assertEquals(result.chunks.size, result.chunks.map { it.chunkId }.toSet().size)
        assertEquals(huge, result.chunks.joinToString("\n\n") { it.displayText })
        assertTrue(result.chunks.all { it.tokenCount <= 200 })
    }

    @Test
    fun `empty sections vanish instead of becoming blank chunks`() {
        assertTrue(
            SiteChunker(config).chunk(page(listOf(SectionRecord(anchor = "_e", headingPath = listOf("E"))))).chunks.isEmpty(),
        )
    }
}
