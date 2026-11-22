package sk.ainet.rag.bundler

import kotlinx.coroutines.runBlocking
import sk.ainet.cartridge.embedding.leaf.LeafEmbeddingCartridge
import sk.ainet.rag.core.bundle.EmbeddingInfo
import sk.ainet.rag.core.document.Chunk

/**
 * The mdbr-leaf-ir neural encoder, arriving as the embedding-leaf-skainet cartridge from the
 * local Maven repository. Needs the pinned model snapshot on disk (the blueprint's fetch step);
 * the manifest block is derived from the cartridge's intrinsic descriptor, so the runtime-side
 * parity check compares like with like.
 */
internal fun embedWithLeaf(chunks: List<Chunk>): Pair<EmbeddingInfo, FloatArray> {
    val descriptor = LeafEmbeddingCartridge.descriptor
    val info = EmbeddingInfo(
        modelId = LeafEmbeddingCartridge.SPACE_FINGERPRINT,
        dimensions = descriptor.dimensions,
        normalized = descriptor.normalized,
        queryPrefix = descriptor.queryPrefix,
        documentPrefix = descriptor.documentPrefix,
    )
    val vectors = try {
        runBlocking {
            LeafEmbeddingCartridge.open().use { engine ->
                val all = FloatArray(chunks.size * engine.dimensions)
                chunks.forEachIndexed { index, chunk ->
                    engine.embed(descriptor.documentPrefix + chunk.embeddedText)
                        .copyInto(all, index * engine.dimensions)
                }
                all
            }
        }
    } catch (failure: IllegalStateException) {
        fail("leaf embedder: ${failure.message}")
    }
    return info to vectors
}
