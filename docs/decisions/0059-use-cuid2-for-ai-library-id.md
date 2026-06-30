---
nav_order: 0059
parent: Decision Records
---
# Use CUID2 for `aiLibraryId`

## Context and Problem Statement

JabRef stores an `aiLibraryId` in the library's metadata to associate AI artifacts (chat history, summaries, embeddings) with a specific `.bib` library across launches.
The id is serialized into the `.bib` file as `@Comment{jabref-meta: aiLibraryId:<id>;}` and is therefore visible to anyone who opens the file in a text editor.
Carrying the id inside the file content (rather than keying off the file path) is what lets AI artifacts stay correlated with the library even when the user renames or moves the `.bib` file.

Because `.bib` files are routinely shared between researchers (e.g., via Git, email, cloud drives, supplementary material of papers), the id ends up in human-facing contexts.
A v4 UUID such as `550e8400-e29b-41d4-a716-446655440000` looks alarming or "machine-y" to a researcher who is just inspecting their references file.

What identifier scheme should we use for `aiLibraryId`?

## Decision Drivers

* The id must be globally unique with negligible collision probability (multiple researchers can independently create libraries; ids must not clash when libraries are merged).
* The id must be stable across JabRef launches and cross-platform.
* The id should look reasonably unobtrusive when a researcher reads the `.bib` file in a text editor — BibTeX files are shared, and the id should not say "WTF".
* The id should be generated locally without contacting a server (consistent with [ADR-0034](0034-use-citation-key-for-grouping-chat-messages.md): no server is available).
* Prefer a modern, actively maintained scheme.

## Considered Options

* `UUID.randomUUID()` (RFC 4122 v4 UUID).
* [CUID2](https://github.com/paralleldrive/cuid2).
* Short hash of the file path / first entry.

## Decision Outcome

Chosen option: **CUID2**, because it offers the same collision-resistance guarantees as a v4 UUID while producing a shorter, lowercase, alphanumeric string that is far less jarring inside a shared `.bib` file.
The Java port `io.github.thibaultmeyer:cuid` is on the dependency graph, and its v2.x line implements the CUID2 specification.

`AiService.ensureAiLibraryIdPresent` generates the id via the CUID2 generator.
The id remains an opaque `String` from the rest of the code's perspective, so no API changes propagate beyond that call site.

### Consequences

* Good, because the id is shorter (~24 chars instead of 36) and lowercase alphanumeric, which reads better in a shared `.bib` file.
* Good, because CUID2 is explicitly designed to be collision-resistant for horizontally-distributed generation, which matches our case (every JabRef install generates ids independently).
* Good, because CUID2 is, by design, hard to guess — slightly better than v4 UUIDs against fingerprinting if an id ever leaks into a URL or log.
* Bad, because we carry a small dependency surface compared to the JDK-builtin `UUID`.
* Bad, because CUID2 is less universally recognized than UUID — a developer encountering one for the first time may need a moment to identify the format.

### Confirmation

The serialization round-trip tests (`BibDatabaseWriterTest.writeAiLibraryId`, `MetaDataParser`) treat the value as an opaque string and pass with a CUID2 value.
A code review of `AiService.ensureAiLibraryIdPresent` confirms the CUID2 generator is the only source of new ids.

## Pros and Cons of the Options

### `UUID.randomUUID()`

Example: `550e8400-e29b-41d4-a716-446655440000`.

* Good, because it is built into the JDK — no extra dependency.
* Good, because it is universally recognized.
* Neutral, because collision probability is negligible (122 random bits).
* Bad, because the canonical form (`8-4-4-4-12` hex with hyphens) is long and visually noisy in a `.bib` file shared with researchers.
* Bad, because it conveys a "this is a generated machine token" feeling that is at odds with the otherwise human-readable nature of `.bib` files.

### CUID2

Example: `tz4a98xxat96iws9zmbrgj3a`.

Java port used: [thibaultmeyer/cuid-java](https://github.com/thibaultmeyer/cuid-java).

* Good, because the textual form is shorter and lowercase alphanumeric, blending in with other identifiers researchers already see (citation keys, DOIs).
* Good, because the spec is explicit about collision resistance under distributed generation.
* Good, because it is a modern, actively maintained scheme (the original CUID has been deprecated in favor of CUID2).
* Good, because already used in indexing and OpenOffice integration.
* Bad, because it is one more dependency to track.
* Bad, because it is slightly less familiar to developers than UUID.

### Short hash of the file path / first entry

Example: `a3f1c9d2` (CRC32 / truncated SHA-1 of the absolute path).

* Good, because it is deterministic — moving a `.bib` file would not orphan its AI artifacts.
* Bad, because it is not unique: two libraries can share a citation key, and file paths change.
* Bad, because if a user copies a library, both copies would point at the same AI artifacts — exactly what `aiLibraryId` is meant to prevent.
* Bad, because the id would change if the underlying input changes, breaking the stability requirement.

## More Information

Implementation site: `AiService.ensureAiLibraryIdPresent` in `jablib/src/main/java/org/jabref/logic/ai/AiService.java`.
