package sk.ainet.rag.core

import sk.ainet.rag.core.index.Bm25Index
import sk.ainet.rag.core.text.Tokenizer
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Retrieval quality as a build gate. The scoring path here is byte-for-byte the one a device
 * runs, so a tokenizer or index regression fails CI instead of surfacing as "search got worse"
 * in the field. The printed table is the part a human reads; the asserted floors only exist so
 * the build notices a collapse.
 */
class EvalHarnessTest {

    @Test
    fun `hit rates stay above the floor`() {
        val index = Bm25Index.build(Fixture.chunks)
        val docOfChunk = Fixture.chunks.map { it.docId }

        var atOne = 0
        var atFour = 0
        val missed = mutableListOf<String>()

        for (case in EvalQueries.cases) {
            val rankedDocs = index.search(Tokenizer.tokenize(case.query), 8)
                .map { docOfChunk[it.chunkIndex] }
                .distinct()
            if (rankedDocs.firstOrNull() == case.expectedDocId) atOne++
            if (case.expectedDocId in rankedDocs.take(4)) {
                atFour++
            } else {
                missed += "${case.query} -> wanted ${case.expectedDocId}, got ${rankedDocs.take(4)}"
            }
        }

        val total = EvalQueries.cases.size
        println("eval: $total queries, ${Fixture.published.size} documents, ${Fixture.chunks.size} chunks")
        println("hit-rate@1: $atOne/$total (${atOne * 1000 / total / 10.0}%)")
        println("hit-rate@4: $atFour/$total (${atFour * 1000 / total / 10.0}%)")
        missed.forEach { println("  miss@4: $it") }

        assertTrue(atOne * 100 / total >= 70, "hit-rate@1 collapsed to ${atOne * 100 / total}%")
        assertTrue(atFour * 100 / total >= 85, "hit-rate@4 collapsed to ${atFour * 100 / total}%")
    }

    @Test
    fun `every expectation points at a real published document`() {
        val published = Fixture.published.mapTo(mutableSetOf()) { it.id }
        val dangling = EvalQueries.cases.map { it.expectedDocId }.distinct().filter { it !in published }
        assertTrue(dangling.isEmpty(), "eval set names documents that do not exist: $dangling")
    }
}
