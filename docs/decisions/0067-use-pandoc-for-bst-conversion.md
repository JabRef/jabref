---
nav_order: 67
parent: Decision Records
---

# Use Pandoc to Convert BST Output for the LibreOffice Integration

## Context and Problem Statement

JabRef's LibreOffice integration supports CSL styles and JStyles, but not BST (BibTeX style) files, although many publishers still distribute only `.bst`.
Initial BST support existed in the OpenOffice panel but was removed as unfinished ([#602](https://github.com/JabRef/jabref/pull/602), tracked in [#624](https://github.com/JabRef/jabref/issues/624)); the stated blocker was that `.bst` files such as `IEEEtran.bst` emit "quite a bit of non-trivial latex code to parse".

Supporting BST in LibreOffice documents is the goal, not a question. The open question is narrower: `BstVM` renders an entry to a LaTeX fragment (`\emph{...}`, `` ``...'' ``, `~`, `{\&}`, plus a `\providecommand` preamble), whereas `OOTextIntoOO` consumes an HTML-like tag vocabulary (`<b>`, `<i>`, `<smallcaps>`, `<sup>`, `<p>`).
**How do we convert BST's LaTeX output into `OOText`?**

Every option below produces BST-styled citations in a LibreOffice document. They differ only in who parses the LaTeX and what formatting survives.

## Decision Drivers

* Robustness against the LaTeX subset emitted by arbitrary `.bst` files, not just one curated style
* Preservation of semantic formatting (italics, bold, small caps) rather than flattening to plain text
* Long-term maintenance burden of any LaTeX parser JabRef would own
* Dependency footprint and portability across Windows, macOS, and Linux
* Reuse of existing, reviewed JabRef code (`CSLFormatUtils.transformHTML`, `OOTextIntoOO`, the numbering logic in `CSLReferenceMarkManager`)
* The feature has stalled twice; a credible answer to the known blocker is required

## Considered Options

1. Hand-written LaTeX-subset to `OOText` converter
2. Pandoc as an optional external process
3. Pandoc bundled with JabRef
4. Reuse the existing plain-text cleanup (`RemoveLatexCommandsFormatter` / `LatexToUnicodeAdapter`)

## Decision Outcome

Chosen option: "Pandoc as an optional external process", because it removes exactly the blocker that stopped this feature twice - parsing arbitrary BST LaTeX - while everything downstream of the produced HTML reuses code JabRef already ships and trusts.
The dependency is optional and follows the precedent already set by `OcrMyPdfEngine`, so it adds no weight for users who do not use BST.

### Consequences

* Good, because Pandoc's LaTeX parser is mature and battle-tested, and handles `~`, `` ``...'' ``, `\emph`, `{\&}`, and brace nesting without any parser code owned by JabRef
* Good, because semantic formatting survives: `\emph{Journal}` becomes `<em>Journal</em>` and ultimately an italic run in LibreOffice
* Good, because the remaining JabRef code is a small tag rename (`<em>`/`<strong>`/`<span class="smallcaps">`) that then delegates to the existing `CSLFormatUtils.transformHTML`, inheriting entity decoding and `<div>`/`<a>`/`<span>` stripping
* Good, because it follows an established JabRef pattern: `OcrMyPdfEngine` already shells out to an optional external binary with an `isAvailable()` guard and a user-configurable path preference
* Good, because a bare command name (`pandoc`) is resolved through the OS `PATH`, so no per-user path logic is required; the Windows installer adds `%LOCALAPPDATA%\Pandoc` to the user `PATH`, and the path is additionally auto-detected and stored as a preference with manual override
* Bad, because BST support requires an installation step that CSL and JStyles do not
* Bad, because it introduces a process boundary into a citation path: process spawning must be batched (one call per bibliography, not per entry) to avoid noticeable latency
* Bad, because failures arrive as exit codes and stderr that must be surfaced deliberately; silently returning empty output produces an empty bibliography that is hard to diagnose
* Neutral, because Pandoc's output still requires stripping the `\providecommand` preamble emitted by styles such as `IEEEtran.bst`; this is done by keeping only the text following the last `\bibitem`

### Confirmation

* A standalone pipeline test renders a `BibEntry` through a real `.bst`, pipes it through Pandoc, and asserts the resulting `OOText` contains the expected `<i>` markup with decoded entities.
* The feature is disabled in the UI when `PandocLatexConverter.isAvailable()` returns `false`, mirroring the OCR feature's behaviour; verified by a test with an invalid configured path.
* Integration tests cover at least one non-trivial style (`IEEEtran.bst`) and one simple style (`apa.bst`) to confirm the preamble handling generalises.
* Bundled `.bst` styles (`IEEEtran`, `apa`, `abbrv`) exercise the pipeline out of the box, alongside user-supplied external `.bst` files.

## Pros and Cons of the Options

### Hand-written LaTeX-subset to `OOText` converter

A JabRef-owned mapper from `\emph{}`, `\textbf{}`, `\textsc{}`, `` `` '' ``, `~` to the `OOText` tag vocabulary.

* Good, because it introduces no external dependency, so BST works for every user out of the box
* Good, because conversion happens in-process, with no latency or failure modes from a subprocess
* Good, because formatting is preserved for the constructs the mapper covers
* Neutral, because for a single curated style family the required LaTeX subset is small and well defined
* Bad, because the LaTeX subset emitted across arbitrary `.bst` files is open-ended, and correctness requires brace-level parsing rather than regular expressions
* Bad, because this is precisely the work identified as the blocker in [#602](https://github.com/JabRef/jabref/pull/602) and the reason the feature was removed; nothing has changed to make it easier
* Bad, because JabRef would own and maintain a LaTeX parser indefinitely, growing it per reported `.bst` file

### Pandoc as an optional external process

`pandoc -f latex -t html --wrap=none` over stdin/stdout, wrapped in a class following `OcrMyPdfEngine`.

* Good, because it eliminates the historical blocker outright
* Good, because its HTML output maps onto the `OOText` vocabulary with a handful of tag renames
* Good, because the optional-dependency pattern already exists in the codebase and needs no new infrastructure
* Neutral, because the binary is discovered through the OS `PATH` with a preference for manual override, so no platform-specific path logic is needed
* Bad, because BST support is unavailable until the user installs Pandoc
* Bad, because process-level error handling (exit code, stderr draining) must be implemented carefully to avoid silent empty output and pipe-buffer deadlocks

### Pandoc bundled with JabRef

Ship platform-specific Pandoc binaries inside the JabRef distribution.

* Good, because it keeps Pandoc's parser quality while removing the installation step, so BST works out of the box
* Good, because the conversion code is identical to the optional-process option; only discovery changes
* Neutral, because JabRef already ships platform-specific installers, so per-platform assets are not a new concept
* Bad, because Pandoc binaries add roughly 100 MB per platform to the distribution, which is disproportionate for an optional feature
* Bad, because JabRef becomes responsible for tracking Pandoc releases and security updates
* Bad, because Pandoc is GPL-licensed, which requires review before bundling rather than invoking

### Reuse the existing plain-text cleanup

`BstPreviewLayout` already strips `\newblock`, converts quotes, and applies `RemoveLatexCommandsFormatter`; feed its output into `OOText` as unformatted text.

* Good, because the code exists, is already used for BST previews, and needs no new dependency
* Good, because it is by far the smallest change and would ship soonest
* Bad, because it produces flat plain text: `\emph{Journal}` is discarded rather than converted, so italicised journal and book titles are lost - a visible defect in a bibliography
* Bad, because it deletes markup instead of translating it, which is acceptable for a tooltip but not for a document deliverable
* Bad, because it already contains style-specific hacks (for example a literal `#2}}` replacement for `IEEEtran.bst`), demonstrating that the delete-what-we-do-not-understand approach does not generalise

## More Information

The full pipeline is: `BstVM.render(entry)` -> strip LaTeX structure and preamble -> Pandoc -> normalise Pandoc's tags -> `CSLFormatUtils.transformHTML` -> `OOText` -> `OOTextIntoOO.write`.
Numeric `[n]` markers reuse the first-appearance numbering already implemented in `CSLReferenceMarkManager`, which is independent of CSL.

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

Only the middle column is JabRef's own code: three tag renames plus unwrapping Pandoc's paragraph wrapper.
Everything in the first arrow - brace nesting, ligatures, quote forms, tilde handling, entity escaping - is Pandoc's, and is the work that would otherwise have to be written and maintained in Java.
`\emph` is semantic rather than literal in LaTeX (it flips to upright inside already-italic text), which is why `.bst` files use it for journal and book titles; in a bibliography entry it always resolves to italic.

Changing `BstVM` to emit HTML instead of LaTeX was examined and is not viable: the markup originates in string literals inside the `.bst` programs themselves, which the virtual machine only concatenates, so there is nothing at the VM level to reinterpret. It is therefore not listed as an option.

`.bst` files define only the bibliography, not the in-text citation format - in LaTeX that responsibility belongs to packages such as `natbib`, not to the style file.
JabRef therefore supplies the citation format itself, offering two fixed choices, numeric and author-year, analogous to the default JStyles.
This is orthogonal to the decision recorded here: the conversion problem concerns the bibliography entries, and the same Pandoc pipeline serves both citation formats.

Related issues: [#624](https://github.com/JabRef/jabref/issues/624), [#11102](https://github.com/JabRef/jabref/issues/11102), [#602](https://github.com/JabRef/jabref/pull/602).
Pandoc was first suggested for this purpose in [#624](https://github.com/JabRef/jabref/issues/624).

This decision was implemented in [#16315](https://github.com/JabRef/jabref/pull/16315).
