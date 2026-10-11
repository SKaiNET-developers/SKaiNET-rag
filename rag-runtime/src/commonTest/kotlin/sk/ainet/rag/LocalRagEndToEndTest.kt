package sk.ainet.rag

import sk.ainet.rag.core.agent.LocalRagState
import sk.ainet.rag.core.agent.MatchSource
import sk.ainet.rag.core.bundle.BundleCodec
import sk.ainet.rag.core.bundle.EmbeddingInfo
import sk.ainet.rag.core.document.Chunk
import sk.ainet.rag.core.document.Chunker
import sk.ainet.rag.core.document.FrontMatterParser
import sk.ainet.rag.embedder.skainet.SkaiNetHashedProjectionEmbedder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The whole loop on every target: docs chunked, embedded through SKaiNET, packed, loaded back
 * through the facade, and retrieved hybrid — the build-time and runtime halves are the same
 * compiled code, which is the claim this skeleton exists to make.
 */
class LocalRagEndToEndTest {

    private val docs = mapOf(
        "deposit.md" to """
            |---
            |id: deposit-funds
            |title: Deposit funds
            |category: funds
            |aliases: [add money, top up]
            |last_reviewed: 2026-10-01
            |status: published
            |---
            |## How to deposit
            |Open the funds tab and choose a payment method. Bank transfers settle within one
            |business day and card payments are instant.
        """.trimMargin(),
        "withdraw.md" to """
            |---
            |id: withdraw-funds
            |title: Withdraw funds
            |category: funds
            |aliases: [payout, cash out]
            |last_reviewed: 2026-10-01
            |status: published
            |---
            |## Withdrawal timing
            |Withdrawals are processed on business days and arrive within two business days on
            |your verified bank account.
        """.trimMargin(),
    )

    private fun chunks(): List<Chunk> {
        val chunker = Chunker()
        return docs.entries.sortedBy { it.key }.flatMap { (name, content) ->
            val parsed = FrontMatterParser.parse(name, content)
            check(parsed.issues.isEmpty()) { parsed.issues.toString() }
            chunker.chunk(requireNotNull(parsed.document)).chunks
        }
    }

    private fun bundleBytes(embedding: EmbeddingInfo?): ByteArray {
        val chunks = chunks()
        val vectors = embedding?.let { info ->
            SkaiNetHashedProjectionEmbedder(info.dimensions).use { embedder ->
                val all = FloatArray(chunks.size * info.dimensions)
                chunks.forEachIndexed { index, chunk ->
                    embedder.embed(chunk.embeddedText).copyInto(all, index * info.dimensions)
                }
                all
            }
        }
        return BundleCodec.encode(
            chunks = chunks,
            vectors = vectors,
            clusters = emptyList(),
            contentVersion = 1,
            builtAt = "2026-10-09T00:00:00Z",
            embedding = embedding,
        )
    }

    // The adapter's intrinsic descriptor declares DEFAULT_DIMENSIONS; anything else is a
    // different vector space and the parity check refuses it (see the mismatch test below).
    private val hrp = EmbeddingInfo(
        modelId = SkaiNetQueryEmbedder.MODEL_ID,
        dimensions = SkaiNetHashedProjectionEmbedder.DEFAULT_DIMENSIONS,
        normalized = true,
        queryPrefix = "",
        documentPrefix = "",
    )

    @Test
    fun `retrieves hybrid from a bundle embedded by the same code`() = runTest {
        val bytes = bundleBytes(hrp)
        val rag = LocalRag(LocalRag.Config(loadBundle = { bytes }, topK = 2))
        rag.initialize()

        val state = rag.state.value
        assertIs<LocalRagState.Ready>(state)
        assertTrue(state.usingVectors, "parity-clean bundle must enable vectors")

        val passages = rag.retrieveOnly("when do withdrawals arrive on my verified bank account")
        assertTrue(passages.isNotEmpty())
        assertEquals("withdraw-funds", passages.first().docId)
        assertTrue(
            passages.first().source == MatchSource.HYBRID ||
                passages.first().source == MatchSource.BM25 ||
                passages.first().source == MatchSource.VECTOR,
        )
        rag.release()
    }

    @Test
    fun `mismatched embedding space drops vectors and stays usable`() = runTest {
        val foreign = hrp.copy(modelId = "embeddinggemma-300m-seq256")
        val bytes = bundleBytes(foreign)
        val rag = LocalRag(LocalRag.Config(loadBundle = { bytes }))
        rag.initialize()

        val state = rag.state.value
        assertIs<LocalRagState.Ready>(state)
        assertTrue(!state.usingVectors, "foreign vector space must be refused")

        val passages = rag.retrieveOnly("deposit money by card")
        assertEquals("deposit-funds", passages.firstOrNull()?.docId)
        assertEquals(MatchSource.BM25, passages.first().source)
        rag.release()
    }

    @Test
    fun `bm25 only bundle is a supported state`() = runTest {
        val bytes = bundleBytes(null)
        val rag = LocalRag(LocalRag.Config(loadBundle = { bytes }))
        rag.initialize()
        val state = rag.state.value
        assertIs<LocalRagState.Ready>(state)
        assertTrue(!state.usingVectors)
        assertTrue(rag.retrieveOnly("payout timing").isNotEmpty())
        rag.release()
    }
}
