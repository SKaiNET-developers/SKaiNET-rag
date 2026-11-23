# @skainet/antora-site-search

Client-side **hybrid search** for [Antora](https://antora.org) sites: BM25 plus
[SKaiNET](https://github.com/SKaiNET-developers/SKaiNET) vector retrieval, fused by reciprocal
rank. The site is indexed at build time and queries are answered entirely in the visitor's
browser — the same compiled Kotlin on both sides, so index/query parity holds by construction,
and no query ever leaves the tab.

Honest expectation-setting: the default embedder (`skainet-hrp-v1`, a weight-free hashed random
projection) rewards vocabulary overlap, not meaning — it will not match "login" to
"authentication". It sharpens ranking over plain BM25 and keeps the bundle deterministic; the
engine's manifest contract is built for swapping in neural embedders as they land.

## Enabling

```yaml
antora:
  extensions:
    - require: '@skainet/antora-site-search'
      index_all_versions: false   # default: latest version per component
      category: component         # facet key: component | module
      embed: true                 # false -> BM25-only bundle (smaller, still first-class)
      dump_pages: ''              # debug: write the neutral extraction JSON to this path
```

Requirements: **Node >= 22** (the indexer runs as a WasmGC module in-process).

What it does:

- `uiLoaded` — sets `SITE_SEARCH_PROVIDER=skainet`, which activates the standard
  `#search-input` hook and `{{> search-scripts}}` partial that antora-ui-default (and UIs
  derived from it, like skainet-docs-ui) already carry. If the UI ships no `search-scripts.hbs`
  of its own, a default one is supplied — **a UI-supplied partial always wins**, so the UI
  bundle owns look and feel.
- `beforePublish` — extracts the *rendered* pages (`<article class="doc">`, final heading ids,
  `page.pub.url`; pages with `:noindex:` are skipped), indexes them through the Kotlin/Wasm
  indexer, and publishes `_search/search.lrb` plus the query engine next to the site.

Reproducible builds: set `SOURCE_DATE_EPOCH` and identical content produces an identical
`search.lrb`, byte for byte — diffable in CI.

## The search UI

Without a UI-supplied partial you get `_search/search-ui.js`, a plain but functional dropdown
wired to `#search-input`, using antora-lunr-extension's result CSS classes so existing theme
rules port. It loads the engine lazily on first focus; the component facet checkbox contract
(`input[data-facet-filter="component:name"]` inside `#search-field`) is honored.

UIs that want to own the experience ship their own `partials/search-scripts.hbs` and script
against the engine's public API:

```js
const { skainetSearch } = await import(`${enginePath}/skainet-search.js`);
const handle = await skainetSearch.init({ bundleUrl });
const hits = await handle.search(query, { limit: 10, filter: { category: 'skainet' } });
// hit: { url, anchor, title, heading, text, score, source, component, headingPath }
```

## Developing in this repo

The `dist/` directory (engine + indexer wasm) is built, not committed:

```bash
./gradlew :site-search:search-core:packEngineNpm :site-search:indexer:packNpm
npm install && npm test
```
