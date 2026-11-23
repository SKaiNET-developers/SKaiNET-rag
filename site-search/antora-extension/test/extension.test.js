'use strict'

/**
 * Integration: register the extension against fake Antora catalogs and let it run the real
 * Kotlin/Wasm indexer. Needs the engine built into dist/ first:
 *
 *   ./gradlew :site-search:search-core:packEngineNpm :site-search:indexer:packNpm
 */

const test = require('node:test')
const assert = require('node:assert/strict')
const { readFileSync, existsSync } = require('node:fs')
const ospath = require('node:path')
const { register } = require('../lib/index')

const distReady = existsSync(ospath.join(__dirname, '..', 'dist', 'indexer'))
const html = readFileSync(ospath.join(__dirname, 'fixtures', 'install.html'), 'utf8')

function fakeContext () {
  const listeners = {}
  return {
    on (name, fn) {
      listeners[name] = fn
    },
    getLogger () {
      return { info () {}, warn () {} }
    },
    listeners,
  }
}

function fakePage (overrides = {}) {
  return {
    out: {},
    contents: Buffer.from(html),
    title: 'Install SKaiNET',
    asciidoc: { attributes: {} },
    src: { component: 'skainet', version: '0.57', module: 'ROOT', relative: 'install.adoc' },
    pub: { url: '/skainet/0.57/install.html' },
    ...overrides,
  }
}

function fakeCatalogs (pages) {
  return {
    playbook: { env: {}, dir: __dirname },
    uiCatalog: {
      files: [],
      findByType: () => [],
      addFile (file) {
        this.files.push(file)
      },
    },
    contentCatalog: {
      getComponents: () => [{ name: 'skainet', latest: { version: '0.57' } }],
      getPages: (filter) => pages.filter(filter),
    },
    siteCatalog: {
      files: [],
      addFile (file) {
        this.files.push(file)
      },
    },
  }
}

test('uiLoaded activates the search hooks and supplies the default partial', () => {
  const ctx = fakeContext()
  register.call(ctx, { config: {} })
  const { playbook, uiCatalog } = fakeCatalogs([])
  ctx.listeners.uiLoaded({ playbook, uiCatalog })

  assert.equal(playbook.env.SITE_SEARCH_PROVIDER, 'skainet')
  const partial = uiCatalog.files.find((f) => f.path === 'partials/search-scripts.hbs')
  assert.ok(partial, 'a default search-scripts partial is added when the UI ships none')
  assert.match(partial.contents.toString(), /_search\/search-ui\.js/)
})

test('beforePublish indexes the rendered pages and publishes the engine', { skip: !distReady && 'dist/ not built — run the gradle pack tasks first' }, async () => {
  const ctx = fakeContext()
  register.call(ctx, { config: {} })
  const catalogs = fakeCatalogs([
    fakePage(),
    fakePage({
      src: { component: 'skainet', version: '0.42', module: 'ROOT', relative: 'install.adoc' },
      pub: { url: '/skainet/0.42/install.html' },
    }),
    fakePage({ asciidoc: { attributes: { noindex: '' } } }),
  ])
  await ctx.listeners.beforePublish(catalogs)

  const paths = catalogs.siteCatalog.files.map((f) => f.out.path)
  assert.ok(paths.includes('_search/search.lrb'))
  assert.ok(paths.includes('_search/search-ui.js'))
  assert.ok(paths.includes('_search/package.json'))
  assert.ok(paths.includes('_search/skainet-search.js'))
  assert.ok(paths.includes('_search/skainet-search-core.wasm'))

  const bundle = catalogs.siteCatalog.files.find((f) => f.out.path === '_search/search.lrb')
  assert.equal(bundle.contents.subarray(0, 4).toString(), 'LRB2', 'the index is a real bundle')
  assert.ok(bundle.contents.length > 1000)
  // Only the latest component version and non-noindex pages were indexed: the bundle text
  // must mention the 0.57 URL and never the 0.42 one.
  const text = bundle.contents.toString('latin1')
  assert.ok(text.includes('/skainet/0.57/install.html'))
  assert.ok(!text.includes('/skainet/0.42/install.html'))

  // Determinism: the same catalogs index to the same bytes when the clock is pinned.
  process.env.SOURCE_DATE_EPOCH = '1764000000'
  try {
    const again = fakeCatalogs([fakePage()])
    const andAgain = fakeCatalogs([fakePage()])
    await ctx.listeners.beforePublish(again)
    await ctx.listeners.beforePublish(andAgain)
    const bytesOf = (c) => c.siteCatalog.files.find((f) => f.out.path === '_search/search.lrb').contents
    assert.ok(bytesOf(again).equals(bytesOf(andAgain)))
  } finally {
    delete process.env.SOURCE_DATE_EPOCH
  }
})
