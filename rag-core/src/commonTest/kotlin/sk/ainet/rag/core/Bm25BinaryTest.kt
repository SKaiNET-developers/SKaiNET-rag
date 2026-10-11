package sk.ainet.rag.core

import sk.ainet.rag.core.index.Bm25Binary
import sk.ainet.rag.core.index.Bm25Index
import sk.ainet.rag.core.text.Tokenizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Bm25BinaryTest {

    private val index = Bm25Index.build(Fixture.chunks)

    @Test
    fun `snapshot survives the binary round trip exactly`() {
        val snapshot = index.snapshot()
        val decoded = Bm25Binary.decode(Bm25Binary.encode(snapshot))

        assertEquals(snapshot.k1, decoded.k1)
        assertEquals(snapshot.b, decoded.b)
        assertEquals(snapshot.averageChunkLength, decoded.averageChunkLength)
        assertEquals(snapshot.chunkLengths, decoded.chunkLengths)
        assertEquals(snapshot.postings.keys, decoded.postings.keys)
        for ((term, postings) in snapshot.postings) {
            assertEquals(
                postings.sortedBy { it.chunkIndex },
                decoded.postings.getValue(term),
                "postings for `$term`",
            )
        }
    }

    @Test
    fun `decoded index ranks identically to the built one`() {
        val decoded = Bm25Index.from(Bm25Binary.decode(Bm25Binary.encode(index.snapshot())))
        val queries = listOf(
            "which jep introduced virtual threads",
            "class file version of java 17",
            "flight recorder",
            "overnight charges for gold",
        )
        for (query in queries) {
            val terms = Tokenizer.tokenize(query)
            assertEquals(index.search(terms, 8), decoded.search(terms, 8), "ranking for `$query`")
        }
    }

    @Test
    fun `binary form is much smaller than json spelled postings`() {
        val binary = Bm25Binary.encode(index.snapshot()).size
        // The JSON baseline is approximated by the string form kotlinx would emit; the point of
        // the assertion is only that the binary layout stays a clear win, not an exact ratio.
        val jsonApprox = index.snapshot().postings.entries.sumOf { (term, postings) ->
            term.length + 20 + postings.sumOf { 45 }
        }
        assertTrue(binary < jsonApprox / 2, "binary $binary bytes vs ~json $jsonApprox")
    }
}
