# Code checklist

This is the final quality check to run **after the work is finished**, before
creating a PR. `AGENTS.md` gives guidance to follow *while* developing; this
file is the gate that confirms the result. Go through every point and fix the
code until all points are fulfilled. Do not skip any point.

## Code rules

- [ ] JSpecify annotations used instead of `== null` checks.
- [ ] `org.jabref.logic.util.strings.StringUtil.isBlank(java.lang.String)` used instead of `== null || ...isBlank()`.
- [ ] No `catch (Exception e)` — only specific exceptions caught.
- [ ] No commented-out code left behind.
- [ ] User-facing text is localized (`Localization.lang` in Java, `%` prefix in FXML).
- [ ] New `BibEntry` objects created with withers (`withField`, not `setField`).
- [ ] Tests added or updated for changed behavior in `org.jabref.model` / `org.jabref.logic`.

## Verification commands

- [ ] `./gradlew :jablib:check` (or `./gradlew check` for all modules) passes.
- [ ] `./gradlew checkstyleMain checkstyleTest checkstyleJmh` passes.
- [ ] `./gradlew modernizer` passes.
- [ ] `./gradlew --no-configuration-cache :rewriteDryRun` reports no changes (run `./gradlew rewriteRun` to fix).
- [ ] `./gradlew javadoc` passes.
- [ ] `npx markdownlint-cli2 "docs/**/*.md" "*.md"` passes.
- [ ] `docker run -v $(pwd):/github/workspace ghcr.io/leventebajczi/intellij-format:master "*.java" "" ".idea/codeStyles/Project.xml"` executed to ensure proper formatting.

## Documentation

- [ ] `CHANGELOG.md` entry added (end-user wording, no extra blank lines).
- [ ] Requirement added to `docs/requirements/<area>.md` for a new feature or significant bug fix.
- [ ] Developer documentation under `docs/` updated if behavior or architecture changed.
