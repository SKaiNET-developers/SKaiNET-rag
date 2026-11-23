package sk.ainet.rag.core

import sk.ainet.rag.core.bundle.BundleCodec
import sk.ainet.rag.core.bundle.BundleFormatException
import sk.ainet.rag.core.bundle.Cluster
import sk.ainet.rag.core.bundle.EmbeddingInfo
import sk.ainet.rag.core.bundle.StoredChunk
import sk.ainet.rag.core.text.Tokenizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BundleCodecTest {

    private val embedding = EmbeddingInfo(
        modelId = "skainet-hrp-v1",
        dimensions = 4,
        normalized = true,
        queryPrefix = "",
        documentPrefix = "",
    )

    private fun vectors(): FloatArray = FloatArray(Fixture.chunks.size * 4) { (it % 7) / 7f }

    @Test
    fun `round trips a full bundle`() {
        val clusters = listOf(Cluster("c1", listOf("how to close"), "See the help.", listOf()))
        val bytes = BundleCodec.encode(
            chunks = Fixture.chunks,
            vectors = vectors(),
            clusters = clusters,
            contentVersion = 7,
            builtAt = "2026-10-09T00:00:00Z",
            embedding = embedding,
        )
        val bundle = BundleCodec.decode(bytes)

        assertEquals(7, bundle.manifest.contentVersion)
        assertEquals(Fixture.chunks.size, bundle.manifest.chunkCount)
        assertEquals(embedding, bundle.manifest.embedding)
        assertEquals(Fixture.chunks.map(StoredChunk::from), bundle.chunks)
        assertEquals(clusters, bundle.clusters)
        assertNotNull(bundle.vectors)
        assertTrue(vectors().contentEquals(bundle.vectors!!))

        // The decoded BM25 index answers like one built directly from the chunks.
        val terms = Tokenizer.tokenize("which jep introduced virtual threads")
        assertEquals(
            sk.ainet.rag.core.index.Bm25Index.build(Fixture.chunks).search(terms, 4),
            bundle.bm25.search(terms, 4),
        )
    }

    @Test
    fun `bm25 only bundle has no embedding block and no vectors`() {
        val bytes = BundleCodec.encode(
            chunks = Fixture.chunks,
            vectors = null,
            clusters = emptyList(),
            contentVersion = 1,
            builtAt = "2026-10-09T00:00:00Z",
            embedding = null,
        )
        val bundle = BundleCodec.decode(bytes)
        assertNull(bundle.manifest.embedding)
        assertNull(bundle.vectors)
        // The manifest JSON must omit the block entirely rather than write null.
        assertTrue("embedding" !in bytes.decodeToString(8, 600))
    }

    @Test
    fun `identical content encodes to identical bytes`() {
        fun encode() = BundleCodec.encode(
            chunks = Fixture.chunks,
            vectors = vectors(),
            clusters = emptyList(),
            contentVersion = 3,
            builtAt = "2026-10-09T00:00:00Z",
            embedding = embedding,
        )
        assertTrue(encode().contentEquals(encode()))
    }

    @Test
    fun `url metadata round trips and absent fields write no json`() {
        val linked = Fixture.chunks.first().copy(urlPath = "pages/x.html", anchor = "why")
        val bytes = BundleCodec.encode(
            chunks = listOf(linked),
            vectors = null,
            clusters = emptyList(),
            contentVersion = 1,
            builtAt = "now",
            embedding = null,
        )
        val decoded = BundleCodec.decode(bytes).chunks.single()
        assertEquals("pages/x.html", decoded.urlPath)
        assertEquals("why", decoded.anchor)

        // A chunk without the metadata serializes to exactly the pre-URL bytes.
        val bare = BundleCodec.encode(
            chunks = listOf(Fixture.chunks.first()),
            vectors = null,
            clusters = emptyList(),
            contentVersion = 1,
            builtAt = "now",
            embedding = null,
        )
        assertTrue("urlPath" !in bare.decodeToString())
    }

    @Test
    fun `site metadata round trips and absent fields write no json`() {
        val sited = Fixture.chunks.first().copy(
            component = "skainet",
            componentVersion = "0.57",
            headingPath = listOf("Install", "Prerequisites"),
        )
        val decoded = BundleCodec.decode(
            BundleCodec.encode(
                chunks = listOf(sited),
                vectors = null,
                clusters = emptyList(),
                contentVersion = 1,
                builtAt = "now",
                embedding = null,
            ),
        ).chunks.single()
        assertEquals("skainet", decoded.component)
        assertEquals("0.57", decoded.componentVersion)
        assertEquals(listOf("Install", "Prerequisites"), decoded.headingPath)

        // A chunk without site metadata serializes to exactly the pre-site bytes.
        val bare = BundleCodec.encode(
            chunks = listOf(Fixture.chunks.first()),
            vectors = null,
            clusters = emptyList(),
            contentVersion = 1,
            builtAt = "now",
            embedding = null,
        ).decodeToString()
        assertTrue("component" !in bare)
        assertTrue("headingPath" !in bare)
    }

    @Test
    fun `refuses vectors without an embedding block`() {
        assertFailsWith<IllegalArgumentException> {
            BundleCodec.encode(
                chunks = Fixture.chunks,
                vectors = vectors(),
                clusters = emptyList(),
                contentVersion = 1,
                builtAt = "now",
                embedding = null,
            )
        }
    }

    @Test
    fun `refuses a corrupted magic`() {
        val bytes = BundleCodec.encode(
            chunks = Fixture.chunks,
            vectors = null,
            clusters = emptyList(),
            contentVersion = 1,
            builtAt = "now",
            embedding = null,
        )
        bytes[0] = 'X'.code.toByte()
        assertFailsWith<BundleFormatException> { BundleCodec.decode(bytes) }
    }

    @Test
    fun `refuses truncated vector data`() {
        val bytes = BundleCodec.encode(
            chunks = Fixture.chunks,
            vectors = vectors(),
            clusters = emptyList(),
            contentVersion = 1,
            builtAt = "now",
            embedding = embedding.copy(dimensions = 4),
        )
        // Corrupt the declared chunk count by re-encoding with a mismatched embedding block.
        assertFailsWith<IllegalArgumentException> {
            BundleCodec.encode(
                chunks = Fixture.chunks,
                vectors = FloatArray(3),
                clusters = emptyList(),
                contentVersion = 1,
                builtAt = "now",
                embedding = embedding,
            )
        }
        // And decoding intact bytes still works after the failed encode.
        assertNotNull(BundleCodec.decode(bytes).vectors)
    }
}
