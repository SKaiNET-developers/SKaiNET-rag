package sk.ainet.rag.sitesearch.indexer

import sk.ainet.rag.core.bundle.BundleCodec
import sk.ainet.rag.core.bundle.EmbeddingInfo
import sk.ainet.rag.core.document.Chunk
import sk.ainet.rag.embedder.skainet.SkaiNetHashedProjectionEmbedder

/**
 * Pages in, one `.lrb` bundle out — the same encoding, embedder and determinism contract as
 * `rag-bundler`, minus everything that assumes a curated Markdown corpus. Identical records and
 * config produce identical bytes on every target, which is what lets a docs pipeline diff its
 * search index in CI the way this repo diffs its demo bundle.
 */
class SiteIndexer(private val config: SiteIndexConfig) {

    fun index(pages: List<PageRecord>): SiteIndexResult {
        val chunker = SiteChunker(config)
        val chunks = mutableListOf<Chunk>()
        val warnings = mutableListOf<String>()
        for (page in pages) {
            val result = chunker.chunk(page)
            chunks += result.chunks
            warnings += result.warnings
        }
        require(chunks.isNotEmpty()) { "no indexable text in ${pages.size} page(s)" }

        var embedding: EmbeddingInfo? = null
        var vectors: FloatArray? = null
        if (config.embed) {
            embedding = EmbeddingInfo(
                modelId = SkaiNetHashedProjectionEmbedder.MODEL_ID,
                dimensions = config.dimensions,
                normalized = true,
                queryPrefix = "",
                documentPrefix = "",
            )
            vectors = SkaiNetHashedProjectionEmbedder(config.dimensions).use { embedder ->
                val all = FloatArray(chunks.size * config.dimensions)
                chunks.forEachIndexed { index, chunk ->
                    embedder.embed(chunk.embeddedText).copyInto(all, index * config.dimensions)
                }
                all
            }
        }

        val bundle = BundleCodec.encode(
            chunks = chunks,
            vectors = vectors,
            clusters = emptyList(),
            contentVersion = config.contentVersion,
            builtAt = config.builtAt,
            embedding = embedding,
        )
        return SiteIndexResult(
            bundle = bundle,
            chunkCount = chunks.size,
            pageCount = pages.size,
            warnings = warnings,
        )
    }
}
