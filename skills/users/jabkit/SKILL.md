---
name: jabkit
category: users
description: JabRef's Swiss Army knife CLI for BibTeX/biblatex - fetches entries online, converts DOIs to BibTeX, turns PDF papers into BibTeX entries, checks libraries, generates citation keys, writes XMP metadata, and searches .bib files.
license: MIT
---

# jabkit — JabRef's CLI Toolkit

`jabkit` exposes JabRef's BibTeX/biblatex handling as a command-line application.

## Installation

Preferred: the native binary — a single self-contained executable, no JDK or JBang needed, instant startup. Download and unpack for your platform:

<!-- Features that do not use AWT have been verified to work without the bundled .so files. -->

```bash
# Linux (amd64)
curl -fL https://builds.jabref.org/main/linux-amd64/tools/jabkit-native_linux.tar.gz | tar xz
./jabkit/jabkit --help
```

```bash
# Linux (arm64)
curl -fL https://builds.jabref.org/main/linux-arm/tools/jabkit-native_linux_arm64.tar.gz | tar xz
./jabkit/jabkit --help
```

```bash
# macOS (Apple Silicon)
curl -fLO https://builds.jabref.org/main/macOS-silicon/tools/jabkit-native_macos-silicon.zip
unzip jabkit-native_macos-silicon.zip
.jabkit/jabkit --help
```

On other platforms (e.g. Windows, Intel macOS), run `jabkit` via [JBang](https://www.jbang.dev/) instead — no JDK setup needed. Install JBang if missing:

```bash
curl -Ls https://sh.jbang.dev | bash -s - app setup   # Linux/macOS
```

```powershell
iex "& { $(iwr -useb https://ps.jbang.dev) } app setup"   # Windows
```

On linux-x64, mac-aarch64 or windows-x64, setting the environment variable `JBANG_USE_NATIVE` to `true` before the command above installs JBang's native binary instead of its JAR: one JDK download less and a faster start.

```bash
export JBANG_USE_NATIVE=true          # Bash/zsh, persist in ~/.bashrc or ~/.zshrc
```

```powershell
$env:JBANG_USE_NATIVE = 'true'        # PowerShell, persist in $PROFILE
```

Persist it rather than setting it only for the install — the launcher re-reads it on every run and falls back to the JAR when it is unset.
Other platforms have no native build and the install would fail, so leave it unset there.

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
```

## Notes for agents

- Always use `-p`/`--porcelain` when parsing output programmatically.
- `fetch --provider` matches JabRef's web-search fetcher names case-insensitively; on an unknown name, jabkit reports `Could not find fetcher`.
- URLs are accepted as input: `jabkit convert --input https://example.org/refs.ris --input-format ris`.
