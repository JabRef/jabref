---
nav_order: 67
parent: Decision Records
---

# Use Pandoc to Convert BST Output for the LibreOffice Integration

The Context, Options, and original confirmation plan record the decision at introduction.
BST is now supported; [Current implementation](#current-implementation) describes the shared engine introduced in [ADR-0084](0084-canonical-writer-citations.md).

## Context and Problem Statement

JabRef's [LibreOffice integration](https://docs.jabref.org/cite/openofficeintegration) supports [CSL styles](https://citationstyles.org/) and [JStyles](https://jstyles.jabref.org/), but not [BST](https://text2bib.org/bsts) (BibTeX style) files, although many publishers still distribute only `.bst`.
Initial BST support existed in the OpenOffice panel but was removed as unfinished ([#602](https://github.com/JabRef/jabref/pull/602), tracked in [#624](https://github.com/JabRef/jabref/issues/624)); the [stated blocker](https://github.com/JabRef/jabref/pull/602#issuecomment-167756642) was that `.bst` files such as `IEEEtran.bst` emit "quite a bit of non-trivial latex code to parse".

Supporting BST in LibreOffice documents is the goal. The open question is: `BstVM` renders an entry to a LaTeX fragment (`\emph{...}`, ``` ``...'' ```, `~`, `{\&}`, plus a `\providecommand` preamble), whereas `OOTextWriter` consumes an HTML-like tag vocabulary (`<b>`, `<i>`, `<smallcaps>`, `<sup>`, `<p>`).
**How do we convert BST's LaTeX output into `OOText`?**

Every option below produces BST-styled citations in a LibreOffice document. They differ only in who parses the LaTeX and what formatting survives.

## Decision Drivers

* Robustness against the LaTeX subset emitted by arbitrary `.bst` files, not just one curated style
* Preservation of semantic formatting (italics, bold, small caps) rather than flattening to plain text
* Long-term maintenance burden of any LaTeX parser JabRef would own
* Dependency footprint and portability across Windows, macOS, and Linux
* Reuse of existing, reviewed JabRef code (`CSLFormatUtils.transformHTML`, `OOTextWriter`, and the numbering logic then in `CSLReferenceMarkManager`)
* The feature has stalled twice; a credible answer to the known blocker is required

## Considered Options

1. SnuggleTeX-based converter (existing dependency)
2. Pandoc as an optional external process
3. Reuse the existing plain-text cleanup (`RemoveLatexCommandsFormatter` / `LatexToUnicodeAdapter`)
4. Hybrid: Pandoc as primary with a SnuggleTeX-based pure-Java fallback when Pandoc is unavailable
5. Hand-written LaTeX-subset to `OOText` converter

## Decision Outcome

Chosen option: "Pandoc as an optional external process", because it removes exactly the blocker that stopped this feature twice - parsing arbitrary BST LaTeX - while everything downstream of the produced HTML reuses code JabRef already ships and trusts.
The dependency is optional, so it adds no weight for users who do not use BST.

### Consequences

* Good, because Pandoc's LaTeX parser is mature and battle-tested, and handles `~`, ``` ``...'' ```, `\emph`, `{\&}`, and brace nesting without any parser code owned by JabRef
* Good, because semantic formatting survives: `\emph{Journal}` becomes `<em>Journal</em>` and ultimately an italic run in LibreOffice
* Good, because `BSTFormatUtils` maps HTML emphasis and small-caps tags and reuses the existing cleanup approach for entity decoding and `<div>`/`<a>`/`<span>` stripping
* Good, because it follows an established JabRef pattern: `OcrMyPdfEngine` already shells out to an optional external binary with an `isAvailable()` guard and a user-configurable path preference
* Good, because a bare command name (`pandoc`) is resolved through the OS `PATH`, so no per-user path logic is required; the Windows installer adds `%LOCALAPPDATA%\Pandoc` to the user `PATH`, and the path is additionally auto-detected and stored as a preference with manual override
* Bad, because BST support requires an installation step that CSL and JStyles do not
* Bad, because it introduces a process boundary into a citation path: process spawning must be batched (one call per bibliography, not per entry) to avoid noticeable latency
* Bad, because failures arrive as exit codes and stderr that must be surfaced deliberately; silently returning empty output produces an empty bibliography that is hard to diagnose
* Neutral, because the `\providecommand` preamble emitted by styles such as `IEEEtran.bst` must be excluded before Pandoc conversion; `BstCitationFormatter.parseBibliography` extracts each `\bibitem` body without the preamble or bibliography environment

### Confirmation

The original confirmation plan was:

* A standalone pipeline test renders a `BibEntry` through a real `.bst`, pipes it through Pandoc, and asserts the resulting `OOText` contains the expected `<i>` markup with decoded entities.
* The feature is disabled in the UI when `PandocLatexConverter.isAvailable()` returns `false`, mirroring the OCR feature's behaviour; verified by a test with an invalid configured path.
* Integration tests cover at least one non-trivial style (`IEEEtran.bst`) and one simple style (`apa.bst`) to confirm the preamble handling generalises.
* Bundled `.bst` styles (`IEEEtran`, `apa`, `abbrv`) exercise the pipeline out of the box, alongside user-supplied external `.bst` files.

## Pros and Cons of the Options

### SnuggleTeX-based converter (existing dependency)

Use [SnuggleTeX](https://github.com/davemckain/snuggletex) (already a JabRef dependency) to parse LaTeX and produce XHTML/MathML, then map to the `OOText` vocabulary.

* Good, because it is already a JabRef dependency; no new install step; in-process
* Good, because it produces structured XHTML/MathML rather than requiring hand-rolled parsing
* Neutral, because in JabRef today SnuggleTeX is used primarily as a validator with DOM/XHTML output disabled; using its conversion output here would be a new, unexercised path that needs validation against real `.bst` output
* Bad, because its LaTeX coverage is oriented toward math and does not span several macro constructs `.bst` files emit (e.g., `\providecommand`, `\csname...\endcsname`, `\expandafter\ifx`, `\typeout`, per-style macros like `\BIBentryALTinterwordspacing`)
* Bad, because coverage gaps will surface per `.bst` style, leading to open-ended support work extending command coverage and mapping
* Bad, because the mapping layer to `OOText` that will be required to translate the XHTML/MathML output into the `OOText` inline vocabulary (italics, bold, small caps, quotes) will be relatively much harder to implement, due to the need of many non-trivial normalizations (verified using experimentation)

### Pandoc as an optional external process

`pandoc -f latex -t html --wrap=none` over stdin/stdout, wrapped in a class.

* Good, because it eliminates the historical blocker outright
* Good, because its HTML output maps onto the `OOText` vocabulary with a handful of tag renames
* Good, because the optional-dependency pattern already exists in the codebase and needs no new infrastructure
* Neutral, because the binary is discovered through the OS `PATH` with a preference for manual override, so no platform-specific path logic is needed
* Bad, because BST support is unavailable until the user installs Pandoc
* Bad, because process-level error handling (exit code, stderr draining) must be implemented carefully to avoid silent empty output and pipe-buffer deadlocks

### Reuse the existing plain-text cleanup

`BstPreviewLayout` already strips `\newblock`, converts quotes, and applies `RemoveLatexCommandsFormatter`; feed its output into `OOText` as unformatted text.

* Good, because the code exists, is already used for BST previews, and needs no new dependency
* Good, because it is by far the smallest change and would ship soonest
* Bad, because it produces flat plain text: `\emph{Journal}` is discarded rather than converted, so italicized journal and book titles are lost - a visible defect in a bibliography
* Bad, because it deletes markup instead of translating it, which is acceptable for a tooltip but not for a document deliverable
* Bad, because it already contains style-specific hacks (for example a literal `#2}}` replacement for `IEEEtran.bst`), demonstrating that the delete-what-we-do-not-understand approach does not generalise

### Hand-written LaTeX-subset to `OOText` converter

A JabRef-owned mapper from `\emph{}`, `\textbf{}`, `\textsc{}`, ``` `` '' ```, `~` to the `OOText` tag vocabulary.

* Good, because it introduces no external dependency, so BST works for every user out of the box
* Good, because conversion happens in-process, with no latency or failure modes from a subprocess
* Good, because formatting is preserved for the constructs the mapper covers
* Neutral, because for a single curated style family the required LaTeX subset is small and well defined
* Bad, because the LaTeX subset emitted across arbitrary `.bst` files is open-ended, and correctness requires brace-level parsing rather than regular expressions (regex-level handling cannot correctly deal with nested braces)
* Bad, because practical support devolves into extending coverage per encountered `.bst` construct (e.g., style-specific macros), which is open-ended maintenance work compared to inheriting a mature parser's coverage; this is [what stalled the original attempt](https://github.com/JabRef/jabref/pull/602#issuecomment-167756642)

### Hybrid: Pandoc primary with SnuggleTeX-based pure-Java fallback

Description: keep Pandoc as the primary converter; when it is not available, fall back to a limited, in-process path leveraging SnuggleTeX-backed LaTeX→Unicode and preview-style transforms for inline emphasis/quotes. The fallback would map only the subset supported by `OOTextWriter` and document reduced fidelity.

* Good, because BST bibliography would work without installing Pandoc (lower barrier)
* Good, because it is fully in-process and reuses existing JabRef code (preview transforms, `LatexToUnicodeAdapter`)
* Bad, because fidelity for arbitrary `.bst` outputs will be lower than Pandoc; complex LaTeX constructs may not render ideally
* Neutral, because it can be implemented as an availability-based fallback without impacting the main (Pandoc) path
* Bad, because the non-trivial normalizations and mappings to convert to final `OOText` will be applicable here as well for SnuggleTex

## More Information

### Current implementation

The conversion decision remains in use within the shared citation engine described in [ADR-0084](0084-canonical-writer-citations.md).
`BstRenderer` implements `CitationRenderer` and resolves the cited set through `CitationKeyResolution`.
Author-year citation-only rendering uses JabRef's fixed formatter without executing BST or invoking Pandoc.
Numeric and style-defined citations still need BST output for ordering or labels, even without a bibliography.
When BST output is needed, `BstVM.render` runs once over the resolved cited set, unless it is empty.
`BstCitationFormatter.parseBibliography` separates keys, optional labels, and entry bodies at each `\bibitem`, excluding the leading preamble and bibliography environment.
For a requested, nonempty bibliography, bodies pass through `BSTFormatUtils.normalizeLegacyForPandoc`, one batched `PandocLatexConverter.latexToHtml` call, and `BSTFormatUtils.convertPandocHtmlToOOText`, before becoming `OOText` in a `RenderResult`.
Citation-only rendering never invokes Pandoc.
`CitationMarkerWriter` and `OOTextWriter` apply prepared output during the engine's write phase, not while rendering.

The original implementation reused numbering from `CSLReferenceMarkManager` and proposed an analogous `BSTReferenceMarkManager`.
Those managers are no longer active classes: `BstRenderer` derives numbering from the resolved cited set and BST bibliography order through `CitationNumbering.bibliographyOrder`.
Unlike the original UI guard proposed above, the style dialog shows a warning for a blank Pandoc path but does not disable BST selection or probe availability on opening.
`PandocLatexConverter.autoDetect` probes platform-specific candidates, including `pandoc` on `PATH` and a detected Windows installation path; users can override the stored path.
When bibliography conversion actually needs Pandoc, executable failures become `PandocRenderException` with installation and path guidance before Writer mutation.
`BstRendererTest` covers batched conversion, citation-only gating, and missing-executable errors; `PandocLatexConverterTest` covers platform-specific detection candidates.

The current [package layout and ownership guide](../code-howtos/openoffice/code-reorganization.md) identifies the rendering, storage, and Writer infrastructure boundaries.

The conversion Pandoc performs, and the normalisation JabRef adds on top, are:

| BST output (LaTeX) | Pandoc (HTML)                      | Normalised (`OOText`)            | Result in LibreOffice |
|--------------------|------------------------------------|----------------------------------|-----------------------|
| `\emph{Journal}`   | `<em>Journal</em>`                 | `<i>Journal</i>`                 | italic                |
| `\textbf{x}`       | `<strong>x</strong>`               | `<b>x</b>`                       | bold                  |
| `\textsc{x}`       | `<span class="smallcaps">x</span>` | `<smallcaps>x</smallcaps>`       | small caps            |
| ``` ``x'' ```      | `“x”`                              | `“x”`                            | curly quotes          |
| `~`                | non-breaking space                 | non-breaking space               | non-breaking space    |
| `{\&}`             | `&amp;`                            | `&` (decoded by `transformHTML`) | `&`                   |
| paragraph text     | `<p>…</p>`                         | unwrapped                        | no spurious break     |

JabRef normalizes legacy LaTeX switches before Pandoc, then maps HTML emphasis and styled spans, removes inline-math emphasis and wrappers, unwraps outer paragraphs, and applies `BSTFormatUtils.transformHTML` for cleanup and entity decoding.
The table shows representative mappings, not every normalization; the current BST path does not delegate to `CSLFormatUtils.transformHTML` as originally proposed.
Pandoc still owns general LaTeX parsing, including brace nesting, ligatures, quote forms, tilde handling, and entity escaping.
`\emph` is semantic rather than literal in LaTeX (it flips to upright inside already-italic text), which is why `.bst` files use it for journal and book titles; the table illustrates ordinary, non-nested emphasis.

Changing `BstVM` to emit HTML instead of LaTeX was examined and is not viable: the markup originates in string literals inside the `.bst` programs themselves, which the virtual machine only concatenates, so there is nothing at the VM level to reinterpret. It is therefore not listed as an option.

`.bst` files define only the rendering of the bibliography, not the in-text citation format - in LaTeX that responsibility belongs to packages such as `natbib`, not to the style file.
The original implementation therefore supplied two fixed citation formats, numeric and author-year, analogous to the default JStyles.
The current `BstRenderer` also supports style-defined citations using optional labels extracted from bibliography output by `BstCitationFormatter`.
This is orthogonal to the decision recorded here: the Pandoc conversion problem concerns bibliography entry bodies, not the selection of citation-marker format.

Related issues: [#624](https://github.com/JabRef/jabref/issues/624), [#11102](https://github.com/JabRef/jabref/issues/11102), [#602](https://github.com/JabRef/jabref/pull/602).
Pandoc was first suggested for this purpose in [#624](https://github.com/JabRef/jabref/issues/624).

This decision was implemented in [#16315](https://github.com/JabRef/jabref/pull/16315).
