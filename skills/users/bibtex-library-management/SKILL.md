---
name: bibtex-library-management
description: Check, clean, and maintain BibTeX and biblatex libraries with JabRef's jabkit CLI. Use when the user wants to validate a .bib file (integrity and consistency checks), generate citation keys, search a bibliography, convert between bibliography formats, or pseudonymize a library before sharing.
license: MIT
---

# BibTeX Library Management

Maintain BibTeX/biblatex libraries (`.bib` files) from the command line using `jabkit`, JabRef's CLI toolkit.

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

Global flags (place before the subcommand): `-p`/`--porcelain` for script-friendly output, `-d`/`--debug` for verbose logging.

## Validate a library

Run both consistency and integrity checks:

```bash
jabkit check library.bib
```

Or individually:

```bash
jabkit check consistency library.bib --output-format txt
jabkit check integrity library.bib --output-format txt
```

`--output-format` accepts `csv`, `errorformat` (default; machine-readable, suitable for editors), `github-actions`, or `txt`. Exit code signals whether problems were found — use it in CI.

## Generate citation keys

```bash
jabkit citationkeys generate library.bib --output library.bib
```

Useful options:

- `--pattern "[auth][year]"` — override the citation key pattern
- `--avoid-overwrite` — keep existing keys
- `--suffix SECOND_WITH_A` — duplicate-key suffix strategy (`ALWAYS`, `SECOND_WITH_A`, `SECOND_WITH_B`)
- `--transliterate` — transliterate non-Latin fields before key generation

## Search a library

```bash
jabkit search --query "author=smith" --input library.bib --output results.bib
```

The query uses JabRef's search syntax (field=value pairs, boolean operators). Default output format is `bibtex`.

## Convert between formats

```bash
jabkit convert --input library.bib --output library.html --output-format html
```

`--output-format` defaults to `bibtex`. Use `--input-format "*"` to auto-detect the input. Apply field formatters during conversion with `--field-formatters`.

## Pseudonymize before sharing

Replace identifying data for bug reports or datasets; writes a key file for de-pseudonymization:

```bash
jabkit pseudonymize library.bib --output library.pseudo.bib --key library.pseudo.keys.csv
```

## Look up references

```bash
jabkit doi-to-bibtex 10.1145/3149935.3149942          # DOI → BibTeX entry
jabkit fetch --provider ArXiv --query "quantum computing" --output results.bib
jabkit get-cited-works 10.1145/3149935.3149942         # bibliography of a work
jabkit get-citing-works 10.1145/3149935.3149942        # works citing it
```

## Related

- Extracting entries from PDF papers: the `pdf-to-bibtex` skill in this repository.
- Full CLI reference: the `jabkit` skill in this repository.
