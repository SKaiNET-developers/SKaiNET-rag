package sk.ainet.rag.core

import sk.ainet.rag.core.index.Bm25Index
import sk.ainet.rag.core.index.ReciprocalRankFusion
import sk.ainet.rag.core.index.ScoredChunk
import sk.ainet.rag.core.index.VectorIndex
import sk.ainet.rag.core.index.VersionFilter
import sk.ainet.rag.core.text.Tokenizer
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RetrievalTest {

    private val index = Bm25Index.build(Fixture.chunks)

    private fun topDocs(query: String, n: Int = 3): List<String> =
        index.search(Tokenizer.tokenize(query), n).map { Fixture.chunks[it.chunkIndex].docId }

    @Test
    fun `a jep number retrieves through the alias field`() {
        // "JEP 444" (virtual threads) occurs in java-21-api's aliases, and alias hits carry the
        // highest field weight, so the bare number must be enough.
        assertEquals("java-21-api", topDocs("jep 444").first())
    }

    @Test
    fun `a feature question lands on the release that shipped it`() {
        assertEquals("java-21-api", topDocs("which jep introduced virtual threads").first())
        assertEquals("java-17-language", topDocs("sealed classes").first())
        assertEquals("java-8-jvm", topDocs("permanent generation removed").first())
    }

    @Test
    fun `the draft document is invisible to retrieval`() {
        // Its marker vocabulary exists nowhere else; see TestCorpus.draftDocument.
        assertTrue(Fixture.chunks.none { it.docId == "java-next-preview" })
        assertTrue(index.search(Tokenizer.tokenize("xylophone compaction"), 5).isEmpty())
    }

    @Test
    fun `a vocabulary with no match returns nothing`() {
        assertTrue(index.search(Tokenizer.tokenize("qqqq zzzz"), 5).isEmpty())
    }

    @Test
    fun `ranking is deterministic and score-ordered`() {
        val once = index.search(Tokenizer.tokenize("garbage collector"), 5)
        val twice = index.search(Tokenizer.tokenize("garbage collector"), 5)
        assertEquals(once, twice)
        assertEquals(once.map { it.chunkIndex }, once.sortedByDescending { it.score }.map { it.chunkIndex })
    }

    @Test
    fun `a snapshot round trip reproduces every score`() {
        // This equivalence is the build/runtime parity story in miniature.
        val reloaded = Bm25Index.from(index.snapshot())
        for (query in listOf("virtual threads", "class file version", "flight recorder", "streams")) {
            val terms = Tokenizer.tokenize(query)
            val original = index.search(terms, 5)
            val restored = reloaded.search(terms, 5)
            assertEquals(original.map { it.chunkIndex }, restored.map { it.chunkIndex }, query)
            original.zip(restored).forEach { (a, b) ->
                assertTrue(abs(a.score - b.score) < 1e-6f, "score drift on $query")
            }
        }
    }

    @Test
    fun `vector search orders by cosine and drops the orthogonal`() {
        val vectors = floatArrayOf(
            1f, 0f,
            0f, 1f,
            0.7071f, 0.7071f,
        )
        val hits = VectorIndex(vectors, dimensions = 2).search(floatArrayOf(1f, 0f), topK = 3)
        // The orthogonal vector is absent, not merely last: no match is not a weak match.
        assertEquals(listOf(0, 2), hits.map { it.chunkIndex })
    }

    @Test
    fun `the similarity floor keeps noise out of the ranking`() {
        // Otherwise every chunk holds a vector rank and fusion can promote sheer existence.
        val vectors = floatArrayOf(1f, 0f, 0f, 1f, 0f, 1f, 0f, 1f)
        val hits = VectorIndex(vectors, dimensions = 2).search(floatArrayOf(1f, 0f), topK = 8)
        assertEquals(listOf(0), hits.map { it.chunkIndex })
    }

    @Test
    fun `normalize turns a dot product into a cosine`() {
        val unit = VectorIndex.normalize(floatArrayOf(3f, 4f))
        assertTrue(abs(unit[0] - 0.6f) < 1e-6f)
        assertTrue(abs(unit[1] - 0.8f) < 1e-6f)
        assertEquals(listOf(0f, 0f), VectorIndex.normalize(floatArrayOf(0f, 0f)).toList())
    }

    @Test
    fun `fusion prefers the chunk both rankings agree on`() {
        val lexical = listOf(ScoredChunk(1, 9f), ScoredChunk(2, 8f), ScoredChunk(3, 7f))
        val semantic = listOf(ScoredChunk(3, 0.9f), ScoredChunk(1, 0.8f), ScoredChunk(9, 0.7f))
        val fused = ReciprocalRankFusion.fuse(lexical, semantic).map { it.chunkIndex }
        // Chunk 1 places top-two in both lists; nothing else does.
        assertEquals(1, fused.first())
        assertTrue(fused.indexOf(3) < fused.indexOf(2))
        assertTrue(9 in fused)
    }

    @Test
    fun `the version window admits and rejects correctly`() {
        assertTrue(VersionFilter.matches("4.2", minimum = "4.2", maximum = null))
        assertTrue(VersionFilter.matches("4.10.1", minimum = "4.2", maximum = null))
        assertFalse(VersionFilter.matches("4.1", minimum = "4.2", maximum = null))
        assertFalse(VersionFilter.matches("5.0", minimum = null, maximum = "4.9"))
        assertTrue(VersionFilter.matches("4.5", minimum = null, maximum = null))
    }

    @Test
    fun `an unparseable app version hides nothing`() {
        assertTrue(VersionFilter.matches("nightly", minimum = "4.2", maximum = null))
    }
}
