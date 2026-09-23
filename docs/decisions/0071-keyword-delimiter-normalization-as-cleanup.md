---
nav_order: 71
parent: Decision Records
---

# Keyword delimiter normalization is a cleanup, applied on load and as a save action

## Context and Problem Statement

JabRef's keyword separator was a global preference. Opening a library whose `keywords` fields used a different character (typically `;` instead of `,`) rewrote every keyword field on load, which broke the promise that JabRef does not change a `.bib` file unless the user edits something. The rewrite lived in the BibTeX importer, outside the cleanup framework, so users could neither see nor disable it.

The keyword separator is now stored per library. The remaining question is where delimiter normalization belongs: on load, on save, in the cleanup framework, or not at all.

## Decision Drivers

* An untouched entry must stay byte-identical on disk.
* Groups, search, and the keyword editor split on the library's separator, so a library mixing delimiters misbehaves until it is consistent in memory.
* Whatever rewrites field contents should be visible and configurable, the same way as every other cleanup.

## Considered Options

* Keep the rewrite in the importer, guarded so consistent fields are untouched
* Normalize only as a save action
* Normalize as a cleanup, applied on load and offered as a save action

## Decision Outcome

Chosen option: "Normalize as a cleanup, applied on load and offered as a save action".

`NormalizeKeywordDelimitersFormatter` is a regular field formatter on the `keywords` field. It returns a field unchanged when the field already uses the library's separator, so it never produces a diff for a consistent library. The importer applies the same formatter to every loaded entry, which keeps the in-memory model consistent; because untouched entries are not written back, this does not change the file. The formatter is also part of the default save actions and of the cleanup dialog, so users see it and can switch it off per library.

Formatters are stateless singletons looked up by key, so they cannot hold a library's separator. Formatters whose result depends on the separator implement `KeywordSeparatorAware`; the save and cleanup paths, which know the library, bind them to the library's separator before running them. An unbound formatter falls back to the global preference.

### Consequences

* Good, because the file on disk changes only for entries the user saved, and only in the way every other save action does.
* Good, because the behavior is discoverable and can be disabled or run on demand.
* Bad, because load is not strictly verbatim: entries with mixed delimiters are normalized in memory before the user touches them. This is accepted for user convenience; the alternative (save action only) reintroduces wrong group membership and search results for mixed libraries.

## Pros and Cons of the Options

### Keep the rewrite in the importer, guarded

* Good, because it is the smallest change.
* Bad, because the rewrite stays invisible and cannot be disabled.

### Normalize only as a save action

* Good, because load is verbatim.
* Bad, because mixed libraries stay inconsistent in memory until every entry has been saved once.

### Normalize as a cleanup, applied on load and offered as a save action

* Good, because one implementation serves load, save, and the cleanup dialog.
* Bad, because the load step is a deliberate exception to "load is verbatim" and needs the no-change guard to be safe.
