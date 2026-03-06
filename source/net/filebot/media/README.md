# Embedded data files

This folder contains source code and embedded data resources.

## Upstream data source

Data files are vendored from:

- Repository: https://github.com/filebot/data
- License: CC0-1.0 (see `LICENSE` in this folder)

Vendored files:

- `add-series-alias.txt`
- `media-sources.txt`
- `query-excludes.txt`
- `release-groups.txt`
- `series-mappings.txt`
- `build.groovy`
- `makefile`
- `LICENSE`

## Build normalization

Upstream `build.groovy` normalization rules are applied to `.txt` files before embedding:

- trim lines
- remove empty lines
- normalize each line via `java.util.regex.Pattern.compile(it).pattern()`
- case-insensitive sort
- case-insensitive deduplication

## Compatibility notes

- `query-blacklist.txt` is kept as a compatibility alias for existing runtime configuration and is generated from `query-excludes.txt`.
- `thetvdb.txt` remains a separate project-local resource.
