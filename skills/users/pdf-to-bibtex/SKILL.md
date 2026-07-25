---
name: pdf-to-bibtex
category: users
description: Turns a PDF paper into a BibTeX entry describing that paper, using JabRef's jabkit CLI - merges XMP, embedded BibTeX, GROBID, and text heuristics, then enriches the result via DOI/arXiv/ISBN lookup; builds a bibliography from a whole folder of papers. Not for extracting the reference list cited by a paper.
license: MIT
---

# PDF to BibTeX

Produce a BibTeX entry describing a PDF paper (its own title, authors, year, ...) using `jabkit`, JabRef's command-line toolkit.

This skill is about metadata *of* the PDF. Extracting the entries a paper *cites* (its "References" section) is a different task, not covered by this skill.

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

`--input-format "*"` auto-detects the format (also works for non-PDF inputs).

## Recommended workflow

1. Run with `pdfMerged` first. Besides merging the other importers' results, it looks up any DOI, arXiv eprint, or ISBN found in the PDF online and merges the fetched metadata into the entry — no separate DOI lookup is needed.
2. Validate the resulting library:

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
