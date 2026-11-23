package sk.ainet.rag.sample.sitesearch

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLAnchorElement
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

/**
 * The embeddable site-search widget. A host page carries nothing but a mount point and this
 * script — `<div id="skainet-search"></div>` plus `<script defer src="app.js"
 * data-bundle="docs.lrb">` — and the widget builds its own form and result list inside the
 * mount. Every result links to the page and heading anchor the chunk came from
 * ([Passage.href]), which is what separates a search engine from a passage viewer.
 *
 * This DOM contract (mount div id, `data-bundle` on the script tag) is the embryo of the
 * generic static-site embed; treat it as stable.
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

private suspend fun loadBundleBytes(url: String): ByteArray {
    val response = fetchUrl(url).await<FetchResponse>()
    check(response.ok) { "fetching $url failed" }
    return response.arrayBuffer().await<ArrayBuffer>().toByteArray()
}

fun main() {
    val script = document.querySelector("script[data-bundle]") as? HTMLElement
    val bundleUrl = script?.getAttribute("data-bundle") ?: "docs.lrb"
    val mount = document.getElementById(
        script?.getAttribute("data-mount") ?: "skainet-search",
    ) as? HTMLElement ?: return

    val scope = CoroutineScope(Dispatchers.Default)
    val rag = LocalRag(
        LocalRag.Config(
            loadBundle = { loadBundleBytes(bundleUrl) },
            appVersion = "1.0",
            topK = 8,
        ),
    )

    val ui = buildWidget(mount)

    fun search(query: String) {
        if (query.isBlank()) return
        scope.launch { render(ui.results, rag.retrieveOnly(query)) }
    }

    scope.launch {
        rag.state.collect { state ->
            when (state) {
                is LocalRagState.Idle, is LocalRagState.Loading ->
                    ui.status.textContent = "Loading search index…"

                is LocalRagState.Ready -> {
                    ui.status.textContent = "${state.chunkCount} sections indexed · " +
                        if (state.usingVectors) "hybrid (BM25 + SKaiNET vectors)" else "BM25 only"
                    ui.button.disabled = false
                    // Shareable deep link: ?q= searches as soon as the index is ready.
                    val initial = URLSearchParams(window.location.search.toJsString()).get("q")
                    if (!initial.isNullOrBlank() && ui.input.value.isEmpty()) {
                        ui.input.value = initial
                        search(initial)
                    }
                }

                is LocalRagState.Failed -> ui.status.textContent = "Search failed: ${state.reason}"
            }
        }
    }

    ui.form.addEventListener("submit") { event ->
        event.preventDefault()
        search(ui.input.value)
    }

    scope.launch { rag.initialize() }
}

private class WidgetUi(
    val form: HTMLFormElement,
    val input: HTMLInputElement,
    val button: HTMLButtonElement,
    val status: HTMLElement,
    val results: HTMLElement,
)

private fun buildWidget(mount: HTMLElement): WidgetUi {
    val form = document.createElement("form") as HTMLFormElement
    form.className = "sks-form"

    val input = document.createElement("input") as HTMLInputElement
    input.type = "search"
    input.placeholder = "Search the docs — try “which JEP introduced virtual threads”"
    input.setAttribute("aria-label", "Search the docs")

    val button = document.createElement("button") as HTMLButtonElement
    button.type = "submit"
    button.textContent = "Search"
    button.disabled = true

    form.appendChild(input)
    form.appendChild(button)

    val status = document.createElement("div") as HTMLElement
    status.className = "sks-status"
    status.textContent = "Loading search index…"

    val results = document.createElement("div") as HTMLElement
    results.className = "sks-results"

    mount.appendChild(form)
    mount.appendChild(status)
    mount.appendChild(results)
    return WidgetUi(form, input, button, status, results)
}

private fun render(container: HTMLElement, passages: List<Passage>) {
    container.textContent = ""
    if (passages.isEmpty()) {
        val empty = document.createElement("p") as HTMLElement
        empty.className = "sks-empty"
        empty.textContent = "No results."
        container.appendChild(empty)
        return
    }
    for (passage in passages) container.appendChild(hit(passage))
}

private fun hit(passage: Passage): HTMLElement {
    val item = document.createElement("article") as HTMLElement
    item.className = "sks-hit"

    val title = document.createElement("h3") as HTMLElement
    val label = listOfNotNull(passage.title, passage.heading).joinToString(" — ")
    val href = passage.href
    if (href != null) {
        val link = document.createElement("a") as HTMLAnchorElement
        link.href = href
        link.textContent = label
        title.appendChild(link)
    } else {
        title.textContent = label
    }
    item.appendChild(title)

    val badge = document.createElement("span") as HTMLElement
    badge.className = "sks-badge " + badgeClass(passage.source)
    badge.textContent = passage.source.name
    title.appendChild(badge)

    val snippet = document.createElement("p") as HTMLElement
    snippet.textContent = snippetOf(passage.text)
    item.appendChild(snippet)
    return item
}

private fun badgeClass(source: MatchSource): String = when (source) {
    MatchSource.HYBRID, MatchSource.PRECOMPUTED -> "hybrid"
    MatchSource.VECTOR -> "vector"
    MatchSource.BM25 -> "bm25"
}

private fun snippetOf(text: String, budget: Int = 200): String {
    val flat = text.replace('\n', ' ')
    if (flat.length <= budget) return flat
    val cut = flat.lastIndexOf(' ', budget).takeIf { it > 0 } ?: budget
    return flat.take(cut) + "…"
}
