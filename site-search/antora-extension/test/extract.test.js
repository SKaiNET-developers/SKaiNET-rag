'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const ospath = require('node:path')
const { extractPage } = require('../lib/extract')

const html = readFileSync(ospath.join(__dirname, 'fixtures', 'install.html'), 'utf8')

const page = {
  docId: 'skainet:ROOT:install.adoc',
  title: 'fallback title',
  urlPath: '/skainet/0.57/install.html',
  component: 'skainet',
  version: '0.57',
  module: 'ROOT',
}

test('an antora page extracts to anchored section records', () => {
  const record = extractPage(html, page)

  assert.equal(record.title, 'Install SKaiNET', 'the h1 wins over the fallback title')
  assert.equal(record.urlPath, '/skainet/0.57/install.html')
  assert.deepEqual(
    record.sections.map((s) => s.anchor),
    [null, '_prerequisites', '_ios_targets', '_verify'],
    'preamble first, then one record per h2/h3, anchors scraped verbatim',
  )

  const [preamble, prerequisites, ios, verify] = record.sections
  assert.match(preamble.text, /Maven Central/)
  assert.deepEqual(preamble.headingPath, [])

  assert.deepEqual(prerequisites.headingPath, ['Prerequisites'])
  assert.match(prerequisites.text, /JDK 21 toolchain and Gradle/, 'inline markup flattens to text')
  assert.match(prerequisites.text, /macOS and Linux are tier one/, 'list items are paragraphs')
  assert.equal(prerequisites.codeText, 'implementation(platform("sk.ainet:skainet-bom:0.57.0"))')
  assert.ok(!prerequisites.text.includes('skainet-bom'), 'code leaves the prose')
  assert.ok(!prerequisites.text.includes('Xcode'), 'the h3 body is not double-indexed into its h2')

  assert.deepEqual(ios.headingPath, ['Prerequisites', 'iOS targets'], 'the h3 carries its h2 trail')
  assert.match(ios.text, /Xcode/)

  assert.match(verify.text, /smoke test/)
  assert.ok(!verify.text.includes('Your first model'), 'pagination is skipped')
  assert.ok(!verify.text.includes('Contents'), 'the toc is skipped')
  assert.ok(!record.sections.some((s) => s.text.includes('Home')), 'breadcrumbs are skipped')
})

test('a page without an article yields null', () => {
  assert.equal(extractPage('<html><body><p>redirecting…</p></body></html>', page), null)
})
