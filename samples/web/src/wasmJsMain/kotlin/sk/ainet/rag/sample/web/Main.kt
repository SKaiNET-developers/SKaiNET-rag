package sk.ainet.rag.sample.web

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URLSearchParams
import sk.ainet.rag.LocalRag
import sk.ainet.rag.core.agent.LocalRagState
import sk.ainet.rag.core.agent.MatchSource
import sk.ainet.rag.core.agent.Passage
import kotlin.js.Promise
import kotlin.math.roundToInt

/**
 * The browser face of the same retrieval stack every other target runs: `LocalRag` from
 * `rag-runtime`, compiled to Wasm, over the bundle checked in for the Compose sample. The page
 * is static HTML; this file only wires behavior — fetch the bundle once, then every query is
 * tokenized, embedded (SKaiNET hashed projection) and fused in the tab.
 */

private external interface FetchResponse : JsAny {
    val ok: Boolean
    fun arrayBuffer(): Promise<ArrayBuffer>
}

private fun fetchUrl(url: String): Promise<FetchResponse> = js("fetch(url)")

private fun ArrayBuffer.toByteArray(): ByteArray {
    val view = Int8Array(this)
    return ByteArray(view.length) { view[it] }
}

private suspend fun loadBundleBytes(): ByteArray {
    val response = fetchUrl("docs.lrb").await<FetchResponse>()
    check(response.ok) { "fetching docs.lrb failed" }
    return response.arrayBuffer().await<ArrayBuffer>().toByteArray()
}

fun main() {
    val scope = CoroutineScope(Dispatchers.Default)
    val rag = LocalRag(
        LocalRag.Config(
            loadBundle = ::loadBundleBytes,
            appVersion = "1.0",
            topK = 4,
        ),
    )

    val status = document.getElementById("status") as HTMLElement
    val form = document.getElementById("form") as HTMLFormElement
    val queryInput = document.getElementById("query") as HTMLInputElement
    val searchButton = document.getElementById("search") as HTMLButtonElement
    val results = document.getElementById("results") as HTMLElement

    fun search(query: String) {
        if (query.isBlank()) return
        scope.launch {
            render(results, rag.retrieveOnly(query))
        }
    }

    // A shareable deep link: ?q=virtual+threads searches as soon as the corpus is ready.
    val initialQuery = URLSearchParams(window.location.search.toJsString()).get("q")
    var initialQueryRan = false

    scope.launch {
        rag.state.collect { state ->
            when (state) {
                is LocalRagState.Idle, is LocalRagState.Loading -> {
                    status.textContent = "Loading corpus…"
                    status.className = "status"
                }

                is LocalRagState.Ready -> {
                    status.textContent = "${state.chunkCount} chunks · " + if (state.usingVectors) {
                        "hybrid retrieval (BM25 + SKaiNET vectors)"
                    } else {
                        "BM25 only"
                    }
                    status.className = if (state.usingVectors) "status ok" else "status"
                    searchButton.disabled = false
                    if (!initialQueryRan && !initialQuery.isNullOrBlank()) {
                        initialQueryRan = true
                        queryInput.value = initialQuery
                        search(initialQuery)
                    }
                }

                is LocalRagState.Failed -> {
                    status.textContent = "Failed: ${state.reason}"
                    status.className = "status err"
                }
            }
        }
    }

    form.addEventListener("submit") { event ->
        event.preventDefault()
        search(queryInput.value)
    }

    val examples = document.getElementById("examples") as HTMLElement
    val chips = examples.getElementsByClassName("example")
    for (i in 0 until chips.length) {
        val chip = chips.item(i) as HTMLElement
        chip.addEventListener("click") {
            queryInput.value = chip.textContent ?: ""
            search(queryInput.value)
        }
    }

    scope.launch { rag.initialize() }
}

private fun render(container: HTMLElement, passages: List<Passage>) {
    container.textContent = ""
    if (passages.isEmpty()) {
        val empty = document.createElement("p") as HTMLElement
        empty.className = "empty"
        empty.textContent = "No passages found."
        container.appendChild(empty)
        return
    }
    for (passage in passages) container.appendChild(card(passage))
}

private fun card(passage: Passage): HTMLElement {
    val card = document.createElement("article") as HTMLElement
    card.className = "card"

    val title = document.createElement("h3") as HTMLElement
    title.textContent = listOfNotNull(passage.title, passage.heading).joinToString(" — ")
    card.appendChild(title)

    val meta = document.createElement("div") as HTMLElement
    meta.className = "meta"
    val badge = document.createElement("span") as HTMLElement
    badge.className = "badge " + badgeClass(passage.source)
    badge.textContent = passage.source.name
    meta.appendChild(badge)
    meta.append("score ${format(passage.score)}")
    card.appendChild(meta)

    val text = document.createElement("p") as HTMLElement
    text.textContent = passage.text
    card.appendChild(text)
    return card
}

private fun badgeClass(source: MatchSource): String = when (source) {
    MatchSource.HYBRID -> "hybrid"
    MatchSource.VECTOR -> "vector"
    MatchSource.BM25 -> "bm25"
    MatchSource.PRECOMPUTED -> "hybrid"
}

private fun format(score: Float): String {
    val thousandths = (score * 1000).roundToInt()
    return "${thousandths / 1000}.${(thousandths % 1000).toString().padStart(3, '0')}"
}
