---
name: pdf-extract-references
category: users
description: Extracts the works a paper cites - parses the "References" section of PDF papers with JabRef's jabkit CLI and outputs the cited entries as BibTeX, via offline rule-based parsing (IEEE-style numbered references), a GROBID service, or an experimental LLM mode.
license: MIT
---

# Extract references from a PDF

Turn the "References" section of a PDF paper into BibTeX entries — one entry per cited work — using `jabkit pdf extract-references`.

This skill is about the works a paper *cites*. For a BibTeX entry describing the paper *itself* (its own title, authors, year), use the `pdf-to-bibtex` skill instead.

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

## Basic usage

```bash
jabkit -p pdf extract-references paper.pdf                      # single PDF → BibTeX on stdout
jabkit pdf extract-references paper.pdf --output refs.bib       # single PDF → file
jabkit pdf extract-references a.pdf b.pdf --output-dir refs/    # one .bib per PDF into refs/
jabkit pdf extract-references a.pdf b.pdf                       # writes a.bib, b.bib next to each PDF
```

`-p`/`--porcelain` suppresses progress messages — use it whenever parsing the output. `--output` is only valid with a single input file; use `--output-dir` for several.

## Extraction modes

Select with `--mode` (case-insensitive):

| Mode | Strategy |
|---|---|
| `RULE_BASED` | Offline parsing of numbered `[1] ...` references (IEEE-style two-column papers). Default when no GROBID is configured. |
| `GROBID` | Sends the PDF to a [GROBID](https://github.com/kermitt2/grobid) service. Default if GROBID is enabled in preferences; `--grobid-url <url>` overrides the server for this call. |
| `LLM` | Experimental — uses the AI provider configured in preferences. |

In `RULE_BASED` mode each entry keeps the original reference string in its `comment` field, so the parse can be double-checked against the source.

## Notes

- The exit code is non-zero if any input PDF was missing or failed to parse; remaining files are still processed.
- Reference parsing is heuristic. Entries with a `doi` field can be cross-checked by fetching clean metadata: `jabkit doi-to-bibtex <doi>`.

## Related

- BibTeX entry for the paper itself: the `pdf-to-bibtex` skill (`jabkit convert --input paper.pdf --input-format pdfMerged`).
- Cited works looked up online by DOI instead of PDF parsing: `jabkit get-cited-works <doi>`.
- Full CLI reference: the `jabkit` skill in this repository.
