'use strict'

/**
 * @skainet/antora-site-search — hybrid client-side search for Antora sites, one playbook line:
 *
 *   antora:
 *     extensions:
 *       - require: '@skainet/antora-site-search'
 *         index_all_versions: false   # latest version per component (the default)
 *         category: component         # facet key: component | module
 *         embed: true                 # false -> BM25-only bundle, smaller and still first-class
 *         dump_pages: ''              # debug: write the extraction JSON here
 *
 * `uiLoaded` activates the UI's standard search hooks (`SITE_SEARCH_PROVIDER`, the same switch
 * antora-lunr-extension throws) and supplies a default `search-scripts` partial — a partial the
 * UI bundle ships wins, so a UI like skainet-docs-ui owns the look and feel. `beforePublish`
 * extracts the rendered pages, hands them to the Kotlin/Wasm indexer in-process (Node >= 22),
 * and publishes `_search/search.lrb` plus the engine next to the site.
 */

const { promises: fsp } = require('node:fs')
const ospath = require('node:path')
const { extractPage } = require('./extract')

const PROVIDER = 'skainet'
const SEARCH_DIR = '_search'
const PARTIAL_PATH = 'partials/search-scripts.hbs'

const MEDIA_TYPES = {
  '.js': 'text/javascript',
  '.mjs': 'text/javascript',
  '.wasm': 'application/wasm',
  '.lrb': 'application/octet-stream',
}

module.exports.register = function ({ config = {} }) {
  const {
    indexAllVersions = false,
    category = 'component',
    embed = true,
    dumpPages = '',
  } = config
  const logger = (this.getLogger && this.getLogger('skainet-site-search')) || console

  this.on('uiLoaded', ({ playbook, uiCatalog }) => {
    playbook.env.SITE_SEARCH_PROVIDER = PROVIDER
    const supplied = uiCatalog
      .findByType('partial')
      .some((file) => file.path === PARTIAL_PATH)
    if (!supplied) {
      uiCatalog.addFile({
        contents: Buffer.from(defaultPartial(), 'utf8'),
        path: PARTIAL_PATH,
        stem: 'search-scripts',
        type: 'partial',
      })
    }
  })

  this.on('beforePublish', async ({ playbook, contentCatalog, siteCatalog }) => {
    const pages = collectPages(contentCatalog, { indexAllVersions })
    if (dumpPages) {
      const file = ospath.resolve(playbook.dir || '.', dumpPages)
      await fsp.mkdir(ospath.dirname(file), { recursive: true })
      await fsp.writeFile(file, JSON.stringify(pages, null, 2))
      logger.info(`dumped ${pages.length} page records to ${file}`)
    }

    const { indexSite } = await import(distUrl('indexer/skainet-search-indexer.mjs'))
    const result = JSON.parse(
      indexSite(
        JSON.stringify(pages),
        JSON.stringify({
          contentVersion: contentVersionFrom(playbook),
          builtAt: process.env.SOURCE_DATE_EPOCH
            ? new Date(Number(process.env.SOURCE_DATE_EPOCH) * 1000).toISOString()
            : new Date().toISOString(),
          embed,
          categoryFrom: category === 'module' ? 'MODULE' : 'COMPONENT',
        }),
      ),
    )
    if (!result.ok) throw new Error(`skainet-site-search indexing failed: ${result.error}`)
    for (const warning of result.warnings) logger.warn(warning)

    siteCatalog.addFile({
      mediaType: MEDIA_TYPES['.lrb'],
      contents: Buffer.from(result.bundleBase64, 'base64'),
      out: { path: `${SEARCH_DIR}/search.lrb` },
    })
    for (const asset of await engineAssets()) siteCatalog.addFile(asset)
    logger.info(
      `indexed ${result.chunkCount} sections from ${result.pageCount} pages into ${SEARCH_DIR}/search.lrb`,
    )
  })
}

function collectPages (contentCatalog, { indexAllVersions }) {
  const latest = new Set()
  for (const component of contentCatalog.getComponents()) {
    if (component.latest) latest.add(`${component.name}@${component.latest.version}`)
  }
  const records = []
  for (const page of contentCatalog.getPages((it) => it.out)) {
    const attributes = (page.asciidoc && page.asciidoc.attributes) || {}
    if ('noindex' in attributes || 'page-noindex' in attributes) continue
    const { component, version, module: module_, relative } = page.src
    if (!indexAllVersions && !latest.has(`${component}@${version}`)) continue
    const record = extractPage(page.contents.toString(), {
      docId: `${component}:${module_}:${relative}`,
      title: page.title,
      urlPath: page.pub.url,
      component,
      version,
      module: module_,
    })
    if (record) records.push(record)
  }
  return records
}

/** A monotonic content stamp: Antora has no corpus version, so the day of the build serves. */
function contentVersionFrom (playbook) {
  const date = process.env.SOURCE_DATE_EPOCH
    ? new Date(Number(process.env.SOURCE_DATE_EPOCH) * 1000)
    : new Date()
  return Number(date.toISOString().slice(0, 10).replaceAll('-', ''))
}

async function engineAssets () {
  const dir = ospath.join(__dirname, '..', 'dist', 'engine')
  let names
  try {
    names = await fsp.readdir(dir)
  } catch {
    throw new Error(
      `the engine distribution is missing from ${dir} — in a repo checkout, run ` +
        '`./gradlew :site-search:search-core:packEngineNpm :site-search:indexer:packNpm` first',
    )
  }
  const assets = [
    {
      mediaType: MEDIA_TYPES['.js'],
      contents: await fsp.readFile(ospath.join(__dirname, 'assets', 'search-ui.js')),
      out: { path: `${SEARCH_DIR}/search-ui.js` },
    },
    {
      // Browsers get the module format from the script tag, but a Node consumer of the
      // published site (smoke test, determinism canary) resolves .js by the nearest
      // package.json — without this marker the ES-module engine parses as CommonJS.
      mediaType: 'application/json',
      contents: Buffer.from('{"type":"module"}\n'),
      out: { path: `${SEARCH_DIR}/package.json` },
    },
  ]
  for (const name of names) {
    assets.push({
      mediaType: MEDIA_TYPES[ospath.extname(name)] || 'application/octet-stream',
      contents: await fsp.readFile(ospath.join(dir, name)),
      out: { path: `${SEARCH_DIR}/${name}` },
    })
  }
  return assets
}

function distUrl (relative) {
  return new URL(`../dist/${relative}`, require('node:url').pathToFileURL(__filename)).href
}

/**
 * The fallback wiring for UIs that ship no `search-scripts.hbs` of their own. Dataset names
 * follow the convention antora-lunr-extension established (`#search-ui-script`,
 * `data-site-root-path`, `data-snippet-length`) so existing overrides port, plus this engine's
 * `data-engine-path`/`data-bundle-path`.
 */
function defaultPartial () {
  return `<script id="search-ui-script" type="module" src="{{{siteRootPath}}}/${SEARCH_DIR}/search-ui.js"
  data-site-root-path="{{{siteRootPath}}}"
  data-engine-path="{{{siteRootPath}}}/${SEARCH_DIR}"
  data-bundle-path="{{{siteRootPath}}}/${SEARCH_DIR}/search.lrb"
  data-snippet-length="100"{{#if page.component}}
  data-page-component="{{page.component.name}}"{{/if}}></script>
`
}
