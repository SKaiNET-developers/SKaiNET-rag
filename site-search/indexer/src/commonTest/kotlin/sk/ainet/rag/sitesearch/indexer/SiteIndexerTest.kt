package sk.ainet.rag.sitesearch.indexer

import sk.ainet.rag.core.bundle.BundleCodec
import sk.ainet.rag.core.text.Tokenizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SiteIndexerTest {

    // The same fixture drives the JVM and the wasm run of this suite; the pinned digest below
    // is therefore a cross-target determinism canary, in the spirit of the repo's CI byte gate.
    private val pagesJson = """
        [
          {
            "docId": "skainet:ROOT:install.adoc",
            "title": "Install SKaiNET",
            "urlPath": "/skainet/0.57/install.html",
            "component": "skainet",
            "version": "0.57",
            "module": "ROOT",
            "sections": [
              {"text": "SKaiNET installs from Maven Central as a Kotlin Multiplatform BOM."},
              {"anchor": "_prerequisites", "headingPath": ["Prerequisites"],
               "text": "You need a JDK 21 toolchain and Gradle. iOS targets additionally need Xcode.",
               "codeText": "implementation(platform(\"sk.ainet:skainet-bom\"))"},
              {"anchor": "_verify", "headingPath": ["Verify the install"],
               "text": "Run the smoke test to confirm tensors compute on your machine."}
            ]
          },
          {
            "docId": "transformers:ROOT:bert.adoc",
            "title": "BERT embeddings",
            "urlPath": "/skainet-transformers/1.0/bert.html",
            "component": "skainet-transformers",
            "version": "1.0",
            "module": "ROOT",
            "sections": [
              {"anchor": "_pooling", "headingPath": ["Pooling"],
               "text": "Mean pooling averages token vectors; CLS pooling takes the first token."}
            ]
          }
        ]
    """.trimIndent()

    private val configJson = """
        {"contentVersion": 3, "builtAt": "2026-11-23T00:00:00Z", "dimensions": 32}
    """.trimIndent()

    @Test
    fun `pages become a loadable hybrid bundle with site metadata intact`() {
        val result = SiteIndexJson.index(pagesJson, configJson)
        assertEquals(4, result.chunkCount)
        assertEquals(2, result.pageCount)
        assertTrue(result.warnings.isEmpty())

        val bundle = BundleCodec.decode(result.bundle)
        assertEquals(3, bundle.manifest.contentVersion)
        assertEquals("skainet-hrp-v1", bundle.manifest.embedding?.modelId)
        assertEquals(4 * 32, bundle.vectors?.size)

        val chunk = bundle.chunks.first { it.anchor == "_prerequisites" }
        assertEquals("/skainet/0.57/install.html", chunk.urlPath)
        assertEquals("skainet", chunk.component)
        assertEquals("0.57", chunk.componentVersion)
        assertEquals(listOf("Prerequisites"), chunk.headingPath)
        assertEquals("skainet", chunk.category)

        // The decoded BM25 index answers, and the facet is the component.
        val hits = bundle.bm25.search(Tokenizer.tokenize("mean pooling token vectors"), 2)
        assertTrue(hits.isNotEmpty())
        assertEquals("transformers:ROOT:bert.adoc#_pooling", bundle.chunks[hits.first().chunkIndex].chunkId)
    }

    @Test
    fun `identical input produces identical bytes with a pinned digest`() {
        val first = SiteIndexJson.index(pagesJson, configJson).bundle
        val second = SiteIndexJson.index(pagesJson, configJson).bundle
        assertTrue(first.contentEquals(second))
        // Pinned across targets: the JVM and the wasm test run must agree on every byte,
        // embeddings included. A legitimate chunking or format change re-pins this constant.
        assertEquals("bb22f80a4da1d7c3", fnv1a64(first))
    }

    @Test
    fun `embed false yields a bm25 only bundle`() {
        val result = SiteIndexJson.index(
            pagesJson,
            """{"contentVersion": 1, "builtAt": "2026-11-23T00:00:00Z", "embed": false}""",
        )
        val bundle = BundleCodec.decode(result.bundle)
        assertNull(bundle.manifest.embedding)
        assertNull(bundle.vectors)
        assertNotNull(bundle.bm25)
    }

    @Test
    fun `an empty site refuses to produce an empty index`() {
        val failure = runCatching { SiteIndexJson.index("[]", configJson) }
        assertTrue(failure.isFailure)
    }

    private fun fnv1a64(bytes: ByteArray): String {
        var hash = -3750763034362895579L // FNV offset basis, 0xcbf29ce484222325
        for (byte in bytes) {
            hash = hash xor (byte.toLong() and 0xff)
            hash *= 1099511628211L // FNV prime
        }
        return hash.toULong().toString(16).padStart(16, '0')
    }
}
