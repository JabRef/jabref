# Journal Abbreviations

## Preparation

- Ensure that `buildres/abbrv.jabref.org` contains data. Otherwise, the submodules were not configured correctly.
- Ensure that `jablib/build/generated/resources/journals/journal-list.mv` exists. Gradle task `generateJournalListMV` uses `build-support/src/main/java/AbbreviationListMvGenerator.java` to generate separate journal and conference stores.

## Where does the data come from?

The generator uses all `.csv` files from <https://github.com/JabRef/abbrv.jabref.org/tree/main/journals>, but ignores following ones:

```java
        Set<String> ignoredNames = Set.of(
                // remove all lists without dot in them:
                // we use abbreviation lists containing dots in them only (to be consistent)
                "journal_abbreviations_entrez.csv",
                "journal_abbreviations_medicus.csv",
                "journal_abbreviations_webofscience-dotless.csv",

                // we currently do not have good support for BibTeX strings
                "journal_abbreviations_ieee_strings.csv"
        );
```

## Conference abbreviation example

The example CSV at `jablib/src/test/resources/org/jabref/logic/conferences/conference_abbreviations_example.csv`
uses the same full-name, abbreviation columns as journal lists.
It maps `International Conference on Business Process Management` to `BPM`, as specified in
<https://github.com/JabRef/jabref/issues/12728>.
`ConferenceAbbreviationIntegrationTest` loads this file through `ConferenceAbbreviationLoader`, abbreviates
`booktitle` through `CleanupWorker`, checks the database through `IntegrityCheck`, and expands the title again.
The example is a test resource, so it does not modify the separate `abbrv.jabref.org` submodule or provide a built-in conference list.

## Built-in conference lists

The same generator reads CSV files from `jablib/src/main/abbrv.jabref.org/conferences/`
into `jablib/build/generated/resources/conferences/conference-list.mv`.
The CSV data belongs to the separate `abbrv.jabref.org` repository and needs a separate contribution there.
After that contribution is merged, update the submodule revision to distribute its records with JabRef.
Older submodule revisions without this directory produce an empty conference store; external CSV lists still work.

Conference records use the shared journal lookup rules. This version does not extract years,
publisher prefixes, conference counts, or parenthesized acronyms from unlisted titles.
For example, an explicit CSV record can map the issue's 2023 INDIN title to `21st INDIN`
and the shortest unique form `INDIN`; other annual variants need separate records.

To try the example in the GUI, open Preferences, select Conference abbreviations, and use Open existing list
(the folder button) to select the CSV. Save the preferences, select an entry with the example `booktitle`,
and open Clean up entries. In Journals and book titles, apply a list-based abbreviation to obtain `BPM`,
or apply Expand journals and book titles to restore the full name. The change supports undo.
LTWA affects journal names only. Conference lists can be created and edited on their preferences page;
removing a list removes its configuration rather than deleting its CSV file.

## Future work

See <https://github.com/JabRef/jabref-issue-melting-pot/issues/41>
