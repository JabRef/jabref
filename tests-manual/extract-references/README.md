# Extract References — Manual Test Files

Test setup for [JabRef issue #14085](https://github.com/JabRef/jabref/issues/14085):
extracting descriptive text about papers from "Related Work" sections in PDFs.

## Directory Structure

```
extract-references/
├── paper1/
│   ├── main.pdf        — PDF of main paper
│   ├── main.tex        — Main paper with a "Related Work" section
│   └── references.bib  — BibTeX entries for the three referenced papers
```

## How to Test (Issue #14085)

### Setup

1. Open JabRef.
2. Open `extract-references.bib`.
3. Select `Doe2024` paper.

### Test: Extract related work text

The **Related Work** section of `main.pdf` contains one sentence per referenced paper,
using IEEE numeric citations `[1]`, `[2]`, `[3]`:

| Citation | Key in .bib  | Paper title                                                |
|----------|--------------|------------------------------------------------------------|
| `[1]`    | `Smith2021`  | Sustainable Agriculture Methods for Tropical Regions       |
| `[2]`    | `Garcia2022` | Machine Learning Applications in Climate Change Monitoring |
| `[3]`    | `Chen2023`   | Renewable Energy Systems: A Comprehensive Survey           |

**Steps to test the "Insert related work text" feature:**

1. Open `paper1/main.pdf` in your PDF reader.
2. In the **Related Work** section, select the sentence describing paper `[1]`:

   > Smith and Brown demonstrated that organic farming practices in tropical regions can increase yield by up to 30% while reducing chemical input by 50% [1].

3. Copy the selected text to the clipboard (`Ctrl+C`).
4. In JabRef, select the entry `Smith2021` in your library.
5. Open **Tools → Insert related work text**.
6. JabRef should display the clipboard content and match citation `[1]` to `Smith2021`.
7. Click **Insert**.
8. Verify that the `Smith2021` entry now has a `comment-{username}` field containing:

   ```markdown
   - Smith2021: Smith and Brown demonstrated that organic farming practices in tropical regions can increase yield by up to 30% while reducing chemical input by 50%.
   ```

Repeat steps 2–8 for citations `[2]` (entry `Garcia2022`) and `[3]` (entry `Chen2023`).

### Reference format used

The main paper uses the **IEEE numeric** citation format: `[n]`.

To test other citation formats, additional test PDFs using author-year
(e.g., `(Smith 2021)`) or combined (e.g., `[Smith21]`) formats should be created
in separate subdirectories following the same pattern.

## Generating the PDFs

**Prerequisites:** A LaTeX distribution with the `mwe` and `IEEEtran` packages installed.
On most systems these are available via TeX Live (`texlive-publishers`, `texlive-science`)
or MiKTeX.

### Generate the main paper

```bash
cd main-paper
pdflatex main.tex
bibtex main
pdflatex main.tex
pdflatex main.tex
```

Two extra `pdflatex` runs are needed so BibTeX references and cross-references are resolved.
