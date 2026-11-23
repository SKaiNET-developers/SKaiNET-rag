package sk.ainet.rag.bundler

import sk.ainet.rag.core.bundle.BundleCodec
import sk.ainet.rag.core.bundle.Cluster
import sk.ainet.rag.core.bundle.EmbeddingInfo
import sk.ainet.rag.core.document.Chunk
import sk.ainet.rag.core.document.Chunker
import sk.ainet.rag.core.document.DocumentIssue
import sk.ainet.rag.core.document.DocumentStatus
import sk.ainet.rag.core.document.DocumentValidator
import sk.ainet.rag.core.document.FrontMatterParser
import sk.ainet.rag.core.document.ParsedDocument
import sk.ainet.rag.core.document.ValidationConfig
import sk.ainet.rag.embedder.skainet.SkaiNetHashedProjectionEmbedder
import java.io.File
import kotlin.system.exitProcess
import kotlinx.serialization.json.Json

/**
 * The build-time half of the skeleton: Markdown docs in, one bundle file out.
 *
 * Embedding runs in-process through the same compiled SKaiNET embedder the runtime uses — the
 * sidecar protocol and its interpreter are gone, and with them the twin-stack parity risk the
 * manifest contract existed to police. The contract stays anyway: it still catches a stale
 * bundle meeting a newer embedder.
 */
fun main(args: Array<String>) {
    val options = Options.parse(args) ?: run {
        System.err.println(Options.USAGE)
        exitProcess(2)
    }

    val docsDir = File(options.docs)
    if (!docsDir.isDirectory) fail("docs directory not found: ${options.docs}")
    val sources = docsDir.walkTopDown()
        .filter { it.isFile && it.extension == "md" }
        .sortedBy { it.relativeTo(docsDir).path }
        .toList()
    if (sources.isEmpty()) fail("no .md documents under ${options.docs}")

    val issues = mutableListOf<DocumentIssue>()
    val documents = mutableListOf<ParsedDocument>()
    for (file in sources) {
        val result = FrontMatterParser.parse(file.relativeTo(docsDir).path, file.readText())
        issues += result.issues
        val document = result.document?.let { withUrl(it, options) } ?: continue
        if (document.status == DocumentStatus.PUBLISHED) documents += document
    }

    val today = java.time.LocalDate.now().toString()
    issues += DocumentValidator(ValidationConfig(categories = options.categories)).validate(documents, today)

    val chunker = Chunker()
    val chunks = mutableListOf<Chunk>()
    for (document in documents) {
        val result = chunker.chunk(document)
        issues += result.issues
        chunks += result.chunks
    }

    issues.forEach { System.err.println(it) }
    if (issues.any { it.severity == DocumentIssue.Severity.ERROR }) {
        fail("validation failed with ${issues.count { it.severity == DocumentIssue.Severity.ERROR }} error(s)")
    }
    if (chunks.isEmpty()) fail("no published chunks; nothing to bundle")

    var embedding: EmbeddingInfo? = null
    var vectors: FloatArray? = null
    if (options.embed) {
        val embedded = when (options.embedder) {
            EmbedderKind.HRP -> embedWithHrp(chunks, options.dimensions)
            EmbedderKind.LEAF -> {
                if ("--dimensions" in args) {
                    fail("--dimensions applies to the hrp embedder; leaf is fixed at the cartridge's dimensions")
                }
                embedWithLeaf(chunks)
            }
        }
        embedding = embedded.first
        vectors = embedded.second
    }

    val clusters = options.clusters?.let { path ->
        Json { ignoreUnknownKeys = true }.decodeFromString<List<Cluster>>(File(path).readText())
    } ?: emptyList()

    val bytes = BundleCodec.encode(
        chunks = chunks,
        vectors = vectors,
        clusters = clusters,
        contentVersion = options.contentVersion,
        builtAt = options.builtAt ?: java.time.Instant.now().toString(),
        embedding = embedding,
    )
    val out = File(options.out)
    out.parentFile?.mkdirs()
    out.writeBytes(bytes)
    println(
        "wrote ${out.path}: ${bytes.size} bytes, ${chunks.size} chunks from ${documents.size} documents" +
            if (embedding != null) {
                ", ${embedding.dimensions}-d vectors (${embedding.modelId})"
            } else {
                ", BM25 only"
            },
    )
}

/**
 * Resolves the chunk-level page URL: an explicit front-matter `url:` wins; otherwise `--url-base`
 * derives one from the source path (`java-21.md` → `<base>/java-21<suffix>`). Without either,
 * [ParsedDocument.urlPath] stays null and the bundle bytes are identical to a pre-URL build.
 */
private fun withUrl(document: ParsedDocument, options: Options): ParsedDocument {
    val base = options.urlBase
    if (document.urlPath != null || base == null) return document
    val page = document.sourcePath.removeSuffix(".md") + options.urlSuffix
    return document.copy(urlPath = if (base.isEmpty()) page else base.trimEnd('/') + "/" + page)
}

/** The in-repo hashed random projection: weight-free, deterministic, always available. */
private fun embedWithHrp(chunks: List<Chunk>, dimensions: Int): Pair<EmbeddingInfo, FloatArray> {
    val info = EmbeddingInfo(
        modelId = SkaiNetHashedProjectionEmbedder.MODEL_ID,
        dimensions = dimensions,
        normalized = true,
        queryPrefix = "",
        documentPrefix = "",
    )
    val vectors = SkaiNetHashedProjectionEmbedder(dimensions).use { embedder ->
        val all = FloatArray(chunks.size * dimensions)
        chunks.forEachIndexed { index, chunk ->
            embedder.embed(chunk.embeddedText).copyInto(all, index * dimensions)
        }
        all
    }
    return info to vectors
}

// embedWithLeaf lives in src/main-leaf (the cartridge-backed implementation) or
// src/main-noleaf (a runtime refusal), selected by -Plocalrag.noLeaf in the build script.

internal enum class EmbedderKind { HRP, LEAF }

internal fun fail(message: String): Nothing {
    System.err.println("rag-bundler: $message")
    exitProcess(1)
}

internal data class Options(
    val docs: String,
    val out: String,
    val contentVersion: Int,
    val categories: List<String>,
    val embed: Boolean,
    val embedder: EmbedderKind,
    val dimensions: Int,
    val clusters: String?,
    val builtAt: String?,
    val urlBase: String?,
    val urlSuffix: String,
) {
    companion object {
        const val USAGE: String =
            "usage: rag-bundler --docs <dir> --out <file> --content-version <n> " +
                "[--categories a,b,c] [--no-embed] [--embedder hrp|leaf] " +
                "[--dimensions 256 (hrp only)] " +
                "[--clusters clusters.json] [--built-at <iso-instant>] " +
                "[--url-base <prefix>] [--url-suffix <.html>]"

        fun parse(args: Array<String>): Options? {
            fun value(name: String): String? =
                args.indexOf(name).takeIf { it >= 0 && it + 1 < args.size }?.let { args[it + 1] }
            return Options(
                docs = value("--docs") ?: return null,
                out = value("--out") ?: return null,
                contentVersion = value("--content-version")?.toIntOrNull() ?: return null,
                categories = value("--categories")?.split(',')?.filter { it.isNotBlank() } ?: emptyList(),
                embed = "--no-embed" !in args,
                embedder = when (value("--embedder")) {
                    null, "hrp" -> EmbedderKind.HRP
                    "leaf" -> EmbedderKind.LEAF
                    else -> return null
                },
                dimensions = value("--dimensions")?.toIntOrNull()
                    ?: SkaiNetHashedProjectionEmbedder.DEFAULT_DIMENSIONS,
                clusters = value("--clusters"),
                builtAt = value("--built-at"),
                urlBase = value("--url-base"),
                urlSuffix = value("--url-suffix") ?: ".html",
            )
        }
    }
}
