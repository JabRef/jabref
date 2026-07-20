---
name: pdf-to-bibtex
category: users
description: Turns PDF papers into BibTeX entries with JabRef's jabkit CLI, pulling metadata from XMP, embedded BibTeX, GROBID, or text heuristics - builds a bibliography from a whole folder of papers.
license: MIT
---

# PDF to BibTeX

Extract bibliographic data from PDF files and produce BibTeX entries using `jabkit`, JabRef's command-line toolkit.

## Setup

`jabkit` runs via [JBang](https://www.jbang.dev/) — no JDK setup needed. Install JBang if missing:

```bash
curl -Ls https://sh.jbang.dev | bash -s - app setup   # Linux/macOS
```

```powershell
iex "& { $(iwr -useb https://ps.jbang.dev) } app setup"   # Windows
```

Then install `jabkit` on the PATH (the same command also updates it):

```bash
jbang app install --fresh --force jabkit@jabref
```

One-off run without installation:

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

- Extract the works a paper *cites* (its "References" section): the `pdf-extract-references` skill.
- Write metadata back into PDFs (reverse direction): `jabkit pdf update --citation-key <key> --input library.bib --input-format bibtex`
- Full CLI reference: the `jabkit` skill in this repository.
