---
nav_order: 0065
parent: Decision Records
---
# Accept an http(s)/ftp URL as `jabkit` Input File

## Context and Problem Statement

`jabkit` commands read their input file via the shared `InputOption` mixin (positional `FILE` argument or `--input` alias, see [ADR 57](0057-allow-positional-input-file-argument.md)). Both forms are typed as `java.nio.file.Path`, converted eagerly at parse time. Passing a URL (e.g. `jabkit convert --input https://example.org/refs.bib`) silently fails: the URL string is mangled into a bogus relative path and reported as "Unable to open file", even though this is documented elsewhere as supported behavior and JabRef already has a working, unrelated precedent for it (`--importToOpen` in the GUI CLI, `jabgui`'s `CliImportHelper`).

A first attempt at this exact feature already existed in the codebase as an unused overload, `ImportService.importFile(String, ...)`: it detects `http://`/`https://`/`ftp://` prefixes and downloads via the existing `URLDownload` utility (`jablib`), but no command ever called it - it was dead code.

How should `jabkit` resolve a URL passed as input, in a way that covers every command consistently and fits the existing CLI error-handling design ([ADR 63](0063-use-picocli-exceptionhandler.md))?

## Decision Drivers

* Consistency across all `InputOption`-consuming commands (`convert`, `pdf-update`, `search`, `check-consistency`, `check-integrity`, `pseudonymize`, `generate-citation-keys`, `generate-bib-from-aux`)
* Reuse the existing `URLDownload` utility instead of writing a second download implementation
* Fit the centralized `CliException`/`IExecutionExceptionHandler` error model from ADR-0063 instead of introducing a second, parse-time error path
* Minimal surface: only download for the `FILE`/`--input` argument, not for output-file options that happen to share `CygWinPathConverter`

## Considered Options

* Detect and download the URL inside a picocli `ITypeConverter<Path>` used by `InputOption`'s fields
* Keep the fields as raw `String` and resolve (Cygwin path conversion or URL download) inside `InputOption#getInputFile()`, called from each command's `call()`
* Special-case URL handling only in the `convert` command

## Decision Outcome

Chosen option: "resolve inside `InputOption#getInputFile()`", because a picocli type converter runs during argument parsing, before `call()` executes; exceptions thrown there bypass the `CliExceptionHandler`/`CliException` machinery introduced in ADR-0063 and would instead surface as generic picocli parameter errors with the wrong exit code. Resolving inside `getInputFile()` (called at the top of every command's `call()`) lets a download failure throw the existing `ImportServiceException` with `CommandLine.ExitCode.SOFTWARE`, exactly like every other import failure.

Because `InputOption` is the single shared mixin behind all eight input-taking commands, every one of them gains URL support automatically, with no per-command changes.

### Consequences

* Good, because all eight `InputOption`-consuming commands support URLs identically, for free.
* Good, because failures are reported through the existing `CliException`/exit-code model (ADR-0063) instead of a second error path.
* Good, because it reuses `URLDownload.toTemporaryFile()` - no new download logic was written.
* Neutral, because the previously-dead `ImportService.importFile(String, ...)` overload was deleted rather than kept alongside the new path, to avoid two divergent implementations of the same scheme check.
* Bad, because `InputOption`'s fields moved from `Path` to `String`, so `--output`/other file options (which stay on `CygWinPathConverter`) and `--input`/`FILE` now follow different resolution code paths for what looks like the same kind of argument.
* Bad, because `jabkit` (and, transitively, any server process embedding this code) now fetches attacker-suppliable URLs - a minor SSRF-relevant surface. Mitigated by reusing `URLDownload`, which already enforces `http`/`https`/`ftp` schemes only, a connect timeout, and JabRef's existing TLS trust store.

### Confirmation

Tests in `jabkit` start a local `okhttp3.mockwebserver3.MockWebServer` and exercise `--input https://.../file.bib` for both a successful download and a failed request, asserting the resulting exit code and database content. A code review confirms no `InputOption`-consuming command bypasses the mixin to parse `--input` itself.

## Pros and Cons of the Options

### Type converter (parse-time)

* Good, because it is the smallest structural change (no field-type change).
* Bad, because converter exceptions are wrapped as picocli `ParameterException`s, not routed through `CliExceptionHandler`, giving download failures the "invalid usage" exit code instead of `SOFTWARE`.
* Bad, because a converter cannot easily distinguish "download failed" from "bad path syntax" for exit-code purposes.

### Resolve inside `InputOption#getInputFile()` (chosen)

* Good, because it fits the ADR-0063 error model without changes to it.
* Good, because it is implemented once and shared by all eight commands.
* Neutral, because the mixin's fields change from `Path` to `String`, deferring path/URL resolution from parse time to call time.

### Special-case only `convert`

* Good, because it touches the fewest files.
* Bad, because it recreates the exact per-command inconsistency that ADR-0057 removed - only `convert` would support URLs, contradicting the shared-mixin design.

## More Information

* [ADR 57](0057-allow-positional-input-file-argument.md) - the shared `InputOption` mixin this decision builds on
* [ADR 63](0063-use-picocli-exceptionhandler.md) - the `CliException`/exit-code model this decision reuses
* `org.jabref.logic.net.URLDownload` (`jablib`) - the existing download utility, also used by `jabgui`'s `CliImportHelper` for `--importToOpen`
