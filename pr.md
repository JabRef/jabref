# Fix package and class name collision across modules (#14052)

## Summary

Resolves split package/class collision noted in [#14052](https://github.com/JabRef/jabref/issues/14052): both `jabgui` and `jabkit` defined `org.jabref.cli.ArgumentProcessor` with different responsibilities. This violates Java module best practices and risks runtime ambiguity.

## Changes

- Rename `jabkit`’s `ArgumentProcessor` → `JabKitArgumentProcessor`
- Rename `jabgui`'s `ArgumentProcessor` → `JabGuiArgumentProcessor`
- Update all imports, constructor calls, and static usages across CLI commands
- Update tests and file names accordingly
- Verified compile and tests for `jabkit`; CLI help smoke test executed via Gradle

### Files of note

- `jabkit/src/main/java/org/jabref/cli/JabKitArgumentProcessor.java`
- `jabgui/src/main/java/org/jabref/cli/JabGuiArgumentProcessor.java`
- `jabkit/src/main/java/org/jabref/JabKit.java`
- CLI commands in `jabkit/src/main/java/org/jabref/cli/*` updated to reference `JabKitArgumentProcessor`
- Tests: `jabkit/src/test/java/org/jabref/cli/JabKitArgumentProcessorTest.java`

## Validation

- Build/compile:
  - `./gradlew :jabkit:compileJava :jabgui:compileJava` (OK)
- Tests:
  - `./gradlew :jabkit:test` (OK)
  - `./gradlew :jabkit:run --args="--help"` (OK; shows command list)
  - `./gradlew :jabgui:test` shows unrelated local failures (journal abbreviations/macOS integration). CI should validate cross-platform.
- Runtime note:
  - If running installed `jabkit` scripts with JDK 23, a `UseCompactObjectHeaders` VM flag may fail. Running via Gradle or with JDK 21 avoids this. CI uses supported toolchains.

## Rationale

- Eliminates split packages in `org.jabref.cli` across modules
- Makes responsibilities explicit (`JabKit*` for CLI toolkit, `JabGui*` for GUI startup/arg handling)
- Reduces risk for module resolution conflicts moving towards stricter module boundaries

## Checklist

- [x] Renamed classes and files
- [x] Updated imports and static usages
- [x] Updated tests and references
- [x] Compiles on `jabkit` and `jabgui`
- [x] `jabkit` tests green; CLI help smoke-tested
- [ ] Let CI validate full matrix (GUI/journal tests are often environment-sensitive)

Fixes #14052
