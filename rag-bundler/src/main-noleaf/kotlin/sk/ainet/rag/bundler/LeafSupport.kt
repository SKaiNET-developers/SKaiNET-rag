package sk.ainet.rag.bundler

import sk.ainet.rag.core.bundle.EmbeddingInfo
import sk.ainet.rag.core.document.Chunk

/**
 * The stub compiled by `-Plocalrag.noLeaf=true` (environments without the ~/.m2 cartridge,
 * such as CI indexing with HRP). Selecting `--embedder leaf` refuses at runtime with the
 * remedy instead of the whole module refusing to compile.
 */
internal fun embedWithLeaf(chunks: List<Chunk>): Pair<EmbeddingInfo, FloatArray> =
    fail(
        "leaf support is not compiled into this build (-Plocalrag.noLeaf=true); rebuild without " +
            "that property after publishing the embedding-leaf-skainet cartridge to ~/.m2",
    )
