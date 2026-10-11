package sk.ainet.rag.core.index

/** One retrieval hit: which chunk, and how strongly the ranking believes in it. */
data class ScoredChunk(val chunkIndex: Int, val score: Float)
