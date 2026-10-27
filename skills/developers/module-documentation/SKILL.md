---
name: module-documentation
category: developers
description: How to write and maintain package-level (package-info.java) and module-level (module-info.java) Javadoc in JabRef, including deep links into devdocs.jabref.org. Use when adding a package or changing a package's or module's public surface.
license: MIT
---

# Package and module documentation

> [!IMPORTANT]
> This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
>
> Read and follow:
> • [AGENTS.md](https://github.com/JabRef/jabref/blob/main/AGENTS.md)
> • [CONTRIBUTING.md](https://github.com/JabRef/jabref/blob/main/CONTRIBUTING.md)

`module-info.java` and `package-info.java` are the orientation layer of the JabRef code base.
They are read by humans and by AI agents that have no code-graph tooling and need to find the right place by reading, so write them for routing: what lives here, where to start, and where to go instead.

## When to update

- Creating a package: add a `package-info.java`.
- Adding, moving, renaming or removing a class that a `package-info.java` mentions, or that is an entry point of its package: update the file.
- Changing a module's exports or responsibility: update the `///` comment on its `module-info.java`.
- Nothing to do for changes inside existing classes.

## Content

- Every module has a Markdown Javadoc (`///`) comment on `module-info.java`: the module's responsibility in one or two sentences, its main entry points (`[ClassName]`), and links to the devdocs pages describing it.
- Every package with non-trivial content has a `package-info.java`.
- Structure: first sentence states the package's purpose. Then the entry-point classes as `[ClassName]` links. Then adjacent packages for related concerns ("Parsing of `.bib` files is in [org.jabref.logic.importer.fileformat]"). Then `@see` deep links to developer documentation.
- Deep links are `@see` block tags, one per line, in the standard Javadoc URL form: `/// @see <a href="https://devdocs.jabref.org/code-howtos/testing.html">Testing code howto</a>` (javadoc rejects a Markdown `[label](url)` after `@see`; the `<a>` element is the one HTML exception to the Markdown-only rule). They point into <https://devdocs.jabref.org/>, which renders `docs/`: `docs/<path>.md` becomes `https://devdocs.jabref.org/<path>.html` (for example `docs/code-howtos/localization.md` → <https://devdocs.jabref.org/code-howtos/localization.html>). Section anchors work as on the rendered page (`architecture-and-components.html#preferences`). Link the ADR in `docs/decisions/` that motivated the package's design where one exists.
- Tone: neutral, professional software-engineering prose. No change history, no issue references, no marketing. Do not repeat class-level Javadoc; point to it.
- Use Markdown syntax inside `///` comments as required by `AGENTS.md`: `` `code` ``, `[ClassName]`, fenced code blocks.

## Examples

- `jablib/src/main/java/org/jabref/logic/bibtex/comparator/package-info.java` (short package)
- `jabgui/src/main/java/org/jabref/gui/preferences/forms/package-info.java` (package with its own conventions)
- `jablib/src/main/java/module-info.java` (module)

## Verification

`./gradlew :<module>:javadoc` must pass. Unresolved `[ClassName]` references do not fail the build (reference checking is off because tests are cross-linked), so grep the output for `reference not found` and fix every hit in the touched files.
