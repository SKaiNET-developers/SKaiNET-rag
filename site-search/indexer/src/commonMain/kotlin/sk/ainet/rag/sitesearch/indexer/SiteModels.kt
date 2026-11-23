package sk.ainet.rag.sitesearch.indexer

import kotlinx.serialization.Serializable

/**
 * The neutral extraction contract between a site generator and this indexer: one record per
 * rendered page, one [SectionRecord] per heading-delimited slice of it. The extractor owns the
 * syntax (HTML, its generator's anchor rules); everything after this boundary is semantic.
 *
 * Anchors are *scraped from the rendered page*, never recomputed here — the generator's id
 * rules (Asciidoctor's `_lower_snake` with `_2` dedup suffixes, custom `[[ids]]`…) are already
 * final in the HTML, so links scroll by construction.
 */
@Serializable
data class PageRecord(
    /** Stable page identity, e.g. `component:module:install.adoc`; chunk ids derive from it. */
    val docId: String,
    val title: String,
    /** Site-root-relative URL of the page, e.g. `/skainet/0.57/install.html`. */
    val urlPath: String,
    val component: String? = null,
    /** The component's display version label (an Antora version, not an app-version gate). */
    val version: String? = null,
    val module: String? = null,
    val sections: List<SectionRecord> = emptyList(),
)

@Serializable
data class SectionRecord(
    /** Fragment id on the page, without `#`; null for text before the first heading. */
    val anchor: String? = null,
    /** Heading trail from the outermost section heading down, e.g. `["Install", "Linux"]`. */
    val headingPath: List<String> = emptyList(),
    /** The section's prose, plain text, paragraphs separated by blank lines. */
    val text: String = "",
    /** Text of the section's code blocks — searchable identifiers, split from the prose. */
    val codeText: String = "",
)

@Serializable
data class SiteIndexConfig(
    val contentVersion: Int,
    /** An ISO instant recorded in the manifest. Pass something content-derived (the commit
     *  date, not the wall clock) when bundle bytes must be reproducible. */
    val builtAt: String,
    /** False produces a BM25-only bundle — a first-class engine state, not a degraded one. */
    val embed: Boolean = true,
    val dimensions: Int = 256,
    val maxChunkTokens: Int = 400,
    /** Which page field becomes the chunk's facet (`filter: {category}` in the search API). */
    val categoryFrom: CategoryFrom = CategoryFrom.COMPONENT,
) {
    @Serializable
    enum class CategoryFrom { COMPONENT, MODULE }
}

class SiteIndexResult(
    val bundle: ByteArray,
    val chunkCount: Int,
    val pageCount: Int,
    val warnings: List<String>,
)
