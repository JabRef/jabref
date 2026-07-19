---
name: jabkit
category: users
description: JabRef's Swiss Army knife CLI for BibTeX/biblatex - fetches entries online, converts DOIs to BibTeX, extracts references from PDFs, checks libraries, generates citation keys, writes XMP metadata, and searches .bib files.
license: MIT
---

# jabkit — JabRef's CLI Toolkit

`jabkit` exposes JabRef's BibTeX/biblatex handling as a command-line application.

## Installation

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

## Global flags

Place before the subcommand:

- `-p`, `--porcelain` — script-friendly output (no banners/progress)
- `-d`, `--debug` — debug logging
- `-v`, `--version` — version info

## Commands

| Command | Purpose |
|---|---|
| `check [FILE]` | Run consistency and integrity checks on a library; subcommands `consistency` and `integrity` run one of them |
| `citationkeys generate` | Generate citation keys for entries in a `.bib` file |
| `convert` | Convert between bibliography formats (including PDF → BibTeX) |
| `doi-to-bibtex DOI...` | Convert one or more DOIs to BibTeX entries |
| `fetch` | Query an online provider (ArXiv, Crossref, ...) and output matching entries |
| `generate-bib-from-aux` | Extract the subset of a library cited in a LaTeX `.aux` file |
| `get-cited-works DOI` | List the works cited by a publication |
| `get-citing-works DOI` | List the works citing a publication |
| `pdf update` | Write XMP metadata and/or embedded BibTeX into linked PDFs |
| `preferences reset\|import\|export` | Manage jabkit preferences |
| `pseudonymize` | Replace identifying data in a library (writes a key file for reversal) |
| `search` | Search in a library using JabRef's search syntax |
| `shorten FILE.tex` | Compile a LaTeX paper with `latexmk` and shorten its cited references (authors → first author, journal abbreviations, DOI cleanup) until it fits a target page count |

Input files are passed positionally or via `--input`; both forms are equivalent. `--input-format "*"` auto-detects the input format.

## Examples

```bash
# DOI to BibTeX
jabkit doi-to-bibtex 10.1145/3149935.3149942

# Fetch from an online catalogue into a file
jabkit fetch --provider ArXiv --query "test driven development" --output tdd.bib

# PDF paper to BibTeX entry (see the pdf-to-bibtex skill for details)
jabkit convert --input paper.pdf --input-format pdfMerged --output paper.bib

# Validate in CI (errorformat output, non-zero exit on findings)
jabkit -p check library.bib --output-format github-actions

# Citation keys with a custom pattern
jabkit citationkeys generate library.bib --pattern "[auth][year]" --output library.bib

# Write BibTeX + XMP metadata into the PDFs linked from an entry
jabkit pdf update --citation-key Smith2020 --input library.bib --input-format bibtex

# Library subset actually cited in a LaTeX document
jabkit generate-bib-from-aux --aux paper.aux --input full-library.bib --output paper.bib

# Shorten a paper's references until it fits 6 pages (rewrites the referenced .bib in place)
jabkit shorten paper.tex --pages 6
```

## Notes for agents

- Always use `-p`/`--porcelain` when parsing output programmatically.
- `fetch --provider` matches JabRef's web-search fetcher names case-insensitively; on an unknown name, jabkit reports `Could not find fetcher`.
- URLs are accepted as input: `jabkit convert --input https://example.org/refs.ris --input-format ris`.
- `shorten` needs a local `latexmk` (any TeX distribution); if none is found it falls back to the `texlive/texlive` Docker image. It applies only the cleanups needed to hit `--pages` (default: one page fewer), rewrites the referenced `.bib` in place, and warns on stderr if the target can't be reached. Use `--output FILE` to redirect the shortened `.bib` instead of overwriting.
