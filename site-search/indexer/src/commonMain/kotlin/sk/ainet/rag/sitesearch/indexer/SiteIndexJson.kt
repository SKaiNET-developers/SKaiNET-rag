package sk.ainet.rag.sitesearch.indexer

import kotlinx.serialization.json.Json

/**
 * The string-boundary face of [SiteIndexer], shared by the wasm export and any host that
 * prefers JSON over data classes. Unknown keys are ignored so the extraction side can grow
 * fields before this side learns them.
 */
object SiteIndexJson {

    val format: Json = Json { ignoreUnknownKeys = true }

    fun index(pagesJson: String, configJson: String): SiteIndexResult {
        val pages = format.decodeFromString<List<PageRecord>>(pagesJson)
        val config = format.decodeFromString<SiteIndexConfig>(configJson)
        return SiteIndexer(config).index(pages)
    }
}
