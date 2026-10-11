package sk.ainet.rag.core.bundle

import sk.ainet.rag.core.document.Chunk
import sk.ainet.rag.core.document.Chunker
import sk.ainet.rag.core.index.Bm25Binary
import sk.ainet.rag.core.index.Bm25Index
import kotlinx.serialization.json.Json

/**
 * The bundle container, readable and writable on every target.
 *
 * Replaces the upstream deflate-ZIP: `java.util.zip` does not exist off the JVM, and a RAG bundle
 * is small enough that transparency beats compression. The container is a flat section table —
 * magic, section count, then (name, length, payload) triples — so a hex dump is self-describing
 * and a reader can skip sections it does not know. Manifest and chunks stay JSON (they are small
 * and human-checkable); the BM25 index and the vectors are binary because that is where the bytes
 * and the parse time actually are. Encoding is deterministic: identical content produces an
 * identical byte stream, which keeps bundles comparable between builds.
 */
object BundleCodec {

    private const val MAGIC = "LRB2"

    fun encode(
        chunks: List<Chunk>,
        vectors: FloatArray?,
        clusters: List<Cluster>,
        contentVersion: Int,
        builtAt: String,
        embedding: EmbeddingInfo?,
    ): ByteArray {
        val index = Bm25Index.build(chunks)
        val snapshot = index.snapshot()

        require(vectors == null || embedding != null) {
            "vectors were supplied without an embedding block; the runtime could not verify parity"
        }
        if (vectors != null && embedding != null) {
            require(vectors.size == chunks.size * embedding.dimensions) {
                "expected ${chunks.size * embedding.dimensions} floats for ${chunks.size} chunks " +
                    "at ${embedding.dimensions} dimensions, got ${vectors.size}"
            }
        }

        val manifest = BundleManifest(
            contentVersion = contentVersion,
            builtAt = builtAt,
            chunkCount = chunks.size,
            chunkerVersion = Chunker.VERSION,
            bm25 = Bm25Params(snapshot.k1, snapshot.b, snapshot.averageChunkLength),
            embedding = embedding,
        )

        val sections = buildList {
            add(BundleEntries.MANIFEST to BundleJson.format.encodeToString(manifest).encodeToByteArray())
            add(BundleEntries.CHUNKS to BundleJson.format.encodeToString(chunks.map(StoredChunk::from)).encodeToByteArray())
            add(BundleEntries.BM25 to Bm25Binary.encode(snapshot))
            add(BundleEntries.CLUSTERS to BundleJson.format.encodeToString(clusters).encodeToByteArray())
            if (vectors != null) add(BundleEntries.VECTORS to vectors.toLittleEndianBytes())
        }

        val out = ByteWriter(initialCapacity = sections.sumOf { it.second.size } + 64)
        out.bytes(MAGIC.encodeToByteArray())
        out.u32(sections.size)
        for ((name, payload) in sections) {
            val nameBytes = name.encodeToByteArray()
            out.u8(nameBytes.size)
            out.bytes(nameBytes)
            out.u32(payload.size)
            out.bytes(payload)
        }
        return out.toByteArray()
    }

    fun decode(bytes: ByteArray): Bundle {
        val reader = ByteReader(bytes)
        if (bytes.size < 8 || reader.bytes(4).decodeToString() != MAGIC) {
            throw BundleFormatException("not a $MAGIC bundle")
        }
        val sections = mutableMapOf<String, ByteArray>()
        repeat(reader.u32()) {
            val name = reader.bytes(reader.u8()).decodeToString()
            sections[name] = reader.bytes(reader.u32())
        }

        val manifest = sections[BundleEntries.MANIFEST]
            ?.let { BundleJson.format.decodeFromString<BundleManifest>(it.decodeToString()) }
            ?: throw BundleFormatException("bundle has no ${BundleEntries.MANIFEST}")
        if (manifest.bundleFormatVersion > BundleManifest.CURRENT_FORMAT_VERSION) {
            throw BundleFormatException(
                "bundle format version ${manifest.bundleFormatVersion} is newer than the " +
                    "supported ${BundleManifest.CURRENT_FORMAT_VERSION}",
            )
        }

        val chunks = sections[BundleEntries.CHUNKS]
            ?.let { BundleJson.format.decodeFromString<List<StoredChunk>>(it.decodeToString()) }
            ?: throw BundleFormatException("bundle has no ${BundleEntries.CHUNKS}")
        val snapshot = sections[BundleEntries.BM25]?.let(Bm25Binary::decode)
            ?: throw BundleFormatException("bundle has no ${BundleEntries.BM25}")
        val clusters = sections[BundleEntries.CLUSTERS]
            ?.let { BundleJson.format.decodeFromString<List<Cluster>>(it.decodeToString()) }
            ?: emptyList()

        val vectors = sections[BundleEntries.VECTORS]?.let { raw ->
            val dimensions = manifest.embedding?.dimensions
                ?: throw BundleFormatException(
                    "bundle carries ${BundleEntries.VECTORS} but no embedding block to describe it",
                )
            val expected = manifest.chunkCount * dimensions * Float.SIZE_BYTES
            if (raw.size != expected) {
                throw BundleFormatException(
                    "${BundleEntries.VECTORS} is ${raw.size} bytes, expected $expected for " +
                        "${manifest.chunkCount} chunks at $dimensions dimensions",
                )
            }
            raw.toFloatArrayLittleEndian()
        }

        return Bundle(
            manifest = manifest,
            chunks = chunks,
            bm25 = Bm25Index.from(snapshot),
            vectors = vectors,
            clusters = clusters,
        )
    }

    private fun FloatArray.toLittleEndianBytes(): ByteArray {
        val out = ByteWriter(initialCapacity = size * Float.SIZE_BYTES)
        for (value in this) out.f32(value)
        return out.toByteArray()
    }

    private fun ByteArray.toFloatArrayLittleEndian(): FloatArray {
        val reader = ByteReader(this)
        return FloatArray(size / Float.SIZE_BYTES) { reader.f32() }
    }
}

class BundleFormatException(message: String) : IllegalStateException(message)

object BundleJson {
    val format: Json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
        // A BM25-only bundle must have no embedding block at all, not a null one: the runtime
        // treats the block as a parity contract, and an explicit null invites a reader to think
        // one was intended.
        explicitNulls = false
    }
}
