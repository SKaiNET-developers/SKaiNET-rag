package sk.ainet.rag.embedder.leaf

import sk.ainet.rag.core.bundle.EmbeddingInfo
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LeafQueryEmbedderTest {

    private val compatible = EmbeddingInfo(
        modelId = LeafQueryEmbedder.MODEL_ID,
        dimensions = LeafQueryEmbedder.RUNTIME.dimensions,
        normalized = true,
        queryPrefix = "",
        documentPrefix = "",
    )

    @Test
    fun runtimeDescriptorIsTheCartridgeDescriptor() {
        assertEquals("mdbr-leaf-ir-v1", LeafQueryEmbedder.MODEL_ID)
        assertEquals(768, LeafQueryEmbedder.RUNTIME.dimensions)
        assertTrue(LeafQueryEmbedder.RUNTIME.normalized)
        assertEquals("", LeafQueryEmbedder.RUNTIME.queryPrefix)
        assertEquals("", LeafQueryEmbedder.RUNTIME.documentPrefix)
    }

    /** Parity runs before any weight load, so this needs no snapshot on disk. */
    @Test
    fun foreignSpaceIsRefusedBeforeTouchingTheModel() = runTest {
        var reason: String? = null
        val embedder = LeafQueryEmbedder.Provider.create(
            compatible.copy(dimensions = 256),
        ) { reason = it }
        assertNull(embedder, "a 256-d manifest must not open a 768-d engine")
        assertTrue(reason.orEmpty().contains("dimensions"), "got: $reason")
    }

    @Test
    fun embedsQueriesInTheCartridgeSpace() = runTest {
        var reason: String? = null
        val embedder = LeafQueryEmbedder.Provider.create(compatible) { reason = it }
        if (embedder == null) {
            println("SKIPPED embedsQueriesInTheCartridgeSpace: $reason")
            return@runTest
        }
        try {
            assertEquals(768, embedder.dimensions)
            val vector = embedder.embed("which JEP introduced virtual threads")
                ?: error("embed returned null with the snapshot present")
            assertEquals(768, vector.size)
            val norm = sqrt(vector.fold(0.0) { acc, v -> acc + v * v })
            assertTrue(abs(norm - 1.0) < 1e-3, "unit norm expected, got $norm")
            val again = embedder.embed("which JEP introduced virtual threads")
                ?: error("second embed returned null")
            assertContentEquals(vector, again, "LEAF is deterministic")
        } finally {
            embedder.close()
        }
    }
}
