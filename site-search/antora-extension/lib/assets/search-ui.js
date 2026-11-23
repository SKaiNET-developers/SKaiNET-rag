/**
 * Fallback search UI: functional, dependency-free, deliberately plain. A UI bundle that ships
 * its own `search-scripts.hbs` (skainet-docs-ui will) replaces all of this and owns the look;
 * this file exists so a site on any UI gets working search from the extension alone.
 *
 * Behavioral contract (kept in parity with the antora-lunr-extension UI so themes port):
 * result list classes `search-result-dropdown-menu` / `search-result-item` /
 * `search-result-document-title` / `search-result-document-hit`, input `#search-input`,
 * facet checkbox `input[data-facet-filter="component:<name>"]` inside `#search-field`.
 * The engine (~250 KB compressed) loads lazily on first focus — a visitor who never searches
 * pays nothing.
 */

const script = document.getElementById('search-ui-script') ?? document.currentScript;
const config = script?.dataset ?? {};
const input = document.getElementById('search-input');

if (input) {
  const snippetLength = Number(config.snippetLength ?? 100);
  let enginePromise;
  let dropdown;
  let lastQuery = '';

  const ensureEngine = () => {
    enginePromise ??= (async () => {
      const { skainetSearch } = await import(`${config.enginePath}/skainet-search.js`);
      return skainetSearch.init({ bundleUrl: config.bundlePath });
    })();
    return enginePromise;
  };

  const facetFilter = () => {
    const facet = document.querySelector('#search-field input[data-facet-filter]:checked');
    if (!facet) return undefined;
    const [key, value] = facet.dataset.facetFilter.split(':');
    return key === 'component' ? { category: value } : undefined;
  };

  const ensureDropdown = () => {
    if (!dropdown) {
      dropdown = document.createElement('div');
      dropdown.classList.add('search-result-dropdown-menu');
      input.parentNode.appendChild(dropdown);
      document.addEventListener('click', (event) => {
        if (!dropdown.contains(event.target) && event.target !== input) hide();
      });
    }
    return dropdown;
  };

  const hide = () => dropdown && (dropdown.innerHTML = '');

  const render = (hits) => {
    const menu = ensureDropdown();
    menu.innerHTML = '';
    if (!hits.length) {
      const item = document.createElement('div');
      item.classList.add('search-result-item');
      item.textContent = 'No results found.';
      menu.appendChild(item);
      return;
    }
    for (const hit of hits) {
      const item = document.createElement('div');
      item.classList.add('search-result-item');
      const link = document.createElement('a');
      link.href = (config.siteRootPath || '') + hit.url + (hit.anchor ? `#${hit.anchor}` : '');
      const title = document.createElement('div');
      title.classList.add('search-result-document-title');
      title.textContent = hit.heading ? `${hit.title} — ${hit.heading}` : hit.title;
      const snippet = document.createElement('div');
      snippet.classList.add('search-result-document-hit');
      snippet.textContent =
        hit.text.length > snippetLength ? `${hit.text.slice(0, snippetLength)}…` : hit.text;
      link.appendChild(title);
      link.appendChild(snippet);
      item.appendChild(link);
      menu.appendChild(item);
    }
  };

  let debounce;
  const search = () => {
    const query = input.value.trim();
    lastQuery = query;
    if (query.length < 2) return hide();
    ensureEngine()
      .then((handle) => handle.search(query, { limit: 10, filter: facetFilter() }))
      .then((hits) => {
        if (lastQuery === input.value.trim()) render(hits);
      })
      .catch((error) => console.error('skainet-site-search:', error));
  };

  input.addEventListener('focus', ensureEngine, { once: true });
  input.addEventListener('pointerenter', ensureEngine, { once: true });
  input.addEventListener('input', () => {
    clearTimeout(debounce);
    debounce = setTimeout(search, 100);
  });
  input.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      input.value = '';
      hide();
    }
  });
  document.addEventListener('keydown', (event) => {
    if ((event.key === '/' || event.key === 's') && !event.metaKey && !event.ctrlKey && !event.altKey) {
      const active = document.activeElement;
      const typing = active && (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA' || active.isContentEditable);
      if (!typing && script?.dataset.searchShortcuts !== 'false') {
        event.preventDefault();
        input.focus();
      }
    }
  });
}
