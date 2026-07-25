# Extract References — Manual Test Files

Test setup for [JabRef issue #14085](https://github.com/JabRef/jabref/issues/14085):
extracting descriptive text about papers from "Related Work" sections in PDFs.

## Directory Structure

```text
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
3. Select `Kopp2024` paper.

### Test: Extract related work text

The **Related Work** section of `main.pdf` is the first paragraph of the
"Systematic literature reviews" section of the JabRef TUGboat paper.
It cites three references using numeric citations `[1]`, `[2]`, `[3]`
(the tugboat bibliography style sorts entries alphabetically by key):

| Citation | Key in .bib       | Paper title                                                                                  |
|----------|-------------------|----------------------------------------------------------------------------------------------|
| `[1]`    | `AlZubidy2018`    | Identification and prioritization of SLR search tool requirements: an SLR and a survey       |
| `[2]`    | `Kitchenham2007`  | Guidelines for performing systematic literature reviews in software engineering              |
| `[3]`    | `Voigt2021`       | Systematic Literature Tools: Are we there yet?                                               |

**Steps to test the "Insert related work text" feature:**

1. Open `paper1/main.pdf` in your PDF reader.
2. In the **Related Work** section, select the sentence describing paper `[2]`:

   > Systematic literature reviews (SLRs) are comprehensive examinations of existing research on a particular topic, conducted according to a well-defined and transparent methodology [2].

3. Copy the selected text to the clipboard (`Ctrl+C`).
4. In JabRef, select the entry `Kitchenham2007` in your library.
5. Open **Tools → Insert related work text**.
6. JabRef should display the clipboard content and match citation `[2]` to `Kitchenham2007`.
7. Click **Insert**.
8. Verify that the `Kitchenham2007` entry now has a `comment-{username}` field containing:

   ```markdown
   - Kitchenham2007: Systematic literature reviews (SLRs) are comprehensive examinations of existing research on a particular topic, conducted according to a well-defined and transparent methodology.
   ```

Repeat steps 2–8 for the remaining citations:

- `[1]` → `AlZubidy2018`:

  > Commonly used e-libraries in the domain of computer science research, such as IEEE, arXiv, and ACM, do not support easy bulk access, which is key to the SLR method [1].

- `[3]` → `Voigt2021`:

  > JabRef fills this gap with its special SLR feature [3].

Note: The sentence "The most difficult challenge researchers face when conducting an SLR is during the search step [1, 3]." cites two references at once and can be used to test how the feature handles multi-citation sentences.

### Reference format used

The main paper uses the **tugboat numeric** citation format: `[n]`.

To test other citation formats, additional test PDFs using author-year
(e.g., `(Kitchenham 2007)`) or combined (e.g., `[Kitch07]`) formats should be created
in separate subdirectories following the same pattern.

## Generating the PDFs

**Prerequisites:** A LaTeX distribution with the `mwe` and `tugboat` packages installed.
On most systems these are available via TeX Live (`texlive-publishers`) or MiKTeX.

### Generate the main paper

```bash
cd paper1
pdflatex main.tex
bibtex main
pdflatex main.tex
pdflatex main.tex
```

Two extra `pdflatex` runs are needed so BibTeX references and cross-references are resolved.
