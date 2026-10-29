---
nav_order: 78
parent: Decision Records
status: proposed
---
# Split Multi-Paper PDFs by Their Bookmark Outline
`adr~split-multipaper-pdfs-by-outline~1`

Needs: impl

## Context and Problem Statement
Conference proceedings are often distributed as one large PDF containing the papers of a whole conference. In a reference manager the user wants one entry per paper, each containing its own PDF and correct page numbers and linked to the proceedings entry.

Taking on this task manually by hand is slow and tedious. Hence, the program has to find out where each paper begins and ends inside a single PDF, and it has to do so without damaging anything when the PDF is not a proceedings volume.

3 sub-problems follow:

1. Where does each paper start? A PDF has no concept of what a 'paper' is. The only structure the file itself provides is its outline (bookmarks).
2. Is the outline trustworthy for this purpose? A single paper's outline (Introduction, Methods, References, ...) can have the same shape as a volume's outline, with sections where a volume has papers. Splitting a single paper into "papers" named after its sections would be a harmful false positive.
3. Page numbering. Printed page numbers differ from PDF page indices because of covers, prefaces and roman-numbered front matter. Opening an entry 'at its page' and writing correct pages fields for new entries both depend on a consistent mapping.

Users want one entry per paper, each pointing back to the proceedings entry, and a PDF for each paper (<https://github.com/JabRef/jabref/issues/8128>)

## Decision Drivers

* No new dependency: PDFBox is already used by JabRef
* Works offline and deterministically, with no external service and no trained model
* Avoid wrong splits: one long paper with sections must not be taken for a volume of papers
* The original PDF must stay untouched
* Printed page numbers and PDF page numbers differ because of front matter
* Prefer doing nothing over splitting wrongly (a false negative is cheap, a false positive litters the library with bogus entries and files)
* Use only what the file itself states: the outline is the only place where it says where each paper starts
* Handle real-world messy outlines: an extra root bookmark around the whole volume, front/back matter, duplicate bookmarks on one page, and damaged outlines with cycles
* Keep one consistent, simple model for printed vs PDF page numbers that works both for the original volume and for the extracted papers

## Considered Options

1. Trust the outline. Treat every top-level bookmark of the outline as a paper without further checks
2. Use the outline, then validate it with content-based checks
3. Ignore the outline; detect paper starts from page layout/text only
4. Let the user type the page ranges manually for each paper
5. Send the PDF to GROBID (online service already integrated in JabRef)

## Decision Outcome

Chosen option 2 'Use the outline and validate it' because the outline is the only author-provided structure. Moreover, cheap content checks remove the main false-positive case (a single paper) without needing any model or network access.

For each paper, a new PDF with its pages is written and the original is kept.
The paper entry has a type that follows the volume (`InProceedings` for a proceedings, `InCollection` for a collection, `InBook` for a book), a title, a `pages` field, a `crossref` to the citation key of the volume entry, and the new PDF as linked file.
The `pageoffset` field of the entry that owns the PDF is added to the printed page number to get the PDF page. If the volume entry has none, the offset is read from the page labels of the PDF, and the volume entry then stores it.
Proceedings, collections and books are split, as are their multi-volume types (`MvProceedings`, `MvCollection`, `MvBook`).

### Finding the papers

1. **Find the paper level of the outline.** Starting at the root, it descends while the level has a single bookmark with children (a wrapper around the whole volume or a part). The bookmarks on the resulting level are the paper candidates; their children are the paper's *sections*. Bookmarks with no resolvable page are skipped, blank titles become `Untitled`, and already-visited bookmarks stop traversal (guards against damaged, cyclic outlines).
2. **Normalise.** Sort by page; keep one bookmark per page, preferring one that has sections.
3. **Handle front and back matter.** If any candidate has sections, candidates without sections are front or back matter and are not papers.

A PDF without a usable outline is left unsplit.

### Checking the result

A single paper's outline (Introduction, Methods, ...) looks like a volume's outline with sections in place of papers. The outline is only trusted when:

* there are at least two papers,
* most papers start on a title page, and
* if the papers have sections, most papers share at least one section name.

"Most" means at least two and at least half of the papers. Section names are compared lower-case with leading numbering removed (`2.1 Introduction` and `Introduction` match), counted once per paper.

A page starts a paper when at least 15 characters in the top 35% of the page are set at least 1.35 times larger than the body text. The body size is the most common font size on the pages where the papers start. A PDF with no extractable text never starts a paper, so it is never split.

These thresholds are heuristics and can miss unusual layouts.

### Generated entries

| Field | Value |
| --- | --- |
| `title` | bookmark title |
| `pages` | printed range, `first--last`, or a single page |
| `file` | link to the extracted PDF (type PDF, description = title) |
| `crossref` | the parent's citation key, only if the parent has one |
| `pageoffset` | `1 - printedFirst`, so that printed page `printedFirst` is page 1 of the paper's own PDF |

Authors, abstract, and other metadata are **not** extracted. Papers whose first printed page would be below 1 (for example parts before printed page 1, given the parent's offset) are skipped.

### Importing a multi-paper PDF

A PDF can also be imported instead of attached to an entry. `MultiPaperPdfImporter` finds the papers in the same way and creates one entry for the whole PDF, which holds the file, and one entry per paper that cross-references it. The PDF is then split among the papers as when it is attached.

The file does not say what kind of volume it is, so the type of the entry for the whole PDF is guessed from the front matter. It is a proceedings when the title pages or the bookmarks name the event or its committees, a collection when they name editors or contributors or the papers give author e-mail addresses, and a book otherwise.

In a BibTeX library, a collection becomes a book, the entry for the whole PDF repeats its title as `booktitle`, and a paper in a book is an `InCollection`.

With GROBID enabled, the import also asks the GROBID service for the cited references.

### Page numbering

```text
PDF page = first printed page in the entry's own `pages` field + pageoffset of the entry that owns the PDF
```

* `pageoffset` (field name `pageoffset`) is **the number of PDF pages before printed page 1**. Missing = `0`.
* It lives on the entry that **owns the PDF**: the proceedings entry for papers that cross-reference it, or the paper itself for papers extracted by the splitter.
* When a page range is extracted for an entry that has no `pageoffset` of its own, the offset of the entry it cross-references is used, and the entry takes it over.
* The first page is read from the start of `pages` (optional letter prefix allowed, for example `A12`). Only arabic numbers are mapped.
* The page is empty, but the PDF is still returned, when `pages` is missing, starts with a roman numeral, or the computed page is below 1.

Proceedings PDF: 6 PDF pages precede printed page 1, so `pageoffset = 6` on the proceedings entry. A paper with `crossref` to it and `pages = {45--52}`:

* page 45 is at PDF page 51, so `resolve` opens the proceedings PDF at 51;
* `pdfPageSpan` returns `PageSpan(51, 58)`.

After splitting, the extracted paper owns an 8-page PDF, printed pages 45-52, so it gets `pageoffset = 1 - 45 = -44`, and printed page 45 maps to PDF page 1.

### Consequences

* Good, because no new dependencies or external services beyond PDFBox are needed.
* Good, because the original PDF is never changed, extraction writes new files.
* Good, because a single paper with sections is not split into false papers.
* Good, because one offset model serves both the volume and the extracted papers, and keeps links usable after splitting.
* Bad, because a PDF without bookmarks cannot be split automatically.
* Bad, because a volume that fails the title or section check is not split, even if its outline is correct.
* Bad, because the outline must have one bookmark per paper. A volume bookmarked per session, with the papers nested below, is usually not split.
* Bad, possible false positives remain. For example, a book-like PDF whose chapters have large headings and repeated section names ("Summary").
* Bad, because the text stripper reads the paper start pages and the front matter, so the time grows with the number of papers.
* Bad, heuristic thresholds can miss unusual layouts (small titles, titles not near the top) or accept unusual books.

### Confirmation

* `./gradlew traceRequirements` fails if `PdfOutlineSplitter` is missing the marker `// [impl->adr~split-multipaper-pdfs-by-outline~1]`
* The markers `// [impl->req~…]` and `// [utest->req~…]`, in `PdfOutlineSplitter`, `PdfLocationResolver` and the tests link the code to the requirements in `docs/requirements/split-attached-pdf.md`.
* `PdfOutlineSplitterTest`, `PdfPaperExtractorTest`, `PdfLocationResolverTest`, `AttachedPdfSplitterTest`, `MultiPaperPdfImporterTest` and `ImportHandlerTest` test the splitting, the extraction, the page offset and the import.

## More Information

* Issue: <https://github.com/JabRef/jabref/issues/8128>
* The page offset follows the proposal in the issue: the proceedings entry holds the PDF and an offset, and the papers cross-reference it.

<!-- markdownlint-disable-file MD022 -->
