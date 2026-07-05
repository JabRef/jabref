---
name: jabref-contributor
description: Development conventions for contributing to JabRef, the open-source BibTeX/biblatex reference manager. Use when working on the JabRef codebase - covers the Gradle build, module layout (jablib, jabgui, jabkit, jabls, jabsrv), testing rules (JUnit 5, no Hamcrest), naming conventions, and the mandatory pre-PR checklist.
license: MIT
---

# Contributing to JabRef

Conventions for working on the [JabRef](https://github.com/JabRef/jabref) codebase.

> **AI policy — read first.** JabRef does not accept fully AI-generated pull requests. AI tools may only assist; a human must understand and take responsibility for every change. See [CONTRIBUTING.md](https://github.com/JabRef/jabref/blob/main/CONTRIBUTING.md) and [AGENTS.md](https://github.com/JabRef/jabref/blob/main/AGENTS.md) in the repository root — when working in a JabRef checkout, read both files before making changes.

## Modules

| Module | Purpose |
|---|---|
| `jablib` | Core library — logic, model, importers/exporters |
| `jabgui` | JavaFX desktop GUI |
| `jabkit` | CLI application |
| `jabls` | Language Server Protocol implementation |
| `jabsrv` | HTTP server |

Key paths: `jablib/src/main/java/org/jabref/logic/` (business logic), `jablib/src/main/java/org/jabref/model/` (data model), `jabgui/src/main/java/org/jabref/gui/` (GUI), `docs/` (developer docs and ADRs).

## Build and run

Requires JDK 25+ for Gradle (the wrapper downloads a JDK itself):

```bash
./gradlew build            # build all modules
./gradlew :jabgui:run      # launch the GUI
./gradlew :jablib:test     # run core tests
```

## Conventions

- **Terminology:** say "library", not "database" — prefer `Library*` over `Database*` in new identifiers.
- **Tests:** plain JUnit 5 assertions only (see ADR-0009); do not introduce Hamcrest or AssertJ. Mock `*Preferences` classes and stub only the getters the test needs.
- **Minimal diffs:** no reformatting of existing code, no speculative refactoring, no drive-by cleanups.
- **Dependencies:** do not add new ones without justification.
- **Architecture decisions:** documented as ADRs in `docs/decisions/`; add a new ADR when making an architecturally significant choice.
- **Localization:** user-visible strings go through `Localization.lang(...)`; add keys to `jablib/src/main/resources/l10n/JabRef_en.properties` only — other languages are translated via Crowdin.

## Before opening a PR

Work through every point of `CHECKLIST.md` in the repository root — it is the mandatory quality gate. Also:

- Add a `CHANGELOG.md` entry (unreleased section) for user-visible changes.
- Reference the issue the PR fixes.
- Run `./gradlew rewriteRun` if the build reports OpenRewrite violations.

## Further reading

- [AGENTS.md](https://github.com/JabRef/jabref/blob/main/AGENTS.md) — rules for automated agents in this repository
- [CONTRIBUTING.md](https://github.com/JabRef/jabref/blob/main/CONTRIBUTING.md) — full contribution guide, including the AI usage policy
- [CHECKLIST.md](https://github.com/JabRef/jabref/blob/main/CHECKLIST.md) — the mandatory pre-PR quality gate
- <https://devdocs.jabref.org/> — developer documentation
- <https://deepwiki.com/JabRef/jabref> — architecture Q&A
