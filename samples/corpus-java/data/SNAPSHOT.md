# Vendored Java Version Almanac data

The JSON files in this directory are a local snapshot of the Java Version Almanac data set
(<https://javaalmanac.io>, API at <https://data.javaalmanac.io>), by Marc R. Hoffmann and
Cay S. Horstmann, licensed **CC BY-SA 4.0**.

- Source: `https://github.com/marchof/java-almanac`, `data/jdk/versions/{8,11,17,21,25}.json`
  and `data/jdk/featurecategories.json`, branch `master`
- Snapshot date: **2026-10-10**
- Scope: LTS releases only (8, 11, 17, 21, 25)

Only these raw JSON files are Almanac content. The Markdown documents the generator produces
from them contain Almanac *facts* (titles, JEP/JSR numbers, dates, class-file versions) wrapped
in template sentences written in this repository; the prose articles on javaalmanac.io
(`content/features/*.md`, individually copyrighted) are deliberately **not** used.

To refresh: re-download the files above, update the snapshot date, and re-run

```bash
./gradlew :samples:corpus-java:run --args="--data $PWD/samples/corpus-java/data --out $PWD/samples/docs-java --last-reviewed <snapshot date>"
```
