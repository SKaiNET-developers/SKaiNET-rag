package sk.ainet.rag.sample.cli

import java.io.File
import kotlinx.coroutines.runBlocking

actual fun readFileBytes(path: String): ByteArray = File(path).readBytes()

fun main(args: Array<String>) = runBlocking { runCli(args) }
