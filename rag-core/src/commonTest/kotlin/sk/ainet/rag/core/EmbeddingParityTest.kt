package sk.ainet.rag.core

import sk.ainet.rag.core.bundle.EmbedderDescriptor
import sk.ainet.rag.core.bundle.EmbeddingInfo
import sk.ainet.rag.core.bundle.EmbeddingParity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EmbeddingParityTest {

    private val manifest = EmbeddingInfo(
        modelId = "mdbr-leaf-ir-v1",
        dimensions = 768,
        normalized = true,
        queryPrefix = "",
        documentPrefix = "",
    )

    private val runtime = EmbedderDescriptor(
        modelId = manifest.modelId,
        dimensions = manifest.dimensions,
        normalized = manifest.normalized,
        queryPrefix = manifest.queryPrefix,
        documentPrefix = manifest.documentPrefix,
    )

    @Test
    fun `agreement on every field is compatible`() {
        assertEquals(EmbeddingParity.Result.Compatible, EmbeddingParity.check(manifest, runtime))
    }

    @Test
    fun `dimensions must agree`() {
        val result = assertIs<EmbeddingParity.Result.Incompatible>(
            EmbeddingParity.check(manifest, runtime.copy(dimensions = 256)),
        )
        assertTrue("dimensions: bundle 768, runtime 256" in result.reasons.single())
    }

    @Test
    fun `query prefixes must agree`() {
        // The signature failure this guard exists for: a prefix drift raises nothing at query
        // time, it just embeds into a slightly different space forever.
        val result = assertIs<EmbeddingParity.Result.Incompatible>(
            EmbeddingParity.check(manifest, runtime.copy(queryPrefix = "query: ")),
        )
        assertTrue("query prefix" in result.reasons.single())
    }

    @Test
    fun `document prefixes must agree`() {
        assertIs<EmbeddingParity.Result.Incompatible>(
            EmbeddingParity.check(manifest, runtime.copy(documentPrefix = "passage: ")),
        )
    }

    @Test
    fun `model ids must agree`() {
        assertIs<EmbeddingParity.Result.Incompatible>(
            EmbeddingParity.check(manifest, runtime.copy(modelId = "skainet-hrp-v1")),
        )
    }

    @Test
    fun `normalization must agree`() {
        assertIs<EmbeddingParity.Result.Incompatible>(
            EmbeddingParity.check(manifest, runtime.copy(normalized = false)),
        )
    }

    @Test
    fun `all mismatches are listed at once`() {
        val result = assertIs<EmbeddingParity.Result.Incompatible>(
            EmbeddingParity.check(
                manifest,
                runtime.copy(dimensions = 256, queryPrefix = "q: ", modelId = "other"),
            ),
        )
        assertEquals(3, result.reasons.size, result.reasons.toString())
    }
}
