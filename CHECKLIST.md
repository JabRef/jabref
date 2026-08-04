# Code checklist

> [!IMPORTANT]
> **This is a mandatory final gate.** When the implementation is finished and before you open a PR, work through **every** box below and tick it. If a box cannot be ticked, fix the code first; mark a box `[/]` only if the point genuinely does not apply.
>
> `AGENTS.md` describes how to write code *while* developing — this file confirms the finished result. Do not skip the checklist and do not skip individual points.

## 1. Code self-review

Read your own diff once, top to bottom, and confirm each point.

### Nullability and control flow

- [x] No `== null` / `!= null` checks — JSpecify annotations (`@NullMarked`, `@Nullable`, `@NonNull`) used instead.
- [x] No `Objects.requireNonNull(...)` — nullability expressed via JSpecify annotations.
- [x] New classes annotated with `@NullMarked` (`org.jspecify.annotations.NullMarked`).
- [x] `Optional` consumed with `ifPresent` / `ifPresentOrElse` / `map` / `orElseThrow` — never `orElse(unusedValue)` nor an `isPresent()` + `get()` block.
- [x] `StringUtil.isBlank(...)` used instead of `s == null || s.isBlank()`.

### Exceptions

- [x] No `catch (Exception e)` — only specific exceptions are caught.
- [x] No `throw new RuntimeException(...)` / `IllegalStateException(...)` — these tear down the whole application.
- [x] Logged exceptions are passed as the **last** logger argument (`LOGGER.info("...", e)`), not concatenated into the message string.

### Style and idioms

- [x] New `BibEntry` objects built with withers (`withField`, not `setField`).
- [x] Modern Java used: `List.of()` / `Map.of()` / `Set.of()`, `Path.of()`, `SequencedCollection` / `SequencedSet`, text blocks.
- [x] Regexes use a precompiled `Pattern.compile(...)` constant, not `String.matches(...)`.
- [x] Background work uses `org.jabref.logic.util.BackgroundTask`, not `new Thread()`.
- [x] No commented-out code, no trivial comments restating the code, no AI-disclosure comments in source.
- [x] Markdown Javadoc (`///`) uses Markdown syntax, not JavaDoc inline tags: `` `code` `` instead of `{@code}`, `[ClassName]` instead of `{@link}`.

### User-facing text

- [x] All user-facing text localized (`Localization.lang` in Java, `%` prefix in FXML).
- [x] Sentence case (not Title Case); no trailing `!`; labels do not end with `:`.
- [x] Variance expressed with placeholders (`"...: %0"`), not string concatenation.

### Security

- [x] User-controlled data (request params, entry fields, file contents) is HTML-escaped before being written into any `text/html` response — including exception/error messages, not just the success body (XSS).

### Tests

- [x] Behavior changes in `org.jabref.model` / `org.jabref.logic` have added or updated tests.
- [x] Tests assert object contents (`assertEquals`), use plain JUnit asserts (not AssertJ), have no `@DisplayName`, do not catch exceptions (let them propagate so JUnit reports setup/teardown failures directly), and use `@TempDir` instead of manual temp directories.

## 2. Verification commands

Run in this order — cheapest first. Each must pass.

- [x] `./gradlew :jablib:check` (or `./gradlew check` for all modules).
- [x] `./gradlew checkstyleMain checkstyleTest checkstyleJmh`.
- [x] `./gradlew modernizer`.
- [x] `./gradlew --no-configuration-cache :rewriteDryRun` reports no changes (run `./gradlew rewriteRun` to fix).
- [x] `./gradlew javadoc`.
- [x] `npx markdownlint-cli2 "docs/**/*.md" "*.md"` (only if Markdown changed).
- [x] Only if formatting is still off after `rewriteRun`: `docker run -v $(pwd):/github/workspace ghcr.io/leventebajczi/intellij-format:master "*.java" "" ".idea/codeStyles/Project.xml"`.

## 3. Documentation

- [x] `CHANGELOG.md` entry added if the change is visible to the user (end-user wording, no extra blank lines). Link the issue if one exists; link the PR only when no issue exists. Use `TODO` as the placeholder when neither is known yet — never a fake number.
- [x] Searched [jabref/issues](https://github.com/JabRef/jabref/issues) and [jabref-koppor/issues](https://github.com/JabRef/jabref-koppor/issues) for a related issue; linked only on a confident match, otherwise kept `TODO` (no `closes`/`fixes` for merely-similar issues).
- [x] Requirement added to `docs/requirements/<area>.md` if the change is a new feature or significant bug fix (skip for refactors, minor fixes, and internal changes).
- [x] Developer documentation under `docs/` updated if behavior or architecture changed.

## 4. Pull request

- [x] PR body built from `.github/PULL_REQUEST_TEMPLATE.md`, every section filled.
- [x] All checklist items kept and marked `[x]`, `[ ]`, or `[/]`.
- [x] All HTML comments removed from the PR body.
- [x] PR created with `gh pr create --body-file <file>` (not `--body`).
- [x] If `CHANGELOG.md` used a `TODO` placeholder (no issue confidently identified yet — an existing issue link always stays), it was replaced with the real PR-number link after PR creation, then committed and pushed. If an issue is identified or created later, the link is switched to the issue.
