---
nav_order: 79
parent: Decision Records
---
# Generate Git commit messages in English only

`adr~english-only-git-commit-messages~1`

Needs: impl

## Context and Problem Statement

The commit dialog should generate a commit message that summarizes what changes since the last commit, for example "Add 3 entries, modify 2 entries" or "Modify author of John2023" when a library is in Git version control ([#16763](https://github.com/JabRef/jabref/issues/16763)).
JabRef generally localizes all text visible to the user automatically.

The problem is that these messages are built at runtime from up to three optional parts, and each part needs its own singular or plural form.

The question is: should these generated messages be localized, and if yes, how it should be done?

## Decision Drivers

* The message has to be grammatically correct English.
* There should be no grammar mistakes like "Add 1 entries" or "modify 1 entries".
* It should stay consistent with JabRef's localization rules ([localization how-to](../code-howtos/localization.md)).
* It should be easy to implement and should not need a new dependency.
* Commit messages end up in the repository history, and every collaborator reads them, even if they use JabRef in a different language.

## Considered Options

* Build the message from separately localized fragments.
* One static localized message with all three counts.
* English-only messages.
* Plural-aware message formats (ICU4J MessageFormat / MessageFormat 2.0).

## Decision Outcome

Chosen option: "English-only messages", because it is the only option that always gives grammatically correct output without adding a new dependency. Furthermore, commit messages are part of the shared repository history, not just one user's interface, and English is the usual convention for Git commit messages anyway.
The generated message is only a suggestion, so users still free edit the messages before committing. This was agreed in the review of [PR #17205](https://github.com/JabRef/jabref/pull/17205).

Because of this, `GitCommitMessageGenerator` builds the message using plain English strings and does not call `Localization.lang(...)`.
Only the surrounding dialog (title, labels, buttons) is localized.

### Consequences

* Good, because every generated message is correct English: "Delete 1 entry", "Add 3 entries, modify 2 entries".
* Good, because everyone sees the same language in `git log`, no matter which language they use in JabRef.
* Good, because no new libraries are needed and the localization consistency tests stay the same.
* Bad, because it is an exception to JabRef's rule that all user-facing text should be localized.
* Bad, because users, who write commit messages in another language receive an English message, although they can still overwrite the suggestions.

### Confirmation

`GitCommitMessageGeneratorTest` checks the exact English output, for example `"Add 3 entries, modify 2 entries"`, `"Delete 1 entry"`, and `"Rename citation key Jones2023 to Jones2024"`.
The code is linked to this decision by the OpenFastTrace tag `[impl->adr~english-only-git-commit-messages~1]` in `GitCommitMessageGenerator`.

## Pros and Cons of the Options

### Build the message from separately localized fragments

Example: localize `"Add %0 entries"`, `"Modify %0 entries"`, `"Delete %0 entries"` separately and join them with `", "`.

* Good, because each fragment is a normal string of text than can be translated
* Bad, because the separator and the word order are hard-coded in Java. In some languages, verbs are placed at the end, or list items are joined differently, which results in grammatically incorrect sentences
* Bad, because one fragment per count cannot express plural rules, for example: Polish uses different forms for 1, for 2–4, and for 5+ entries.
* Bad, because the translators only see fragments without the full sentence, leading to inconsistent translation.

### One static localized message with all three counts

Example: `Localization.lang("Add %0 entries, modify %1 entries, delete %2 entries", add, modify, delete)`.

* Good, because it is one complete sentence that translators can reorder.
* Good, because it follows the localization rules exactly.
* Bad, because it also shows the zero number of changes, as: "Add 0 entries, modify 2 entries, delete 0 entries".
* Bad, because the plural is fixed, leading to the mistake: "modify 1 entries".

### English-only messages

Example: `"Add 3 entries, modify 2 entries"`, `"Delete 1 entry"`, `"Modify author of Smith2024"`.

* Good, because the code can skip zero counts and choose "entry" or "entries" with one simple check.
* Good, because the output is short, correct, and matches common Git conventions.
* Good, because there is no new dependency.
* Bad, because it is not localized.

### Plural-aware message formats (ICU4J MessageFormat / MessageFormat 2.0)

Example pattern: `{count, plural, one {Add # entry} other {Add # entries}}`.

* Good, because each language can define its own correct plural forms.
* Bad, because three optional parts result in 7 different sentence structures for each language (only add, only modify, add + modify, etc...), which the translators have to write them all.
* Bad, because adding new dependency is ICU4J for a single feature.
* Bad, because the localization tools and tests of JabRef only understand the `%0` format and would need to be modified.

## More Information

* Issue: <https://github.com/JabRef/jabref/issues/16763>
* Review decision: <https://github.com/JabRef/jabref/pull/17205>
* <https://stuartgunter.wordpress.com/2011/08/14/even-better-java-i18n-pluralisation-using-icu4j/>
* <https://phrase.com/blog/posts/pluralization/>
* <https://unicode-org.github.io/icu/userguide/format_parse/messages/mf2.html#plural-selection-message>

<!-- markdownlint-disable-file MD022 -->
