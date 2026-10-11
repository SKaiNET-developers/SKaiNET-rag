package sk.ainet.rag.core.bundle

/**
 * Minimal little-endian byte access for the bundle container, written against plain [ByteArray]
 * so the same code compiles on every target. Bundles are kilobytes to low megabytes, so whole
 * buffers in memory are fine; nothing here streams.
 */
internal class ByteReader(private val bytes: ByteArray, var position: Int = 0) {
    val remaining: Int get() = bytes.size - position

    fun u8(): Int = bytes[position++].toInt() and 0xFF

    fun u32(): Int {
        val value = (bytes[position].toInt() and 0xFF) or
            ((bytes[position + 1].toInt() and 0xFF) shl 8) or
            ((bytes[position + 2].toInt() and 0xFF) shl 16) or
            ((bytes[position + 3].toInt() and 0xFF) shl 24)
        position += 4
        return value
    }

    fun f32(): Float = Float.fromBits(u32())

    /** Unsigned LEB128. */
    fun varint(): Int {
        var result = 0
        var shift = 0
        while (true) {
            val byte = u8()
            result = result or ((byte and 0x7F) shl shift)
            if (byte and 0x80 == 0) return result
            shift += 7
            if (shift > 28) throw BundleFormatException("varint too long at $position")
        }
    }

    fun bytes(count: Int): ByteArray {
        if (count < 0 || count > remaining) {
            throw BundleFormatException("section of $count bytes exceeds the $remaining remaining")
        }
        val slice = bytes.copyOfRange(position, position + count)
        position += count
        return slice
    }
}

internal class ByteWriter(initialCapacity: Int = 1024) {
    private var buffer = ByteArray(initialCapacity)
    private var size = 0

    private fun ensure(extra: Int) {
        if (size + extra > buffer.size) {
            buffer = buffer.copyOf(maxOf(buffer.size * 2, size + extra))
        }
    }

    fun u8(value: Int) {
        ensure(1)
        buffer[size++] = value.toByte()
    }

    fun u32(value: Int) {
        ensure(4)
        buffer[size++] = value.toByte()
        buffer[size++] = (value ushr 8).toByte()
        buffer[size++] = (value ushr 16).toByte()
        buffer[size++] = (value ushr 24).toByte()
    }

    fun f32(value: Float) = u32(value.toRawBits())

    fun varint(value: Int) {
        require(value >= 0) { "varint encodes non-negative values, got $value" }
        var rest = value
        while (true) {
            val low = rest and 0x7F
            rest = rest ushr 7
            if (rest == 0) {
                u8(low)
                return
            }
            u8(low or 0x80)
        }
    }

    fun bytes(value: ByteArray) {
        ensure(value.size)
        value.copyInto(buffer, size)
        size += value.size
    }

    fun toByteArray(): ByteArray = buffer.copyOf(size)
}
