package sk.ainet.rag.core.index

import sk.ainet.rag.core.bundle.BundleFormatException
import sk.ainet.rag.core.bundle.ByteReader
import sk.ainet.rag.core.bundle.ByteWriter

/**
 * Compact binary form of the BM25 inverted index, replacing the JSON postings dump.
 *
 * An inverted index is already the sparse representation — no zeros are stored — but JSON spends
 * most of its bytes on repeated keys and decimal text, and parsing it dominates bundle load time.
 * This layout is CSR in spirit: a sorted term dictionary, then one postings run per term with
 * delta+varint chunk ids. Weighted frequencies stay exact f32 so ranking is bit-identical to the
 * in-memory index the build produced.
 *
 * Layout (all integers little-endian, varints LEB128):
 *   u32  format version (1)
 *   f32  k1, f32 b, f32 averageChunkLength
 *   u32  chunkCount, then chunkCount × f32 weighted chunk lengths
 *   u32  termCount, then per term (sorted by UTF-8 bytes):
 *        varint termLength + UTF-8 bytes
 *        varint postingCount
 *        postingCount × (varint chunkIndex delta from previous posting, f32 weightedFrequency)
 */
object Bm25Binary {

    const val VERSION = 1

    fun encode(snapshot: Bm25Index.Snapshot): ByteArray {
        val out = ByteWriter(initialCapacity = 64 * 1024)
        out.u32(VERSION)
        out.f32(snapshot.k1)
        out.f32(snapshot.b)
        out.f32(snapshot.averageChunkLength)
        out.u32(snapshot.chunkLengths.size)
        for (length in snapshot.chunkLengths) out.f32(length)

        val terms = snapshot.postings.keys.sorted()
        out.u32(terms.size)
        for (term in terms) {
            val encoded = term.encodeToByteArray()
            out.varint(encoded.size)
            out.bytes(encoded)
            // Postings are emitted ordered by chunk index so deltas stay non-negative.
            val postings = snapshot.postings.getValue(term).sortedBy { it.chunkIndex }
            out.varint(postings.size)
            var previous = 0
            for (posting in postings) {
                out.varint(posting.chunkIndex - previous)
                out.f32(posting.weightedFrequency)
                previous = posting.chunkIndex
            }
        }
        return out.toByteArray()
    }

    fun decode(bytes: ByteArray): Bm25Index.Snapshot {
        val reader = ByteReader(bytes)
        val version = reader.u32()
        if (version > VERSION) {
            throw BundleFormatException("bm25 binary version $version is newer than the supported $VERSION")
        }
        val k1 = reader.f32()
        val b = reader.f32()
        val averageChunkLength = reader.f32()
        val chunkCount = reader.u32()
        val chunkLengths = List(chunkCount) { reader.f32() }

        val termCount = reader.u32()
        val postings = LinkedHashMap<String, List<Bm25Index.Posting>>(termCount)
        repeat(termCount) {
            val term = reader.bytes(reader.varint()).decodeToString()
            val postingCount = reader.varint()
            var chunkIndex = 0
            val runs = ArrayList<Bm25Index.Posting>(postingCount)
            repeat(postingCount) {
                chunkIndex += reader.varint()
                runs += Bm25Index.Posting(chunkIndex, reader.f32())
            }
            postings[term] = runs
        }

        return Bm25Index.Snapshot(
            k1 = k1,
            b = b,
            averageChunkLength = averageChunkLength,
            chunkLengths = chunkLengths,
            postings = postings,
        )
    }
}
