# Code checklist

> [!IMPORTANT]
> **This is a mandatory final gate.** When the implementation is finished and before you open a PR, work through **every** box below and tick it. If a box cannot be ticked, fix the code first; mark a box `[/]` only if the point genuinely does not apply.
>
> `AGENTS.md` describes how to write code *while* developing — this file confirms the finished result. Do not skip the checklist and do not skip individual points.

## 1. Code self-review

Read your own diff once, top to bottom, and confirm each point.

### Nullability and control flow

- [ ] No `== null` / `!= null` checks — JSpecify annotations (`@NullMarked`, `@Nullable`, `@NonNull`) used instead.
- [ ] No `Objects.requireNonNull(...)` — nullability expressed via JSpecify annotations.
- [ ] `Optional` consumed with `ifPresent` / `ifPresentOrElse` / `map` / `orElseThrow` — never `orElse(unusedValue)` nor an `isPresent()` + `get()` block.
- [ ] `StringUtil.isBlank(...)` used instead of `s == null || s.isBlank()`.

### Exceptions

- [ ] No `catch (Exception e)` — only specific exceptions are caught.
- [ ] No `throw new RuntimeException(...)` / `IllegalStateException(...)` — these tear down the whole application.
- [ ] Logged exceptions are passed as the **last** logger argument (`LOGGER.info("...", e)`), not concatenated into the message string.

### Style and idioms

- [ ] New `BibEntry` objects built with withers (`withField`, not `setField`).
- [ ] Modern Java used: `List.of()` / `Map.of()` / `Set.of()`, `Path.of()`, `SequencedCollection` / `SequencedSet`, text blocks.
- [ ] Regexes use a precompiled `Pattern.compile(...)` constant, not `String.matches(...)`.
- [ ] Background work uses `org.jabref.logic.util.BackgroundTask`, not `new Thread()`.
- [ ] No commented-out code, no trivial comments restating the code, no AI-disclosure comments in source.

### User-facing text

- [ ] All user-facing text localized (`Localization.lang` in Java, `%` prefix in FXML).
- [ ] Sentence case (not Title Case); no trailing `!`; labels do not end with `:`.
- [ ] Variance expressed with placeholders (`"...: %0"`), not string concatenation.

### Security

- [ ] User-controlled data (request params, entry fields, file contents) is HTML-escaped before being written into any `text/html` response — including exception/error messages, not just the success body (XSS).

### Tests

- [ ] Behavior changes in `org.jabref.model` / `org.jabref.logic` have added or updated tests.
- [ ] Tests assert object contents (`assertEquals`), use plain JUnit asserts (not AssertJ), have no `@DisplayName`, do not catch exceptions, and use `@TempDir` instead of manual temp directories.

## 2. Verification commands

Run in this order — cheapest first. Each must pass.

- [ ] `./gradlew :jablib:check` (or `./gradlew check` for all modules).
- [ ] `./gradlew checkstyleMain checkstyleTest checkstyleJmh`.
- [ ] `./gradlew modernizer`.
- [ ] `./gradlew --no-configuration-cache :rewriteDryRun` reports no changes (run `./gradlew rewriteRun` to fix).
- [ ] `./gradlew javadoc`.
- [ ] `npx markdownlint-cli2 "docs/**/*.md" "*.md"` (only if Markdown changed).
- [ ] Only if formatting is still off after `rewriteRun`: `docker run -v $(pwd):/github/workspace ghcr.io/leventebajczi/intellij-format:master "*.java" "" ".idea/codeStyles/Project.xml"`.

## 3. Documentation

- [ ] `CHANGELOG.md` entry added if the change is visible to the user (end-user wording, no extra blank lines). Use `TODO` as the issue/PR reference placeholder when no issue is known and the PR is not yet created — never a fake number.
- [ ] Searched [jabref/issues](https://github.com/JabRef/jabref/issues) and [jabref-koppor/issues](https://github.com/JabRef/jabref-koppor/issues) for a related issue; linked only on a confident match, otherwise kept `TODO` (no `closes`/`fixes` for merely-similar issues).
- [ ] Requirement added to `docs/requirements/<area>.md` if the change is a new feature or significant bug fix (skip for refactors, minor fixes, and internal changes).
- [ ] Developer documentation under `docs/` updated if behavior or architecture changed.

## 4. Pull request

- [ ] PR body built from `.github/PULL_REQUEST_TEMPLATE.md`, every section filled.
- [ ] All checklist items kept and marked `[x]`, `[ ]`, or `[/]`.
- [ ] All HTML comments removed from the PR body.
- [ ] PR created with `gh pr create --body-file <file>` (not `--body`).
- [ ] If `CHANGELOG.md` used a `TODO` placeholder, it was replaced with the real PR-number link after PR creation, then committed and pushed.
