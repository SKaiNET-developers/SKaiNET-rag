/**
 * End-to-end smoke test of the packed engine, no browser required: serves a bundle over
 * loopback HTTP (the engine loads its corpus with `fetch`), drives the public API from
 * `skainet-search.js`, and asserts a linkful hit comes back. Needs Node >= 22 (WasmGC).
 *
 *   node smoke/smoke.mjs build/dist/engine ../../samples/app/src/commonMain/composeResources/files/docs.lrb
 */
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';

const [engineDir, bundleFile] = process.argv.slice(2);
if (!engineDir || !bundleFile) {
  console.error('usage: node smoke.mjs <engine-dist-dir> <bundle.lrb>');
  process.exit(2);
}

const bytes = await readFile(bundleFile);
const server = createServer((_req, res) => {
  res.setHeader('content-type', 'application/octet-stream');
  res.end(bytes);
}).listen(0, '127.0.0.1');
await new Promise((resolve) => server.once('listening', resolve));
const bundleUrl = `http://127.0.0.1:${server.address().port}/docs.lrb`;

try {
  const { skainetSearch } = await import(pathToFileURL(`${engineDir}/skainet-search.js`));
  const handle = await skainetSearch.init({ bundleUrl });
  console.log('ready:', JSON.stringify(handle.info));
  if (!handle.info.chunkCount) throw new Error('engine reported an empty corpus');

  const hits = await handle.search('which JEP introduced virtual threads');
  if (!hits.length) throw new Error('no hits for the canonical query');
  const top = hits[0];
  console.log(
    'top hit:',
    JSON.stringify({ url: top.url, anchor: top.anchor, title: top.title, source: top.source }),
  );
  if (!top.url) throw new Error('top hit carries no url');

  const filtered = await handle.search('virtual threads', { filter: { category: 'no-such-facet' } });
  if (filtered.length) throw new Error('category filter let hits through');

  handle.release();
  console.log('smoke OK');
} finally {
  server.close();
}
