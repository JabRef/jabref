---
name: pdf-to-bibtex
description: Extract BibTeX entries from PDF papers using JabRef's jabkit CLI. Use when the user wants to import academic papers (PDFs) into a BibTeX or biblatex library, generate .bib entries from PDF metadata (XMP, embedded BibTeX, GROBID, text heuristics), or turn a folder of papers into a bibliography.
license: MIT
---

# PDF to BibTeX

Extract bibliographic data from PDF files and produce BibTeX entries using `jabkit`, JabRef's command-line toolkit.

## Setup

`jabkit` runs via [JBang](https://www.jbang.dev/download/) — no JDK setup needed:

```bash
jbang app install --fresh --force jabkit@jabref   # install permanently as `jabkit`
```

Or run once without installing:

```bash
jbang --fresh jabkit@jabref --help
```

## Basic usage

```bash
jabkit convert --input paper.pdf --input-format pdfMerged --output paper.bib
```

Omit `--output` to print BibTeX to stdout. Add `--porcelain` before the subcommand for script-friendly output:

```bash
jabkit -p convert --input paper.pdf --input-format pdfMerged
```

## PDF importer formats

Pass one of these as `--input-format`:

| Format id | Strategy |
|---|---|
| `pdfMerged` | Merges results of the other importers into one best-effort entry — **use this by default** |
| `pdfXmp` | Reads XMP metadata stored in the PDF |
| `pdfEmbeddedBibFile` | Reads a BibTeX file embedded as PDF attachment |
| `pdfVerbatimBibtex` | Parses BibTeX printed verbatim on the first page |
| `pdfContent` | Heuristic extraction from the text of the first page |
| `pdfGrobid` | Sends the PDF to a [GROBID](https://github.com/kermitt2/grobid) service (requires GROBID to be enabled/reachable) |
| `pdfBibiliography` | Rule-based parsing of the bibliography section |

`--input-format "*"` auto-detects the format (also works for non-PDF inputs).

## Recommended workflow

1. Run with `pdfMerged` first.
2. Check the result for a `doi` field. If a DOI is present but fields look incomplete, fetch clean metadata instead:

   ```bash
   jabkit doi-to-bibtex 10.1145/3149935.3149942
   ```

3. Validate the resulting library:

   ```bash
   jabkit check paper.bib
   ```

## Batch conversion

```bash
for f in papers/*.pdf; do
  jabkit -p convert --input "$f" --input-format pdfMerged >> library.bib
done
```

Afterwards generate citation keys for all entries:

```bash
jabkit citationkeys generate library.bib --output library.bib
```

## Related

- Write metadata back into PDFs (reverse direction): `jabkit pdf update --citation-key <key> --input library.bib --input-format bibtex`
- Full CLI reference: the `jabkit` skill in this repository.
