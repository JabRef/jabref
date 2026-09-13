---
name: jabref-contributor
category: developers
description: Gradle build, module layout (jablib, jabgui, jabkit, jabls, jabsrv), JUnit 5 testing rules, naming conventions, and the mandatory pre-PR checklist for contributing to JabRef.
license: MIT
---

# Contributing to JabRef

Conventions for working on the [JabRef](https://github.com/JabRef/jabref) codebase.

> **AI policy — read first.** JabRef does not accept fully AI-generated pull requests. AI tools may only assist; a human must understand and take responsibility for every change. See [CONTRIBUTING.md](https://github.com/JabRef/jabref/blob/main/CONTRIBUTING.md) and [AGENTS.md](https://github.com/JabRef/jabref/blob/main/AGENTS.md) in the repository root — when working in a JabRef checkout, read both files before making changes.

## Modules

| Module   | Purpose                                          |
| -------- | ------------------------------------------------ |
| `jablib` | Core library — logic, model, importers/exporters |
| `jabgui` | JavaFX desktop GUI                               |
| `jabkit` | CLI application                                  |
| `jabls`  | Language Server Protocol implementation          |
| `jabsrv` | HTTP server                                      |

Key paths: `jablib/src/main/java/org/jabref/logic/` (business logic), `jablib/src/main/java/org/jabref/model/` (data model), `jabgui/src/main/java/org/jabref/gui/` (GUI), `docs/` (developer docs and ADRs).

## Build and run

Requires JDK 25+ for Gradle (the wrapper downloads a JDK itself):

```bash
./gradlew build            # build all modules
./gradlew :jabgui:run      # launch the GUI
./gradlew :jablib:test     # run core tests
```

## Conventions

- **Terminology:** say "library", not "database" — prefer `Library*` over `Database*` in new identifiers (see the [glossary](https://github.com/JabRef/jabref/tree/main/docs/glossary), in particular [library](https://github.com/JabRef/jabref/blob/main/docs/glossary/library.md)).
- **Tests:** plain JUnit 5 assertions only (see ADR-0009); do not introduce Hamcrest or AssertJ. Mock `*Preferences` classes and stub only the getters the test needs.
- **`@` in commit messages and PR texts:** wrap annotations in backticks (`` `@Nullable` ``) — GitHub turns a plain `@Nullable` into a mention of that user.
- **Issue references in code:** full URL (`https://github.com/JabRef/jabref/issues/9738`), never a bare `#9738` — the source reader has no repository context.
- **Icons:** add a new icon to `IconTheme.JabRefIcons`; take the SVG path from the `MDI*` enums of [svg-materialdesign](https://github.com/Maran23/svg-materialdesign) (e.g. `MDITechnology.BOOK_OUTLINE`), never as a hardcoded path string. Font icons still come from Ikonli.
- **Minimal diffs:** no reformatting of existing code, no speculative refactoring, no drive-by cleanups.
- **Dependencies:** do not add new ones without justification.
- **Architecture decisions:** documented as ADRs in `docs/decisions/`; add a new ADR when making an architecturally significant choice.
- **Localization:** user-visible strings go through `Localization.lang(...)`; add keys to `jablib/src/main/resources/l10n/JabRef_en.properties` only — other languages are translated via Crowdin.

## Branches

- `main` is the development branch; pull requests target it. `stable` is the last release plus ported fixes and only receives pull requests for changes that make no sense on `main`.
- CI labels a pull request into `main` with `dev: into-stable` when it links an issue of type "bug"; after the merge, CI ports it to `stable` in a separate port pull request. Maintainers add or remove the label by hand; never add it to a port pull request (`port-<number>-to-<branch>`).
- Add the `CHANGELOG.md` entry once, in the branch the pull request targets; the port carries it over.
- Details: <https://devdocs.jabref.org/contributing.html#branching-strategy>.

## Stacked pull requests

When a PR is based on another open PR's branch and that base PR is squash-merged, merging `main` back in produces spurious conflicts — Git no longer sees the branch's commits in `main`. Do **not** use GitHub's "Rebase" button for this; it rewrites the branch and usually multiplies the conflicts.

Instead create a magic merge commit that links the branch to the squashed history, then merge `main` normally: <https://github.com/koppor/magic-merge-commit/blob/main/skills/magic-merge-commit/SKILL.md>

```bash
jbang do@koppor/magic-merge-commit <squash-merged-pr-number>
```

## Before opening a PR

Work through every point of `CHECKLIST.md` in the repository root — it is the mandatory quality gate. Also:

- Add a `CHANGELOG.md` entry (unreleased section) for user-visible changes.
- Reference the issue the PR fixes.
- Write "Steps to test" as a numbered list with a cropped screenshot of the result for visible changes. No videos (only when another program is involved, such as drag and drop or push to an external application).
- Run `./gradlew rewriteRun` if the build reports OpenRewrite violations.

## Further reading

- [AGENTS.md](https://github.com/JabRef/jabref/blob/main/AGENTS.md) — rules for automated agents in this repository
- [CONTRIBUTING.md](https://github.com/JabRef/jabref/blob/main/CONTRIBUTING.md) — full contribution guide, including the AI usage policy
- [CHECKLIST.md](https://github.com/JabRef/jabref/blob/main/CHECKLIST.md) — the mandatory pre-PR quality gate
- <https://devdocs.jabref.org/> — developer documentation
- <https://deepwiki.com/JabRef/jabref> — architecture Q&A
