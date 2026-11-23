@file:OptIn(ExperimentalJsExport::class)

package sk.ainet.rag.sitesearch.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import sk.ainet.rag.LocalRag
import sk.ainet.rag.core.agent.LocalRagState
import sk.ainet.rag.core.agent.Passage
import kotlin.js.Promise

/**
 * The `@JsExport` surface of the engine. Every payload in both directions is a JSON string with
 * an `ok` discriminator — resolving an envelope instead of rejecting keeps error text intact
 * across the Wasm/JS boundary, where a thrown Kotlin exception surfaces as an opaque
 * `WebAssembly.Exception`. The glue layer (`skainet-search.js`) turns `ok: false` back into a
 * rejected promise, so consumers of the public API still program against rejections.
 *
 * One index per module instance: the wasm module is a page-level singleton, and a second
 * [coreInit] releases the first corpus. `fetch` is the only environment call, present in both
 * the browser and Node ≥ 18 — the same artifact serves the page and the test rig.
 */

private val scope = CoroutineScope(Dispatchers.Default)
private var rag: LocalRag? = null

private val json = Json {
    encodeDefaults = true
    explicitNulls = false
}

@Serializable
private data class InitResult(
    val ok: Boolean = true,
    val chunkCount: Int,
    val usingVectors: Boolean,
    val contentVersion: Int,
)

@Serializable
private data class SearchResult(val ok: Boolean = true, val hits: List<Hit>)

@Serializable
private data class Failure(val ok: Boolean = false, val error: String)

/** The flat hit shape of the public API — see `glue/skainet-search.js` for the contract. */
@Serializable
private data class Hit(
    val url: String?,
    val anchor: String?,
    val title: String,
    val heading: String?,
    val text: String,
    val score: Float,
    val source: String,
    val component: String?,
    val headingPath: List<String>?,
)

private fun Passage.toHit() = Hit(
    url = urlPath,
    anchor = anchor,
    title = title,
    heading = heading,
    text = text,
    score = score,
    source = source.name.lowercase(),
    component = component,
    headingPath = headingPath,
)

private external interface FetchResponse : JsAny {
    val ok: Boolean
    val status: Int
    fun arrayBuffer(): Promise<ArrayBuffer>
}

private fun fetchUrl(url: String): Promise<FetchResponse> = js("fetch(url)")

private fun ArrayBuffer.toByteArray(): ByteArray {
    val view = Int8Array(this)
    return ByteArray(view.length) { view[it] }
}

private fun resolving(block: suspend () -> String): Promise<JsString> =
    Promise { resolve, _ ->
        scope.launch {
            val payload = try {
                block()
            } catch (failure: Throwable) {
                json.encodeToString(Failure(error = failure.message ?: "search engine failure"))
            }
            resolve(payload.toJsString())
        }
    }

/**
 * Fetches and opens the bundle at [bundleUrl]; resolves
 * `{"ok":true,"chunkCount":n,"usingVectors":b,"contentVersion":n}` once retrieval is ready, or
 * `{"ok":false,"error":reason}`. [topK] fixes the per-query candidate depth for the lifetime of
 * the index — init with the largest limit the UI will ever ask for.
 */
@JsExport
fun coreInit(bundleUrl: String, appVersion: String, topK: Int): Promise<JsString> = resolving {
    rag?.release()
    val fresh = LocalRag(
        LocalRag.Config(
            loadBundle = {
                val response = fetchUrl(bundleUrl).await<FetchResponse>()
                check(response.ok) { "fetching $bundleUrl failed with HTTP ${response.status}" }
                response.arrayBuffer().await<ArrayBuffer>().toByteArray()
            },
            appVersion = appVersion,
            topK = topK.coerceAtLeast(1),
        ),
    )
    rag = fresh
    fresh.initialize()
    when (val state = fresh.state.value) {
        is LocalRagState.Ready -> json.encodeToString(
            InitResult(
                chunkCount = state.chunkCount,
                usingVectors = state.usingVectors,
                contentVersion = state.contentVersion,
            ),
        )

        is LocalRagState.Failed -> error(state.reason)
        else -> error("engine initialization did not settle")
    }
}

/**
 * Resolves `{"ok":true,"hits":[...]}` — at most [limit] hits, best first, each the flat shape
 * `{url, anchor, title, heading, text, score, source, component, headingPath}`. [categoriesCsv]
 * filters to the named facets (an empty string means all).
 */
@JsExport
fun coreSearch(query: String, limit: Int, categoriesCsv: String): Promise<JsString> = resolving {
    val engine = rag ?: error("coreInit has not completed")
    val categories = categoriesCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    val passages = engine.retrieveOnly(query, categories)
    json.encodeToString(SearchResult(hits = passages.take(limit.coerceAtLeast(1)).map { it.toHit() }))
}

/** Frees the index and the embedder. The module stays loaded; a later [coreInit] starts fresh. */
@JsExport
fun coreRelease() {
    rag?.release()
    rag = null
}
