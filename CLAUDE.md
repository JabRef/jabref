# CLAUDE.md — JabRef

JabRef is an open-source reference manager. Primary language: Java (most recent version), UI: JavaFX, build: Gradle (Kotlin DSL).

Full coding rules and agent constraints are in [AGENTS.md](./AGENTS.md). This file covers practical commands.

---

## Project structure

| Module    | Purpose                                          |
|-----------|--------------------------------------------------|
| `jablib`  | Core library — logic, model, importers/exporters |
| `jabgui`  | JavaFX desktop GUI                               |
| `jabkit`  | CLI application                                  |
| `jabls`   | Language Server Protocol implementation          |
| `jabsrv`  | HTTP server for collaborative database support   |

Key source paths:

- `jablib/src/main/java/org/jabref/logic/` — business logic
- `jablib/src/main/java/org/jabref/model/` — data model
- `jabgui/src/main/java/org/jabref/gui/` — GUI code
- `docs/` — developer documentation and ADRs

---

## Build

```bash
./gradlew build              # Build all modules
./gradlew :jabui:run         # Build and launch the GUI
./gradlew :jabgui:jpackage   # Package as installer
```

Requries JDK 17 or later to run gradle.
Gradle downloads the necessary JDK by itself.
Gradle wrapper is included.

---

## Tests

```bash
# Run all checks for the core library (recommended during development)
./gradlew :jablib:check

# Full check (all modules)
./gradlew check

# Per-module
./gradlew :jablib:test
./gradlew :jabgui:test

# Single test class
./gradlew test --tests "org.jabref.logic.l10n.LocalizationConsistencyTest"

# Coverage report (output: build/reports/jacoco/test/html/index.html)
./gradlew jacocoTestReport
```

Tests requiring external resources have dedicated tasks:

- `./gradlew databaseTest` — requires PostgreSQL
- `./gradlew fetcherTest` — hits live external APIs

---

## Linting

Run before committing:

```bash
./gradlew checkstyleMain checkstyleTest checkstyleJmh
./gradlew modernizer
./gradlew --no-configuration-cache :rewriteDryRun || git diff
./gradlew javadoc
npx markdownlint-cli2 "docs/**/*.md"
npx markdownlint-cli2 "*.md"
```

---

## Key conventions (summary — full rules in AGENTS.md)

- **No Swing** — JavaFX only
- **No `null` returns** from public methods — use `Optional`; annotate with JSpecify (`@NullMarked`, `@Nullable`, `@NonNull`)
- **No `catch (Exception e)`** — catch specific exceptions; log with `LOGGER.debug/info/warn/error(msg, e)`
- **No unchecked exceptions** (`RuntimeException`, `IllegalStateException`) — avoids tearing down the whole app
- **Localization** — all UI strings via `Localization.lang("text")`; FXML strings prefixed with `%`. Add to `jablib/src/main/resources/l10n/JabRef_en.properties` - do not touch other `.properties` files.
- **Tests** — use `assertEquals` not `assertTrue`; use `@TempDir`; no `@DisplayName`; plain JUnit, not AssertJ
- **BibEntry in tests** — use `.withField(...)` not `.setField(...)`
- **GUI dialogs** — use `dialogService`, not native `FileChooser`

---

## Requirements tracing (OpenFastTrace)

JabRef uses [OpenFastTrace](https://github.com/itsallcode/openfasttrace) to trace requirements to implementation and tests.

For a new feature or significant bug fix, **at minimum add the requirement** to the appropriate `docs/requirements/<area>.md` file. Full tracing (`Needs: impl` + implementation comments) is encouraged but can be skipped if the effort is disproportionate.

**Defining a requirement** in `docs/requirements/<area>.md`:

```markdown
### Example
`req~ai.example~1`

Description of the requirement.
```

The identifier must follow the heading with no blank line between them. Add `<!-- markdownlint-disable-file MD022 -->` at the end of the file.

**Optionally — linking an implementation** to a requirement (full trace):

```markdown
Needs: impl
```

```java
// [impl->req~ai.example~1]
```

**Checking coverage:**

```bash
./gradlew traceRequirements   # output: build/tracing.txt
```

See `docs/requirements/` for existing requirements and `docs/requirements/index.md` for full guidance.

---

## Architecture decisions (MADR)

When a significant design or implementation decision is made, create a new MADR in `docs/decisions/`:

1. Copy `docs/decisions/adr-template.md` to `docs/decisions/<NNNN>-<short-title>.md` (next free number).
2. Fill in **Context and Problem Statement**, **Considered Options**, and **Decision Outcome**.
3. Add an entry to `docs/decisions/index.md`.

See [ADR-0000](docs/decisions/0000-use-markdown-architectural-decision-records.md) for the rationale and [adr-template.md](docs/decisions/adr-template.md) for the full template.

---

## Developer documentation

- [devdocs.jabref.org](https://devdocs.jabref.org/) — full developer reference
- `docs/getting-into-the-code/` — workspace setup, code style, IntelliJ config
- `docs/code-howtos/` — localization, testing, fetchers, tools
- `docs/decisions/` — Architecture Decision Records

<!-- markdownlint-disable-file MD033 MD041 -->
