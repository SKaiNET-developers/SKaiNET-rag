package sk.ainet.rag.sample.cli

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.runBlocking
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell

@OptIn(ExperimentalForeignApi::class)
actual fun readFileBytes(path: String): ByteArray {
    val file = fopen(path, "rb") ?: error("cannot open $path")
    try {
        fseek(file, 0, SEEK_END)
        val size = ftell(file).toInt()
        fseek(file, 0, SEEK_SET)
        val bytes = ByteArray(size)
        if (size > 0) {
            bytes.usePinned { pinned ->
                check(fread(pinned.addressOf(0), 1.convert(), size.convert(), file).toInt() == size) {
                    "short read on $path"
                }
            }
        }
        return bytes
    } finally {
        fclose(file)
    }
}

fun main(args: Array<String>) = runBlocking { runCli(args) }
