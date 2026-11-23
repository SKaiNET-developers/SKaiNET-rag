/**
 * skainet-search — the public JavaScript API of SKaiNET Site Search (L2).
 *
 * This is the only module consumers script against; the Kotlin/Wasm core behind it
 * (skainet-search-core.mjs, loaded lazily on init) is an implementation detail whose envelope
 * protocol may change. Importing this file costs ~2 KB; the engine (~155 KB compressed wasm+js)
 * loads on the first `init()` call, so a page can import eagerly and init on first focus.
 *
 *   import { skainetSearch } from './skainet-search.js';
 *   const handle = await skainetSearch.init({ bundleUrl: '_search/search.lrb' });
 *   const hits = await handle.search('virtual threads', { filter: { category: 'skainet' } });
 *
 * Hit shape (flat; `url`+`anchor` compose the link, `headingPath` the breadcrumb):
 *   { url, anchor, title, heading, text, score, source: 'hybrid'|'bm25'|'vector',
 *     component, headingPath }
 *
 * Adapters: DocSearch-style consumers map `title`→hierarchy.lvl0, `heading`→lvl1,
 * `url + '#' + anchor`→url; Pagefind-style consumers already get hydrated results — `text` is
 * the untruncated section body, snippeting is deliberately left to the presentation layer.
 *
 * The wasm core holds one index per page: a second init() replaces the first corpus, and the
 * previous handle goes dead. Site search never needs two corpora at once; if that changes, the
 * constraint lifts in the core, not in this API.
 */

const DEFAULT_LIMIT = 25;

async function init({ bundleUrl, appVersion = '0', limit = DEFAULT_LIMIT, corePath } = {}) {
  if (!bundleUrl) throw new Error('skainetSearch.init: bundleUrl is required');
  const core = await import(
    corePath ?? new URL('./skainet-search-core.mjs', import.meta.url).href
  );
  const { ok: _initOk, ...info } = unwrap(
    JSON.parse(await core.coreInit(bundleUrl, String(appVersion), limit)),
  );
  let released = false;

  const handle = {
    /** { chunkCount, usingVectors, contentVersion } — resolved; kept a promise for API shape. */
    ready: Promise.resolve(info),
    info,
    async search(query, { limit: perQuery = limit, filter = {} } = {}) {
      if (released) throw new Error('skainetSearch: handle was released');
      if (!query || !query.trim()) return [];
      const categories = filter.category ? [].concat(filter.category).join(',') : '';
      const result = unwrap(
        JSON.parse(await core.coreSearch(query, Math.min(perQuery, limit), categories)),
      );
      return result.hits;
    },
    release() {
      if (released) return;
      released = true;
      core.coreRelease();
    },
  };
  return handle;
}

function unwrap(envelope) {
  if (!envelope.ok) throw new Error(envelope.error || 'skainet-search engine failure');
  return envelope;
}

export const skainetSearch = { init };
export default skainetSearch;
