'use strict'

/**
 * Rendered-HTML extraction: one Antora page in, the indexer's neutral section records out.
 *
 * Indexing the *rendered* page rather than the .adoc source is deliberate (the same contract
 * antora-lunr-extension established): includes, attributes and conditionals are already
 * resolved, and Asciidoctor's final heading ids — `_lower_snake`, `_2` dedup suffixes, custom
 * `[[anchors]]` — are sitting in the markup, so this module scrapes anchors and never computes
 * one. The Kotlin boundary downstream is semantic (sections, headings, text); everything
 * syntactic about HTML ends here.
 */

const parse5 = require('parse5')

const SKIP_TAGS = new Set(['script', 'style', 'nav', 'footer', 'template'])
const SKIP_CLASS_WORDS = ['toc', 'pagination', 'breadcrumbs', 'page-versions']
// Block-level containers whose whole inline text reads as one paragraph of a section.
const PARAGRAPH_TAGS = new Set(['p', 'li', 'dt', 'dd', 'td', 'th', 'caption', 'figcaption', 'h4', 'h5', 'h6'])

/**
 * @param {string} html one published page, as Antora will write it
 * @param {object} page `{ docId, title, urlPath, component, version, module }` — title is the
 *   fallback when the article carries no h1
 * @returns {object|null} a PageRecord for the indexer, or null when the page has no
 *   `<article class="doc">` (a redirect stub, a frameless page)
 */
function extractPage (html, page) {
  const article = findArticle(parse5.parse(html))
  if (!article) return null

  const sections = []
  let title = page.title
  let current = newSection(null, [])
  let lastH2 = null

  const flush = () => {
    const text = current.paragraphs.join('\n\n').trim()
    const codeText = current.code.join('\n\n').trim()
    if (text || codeText) {
      sections.push({ anchor: current.anchor, headingPath: current.headingPath, text, codeText })
    }
  }

  const visit = (node) => {
    if (node.nodeName === '#text') {
      const text = node.value.trim()
      if (text) current.paragraphs.push(text)
      return
    }
    const tag = node.tagName
    if (!tag) return (node.childNodes || []).forEach(visit)
    if (SKIP_TAGS.has(tag) || hasSkippedClass(node)) return
    if (tag === 'h1') {
      const text = inlineText(node)
      if (text) title = text
      return
    }
    if (tag === 'pre') {
      const text = inlineText(node)
      if (text) current.code.push(text)
      return
    }
    if (tag === 'h2' || tag === 'h3') {
      const anchor = attr(node, 'id')
      const heading = inlineText(node)
      if (anchor && heading) {
        flush()
        if (tag === 'h2') {
          lastH2 = heading
          current = newSection(anchor, [heading])
        } else {
          current = newSection(anchor, lastH2 ? [lastH2, heading] : [heading])
        }
        return
      }
      // An id-less heading cannot be linked to; its text stays with the current section.
    }
    if (PARAGRAPH_TAGS.has(tag)) {
      const text = inlineText(node, current.code)
      if (text) current.paragraphs.push(text)
      return
    }
    ;(node.childNodes || []).forEach(visit)
  }

  visit(article)
  flush()
  if (!sections.length) return null

  // A page can have neither a catalog title nor an h1 (seen in the wild: pages authored
  // starting at `== ` instead of a doctitle); the indexer requires one. The first section
  // heading is the de-facto title of such a page; the output filename is the last resort.
  if (!title) {
    const headed = sections.find((s) => s.headingPath.length)
    title =
      (headed && headed.headingPath[0]) ||
      (page.urlPath || '').split('/').pop().replace(/\.html$/, '') ||
      page.docId
  }

  return {
    docId: page.docId,
    title,
    urlPath: page.urlPath,
    component: page.component,
    version: page.version,
    module: page.module,
    sections,
  }
}

function newSection (anchor, headingPath) {
  return { anchor, headingPath, paragraphs: [], code: [] }
}

function findArticle (node) {
  if (node.tagName === 'article' && classesOf(node).includes('doc')) return node
  for (const child of node.childNodes || []) {
    const found = findArticle(child)
    if (found) return found
  }
  return null
}

/** Inline text of a subtree; `pre` blocks inside it divert to [codeSink] instead of the prose. */
function inlineText (node, codeSink) {
  const parts = []
  const walk = (n) => {
    if (n.nodeName === '#text') return parts.push(n.value)
    if (!n.tagName || SKIP_TAGS.has(n.tagName)) return
    if (n.tagName === 'pre' && codeSink) {
      const text = inlineText(n)
      if (text) codeSink.push(text)
      return
    }
    ;(n.childNodes || []).forEach(walk)
  }
  ;(node.childNodes || []).forEach(walk)
  return parts.join('').replace(/[ \t]*\n[ \t]*/g, '\n').replace(/[ \t]+/g, ' ').trim()
}

function classesOf (node) {
  return (attr(node, 'class') || '').split(/\s+/).filter(Boolean)
}

function hasSkippedClass (node) {
  const classes = classesOf(node)
  return SKIP_CLASS_WORDS.some((word) => classes.includes(word))
}

function attr (node, name) {
  const found = (node.attrs || []).find((it) => it.name === name)
  return found ? found.value : undefined
}

module.exports = { extractPage }
