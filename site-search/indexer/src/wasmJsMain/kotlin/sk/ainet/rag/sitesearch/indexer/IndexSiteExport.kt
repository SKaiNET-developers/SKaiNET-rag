@file:OptIn(ExperimentalJsExport::class, ExperimentalEncodingApi::class)

package sk.ainet.rag.sitesearch.indexer

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * The Node-facing export: one synchronous call, a JSON envelope back — the same `ok`
 * discriminator convention as the search core, because a thrown Kotlin exception crosses the
 * Wasm boundary as an opaque `WebAssembly.Exception` and a build tool deserves real error text.
 * The bundle rides inside as base64 (`Buffer.from(bundleBase64, 'base64')` on the JS side);
 * at documentation-site scale the transcoding cost is noise against the embedding pass.
 */

private val json = Json { encodeDefaults = true }

@Serializable
private data class IndexOk(
    val ok: Boolean = true,
    val chunkCount: Int,
    val pageCount: Int,
    val warnings: List<String>,
    val bundleBase64: String,
)

@Serializable
private data class IndexFailure(val ok: Boolean = false, val error: String)

/**
 * [pagesJson] is a JSON array of page records (see `SiteModels.kt` for the contract),
 * [configJson] a `SiteIndexConfig` object. Resolves the work synchronously — indexing is pure
 * computation; schedule it off any latency-sensitive path on the caller's side.
 */
@JsExport
fun indexSite(pagesJson: String, configJson: String): String = try {
    val result = SiteIndexJson.index(pagesJson, configJson)
    json.encodeToString(
        IndexOk(
            chunkCount = result.chunkCount,
            pageCount = result.pageCount,
            warnings = result.warnings,
            bundleBase64 = Base64.encode(result.bundle),
        ),
    )
} catch (failure: Throwable) {
    json.encodeToString(IndexFailure(error = failure.message ?: "site indexing failed"))
}
