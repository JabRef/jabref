> [!IMPORTANT]
> This project does not accept fully AI-generated pull requests. AI tools may be used assistively only. You must understand and take responsibility for every change you submit.
>
> Read and follow:
> • [AGENTS.md](./AGENTS.md)
> • [CONTRIBUTING.md](./CONTRIBUTING.md)
# Copilot instructions — JabRef
Purpose
- Short, actionable guidance for Copilot-style assistants working in this repository. Keep responses focused, follow AGENTS.md/CONTRIBUTING.md, and never produce a fully AI-generated PR.

Quick commands
- Build full project: ./gradlew build
- Run GUI (development): ./gradlew :jabgui:run  (root `run` task depends on this)
- Run all checks: ./gradlew check
- Run a module's tests: ./gradlew :<module>:test
- Run a single test class or method: ./gradlew :<module>:test --tests 'com.example.FooTest' or --tests 'com.example.FooTest.testMethod'
- Headless CI-style logic tests (example): CI=true xvfb-run --auto-servernum ./gradlew :jablib:check -x checkstyleJmh -x checkstyleMain -x checkstyleTest -x modernizer
- Linting and static checks (examples):
  - ./gradlew checkstyleMain checkstyleTest checkstyleJmh
  - ./gradlew modernizer
  - ./gradlew --no-configuration-cache :rewriteDryRun || git diff
  - npx markdownlint-cli2 "docs/**/*.md" && npx markdownlint-cli2 "*.md"

High-level architecture (big picture)
- Multi-module Gradle Java project. Major modules:
  - jablib: core logic and libraries (org.jabref.logic, org.jabref.model)
  - jabgui: JavaFX-based GUI
  - jabkit / jabls / jabsrv (+ corresponding -cli modules): CLI and server-related tooling
  - docs/: developer and user documentation (mkdocs site)
- Package layout: org.jabref.model (data model), org.jabref.logic (business logic), org.jabref.gui (UI). Localization files live in src/main/resources/l10n/*.properties and are referenced via Localization.lang(...) calls.
- The root Gradle build config registers a root `run` task which delegates to :jabgui:run.

Key repository conventions (project-specific)
- Java toolchain: Target Gradle toolchain and Java 24+ features. Prefer Path.of, List.of, text blocks, and modern Java idioms.
- UI: Use JavaFX only (no Swing). GUI code should be a thin gateway to org.jabref.logic.
- Concurrency: Do not new Thread(); use org.jabref.logic.util.BackgroundTask and its executeWith helpers.
- Nullability & Optionals: Prefer java.util.Optional and JSpecify annotations (@NonNull/@Nullable). New public methods should not return null.
- Localization: Use Localization.lang("KEY") for user-facing strings; after adding keys run the LocalizationConsistencyTest to update l10n files if tests fail.
- Tests: Prefer JUnit with @TempDir for temp dirs; do not catch exceptions in tests; assert object contents (assertEquals) rather than boolean conditions.
- Adding dependencies: Document new external libraries in external-libraries.md and prefer versions available in JCenter/Maven Central.
- Linting and automated rewrites: OpenRewrite recipes and rewriteDryRun are used. The rewrite plugin runs with failOnDryRunResults=true in CI.
- AI & PR policy: This repo disallows fully AI-generated PRs. Read AGENTS.md and AI_USAGE_POLICY.md before using Copilot or other LLMs. All AI-assisted work must be human-reviewed and attributable.

Useful files to consult
- CONTRIBUTING.md (process, PR checklist)
- AGENTS.md (agent rules and coding conventions)
- AI_USAGE_POLICY.md (AI usage restrictions)
- docs/ and devdocs links referenced in README.md for architecture and localization how-tos

Where to get more context
- The docs/ directory (mkdocs) contains architecture-and-components and code-howtos which explain cross-file design decisions.

Notes for Copilot sessions
- Keep edits minimal and focused; prefer small, reviewable changes.
- Do not submit full PRs generated entirely by an assistant. Always include AI disclosure in PRs per CONTRIBUTING.md.
- When in doubt, cite the relevant file (AGENTS.md or CONTRIBUTING.md) and ask a human.


