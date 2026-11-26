---
parent: Code Howtos
---
# JabFix

JabFix is the linter and formatter for BibTeX libraries in `org.jabref.logic.lint`.
It runs when a library is saved and as `jabkit fix`.
Each check is a rule with a stable id that reports findings and, where a safe repair exists, attaches it to the finding.
`jabkit fix --check` and formatting therefore run the same code and cannot disagree.

## Rules

A `Rule` scans one entry and returns `Finding`s, each of which may carry a `Fix`.
`JabFix` runs the rules of a `RuleSet` over each entry in order and applies the fixes.
Every rule runs once per entry, so it has to be idempotent.

- `FieldValueRule` is the base for a rule that only states what a field value should be.
- `CleanupRule` runs one of JabRef's `CleanupJob`s as a rule, which is how the whitespace cleanups of a save become reportable.
- `SaveActionRule` runs a Save Action formatter. Its id is the formatter key in kebab-case, such as `lower-case`, so every formatter is a rule, whether or not the library has such a Save Action.
- `MagicCommentRule` (`magic-comment`) reports a magic comment that switches nothing off.

`RuleSet.builtIn` lists the standalone rules JabFix ships, which are in `org.jabref.logic.lint.rules`, and `RuleSet.known` adds a rule per Save Action formatter.
`jabkit fix --help` lists all rules and modes with their descriptions.

## Applying the rules on save

`BibDatabaseWriter` applies the rules to a library whose metadata carries a JabFix configuration (`MetaData#getFormatting`), even an empty one.
A library without one is saved as before.

```java
databaseContext.getMetaData().setFormatting(
        LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.FIX));

BibDatabaseWriter writer = new BibDatabaseWriter(...);
writer.writeDatabase(databaseContext);
List<Finding> findings = writer.getFindings();
```

For such a library, the writer applies neither the Save Actions nor its own whitespace cleanup; the rules do.
Citation keys are still generated and journals abbreviated, so that configuring a library does not switch off what its other users rely on.
Repairs are reported as `FieldChange`s, so that they can be undone, and go through the writer's mutation scheduler, so that the GUI can keep them on the JavaFX thread.

`jabkit fix` calls `BibDatabaseWriter#asFormatterRun` instead.
The rules of the run then apply to this save only and are not written to the library, and no citation keys or journal abbreviations are added, since a formatter run changes nothing that no rule reported.

`BibDatabaseWriter` normalizes the layout of every entry it writes anew, so a library JabFix has formatted produces no diff.
An entry that nothing changed is written back as it was read, brackets included.
`entry-brackets` therefore repairs the brackets of the entry as read, and the writer encloses the entries it writes anew in the bracket this rule is configured with, wherever the rule runs, and in braces elsewhere.

## Configuration

`LintSettings` holds what a library or a run asks for: optionally a mode, and per `RuleSelector` a `RuleSettings` with a `RuleMode` and the values the rule takes.
A selector is a rule id, or `field:rule-id` for one field.

| Mode    | What the rule does                     |
|---------|----------------------------------------|
| `off`   | does not run                           |
| `check` | reports what it finds, changes nothing |
| `fix`   | reports what it finds and repairs it   |

A rule that nothing names does not run, so JabFix is off by default; naming a rule `off` states this explicitly.
`ConfiguredRule` applies the mode per finding and removes the fix from a finding of a rule set to `check`.
Where two selectors cover the same field, the more specific one wins, so a library can have a rule repair everywhere and only report on its titles.
`all` and `all-text-fields` count as every field, since Save Actions use them.

`JabFixConfiguration.applied` combines four layers into what a run applies, each outranking the ones before it:

1. a mode (`JabFixMode`): the run's if it names one, otherwise the library's,
2. the library's Save Actions, while it still has a `saveActions` item,
3. the rules of the library's configuration,
4. the rules of the run (`jabkit fix --rule`).

A layer outranks another per rule, so a rule only the library names still runs when the command line names others.
A layer that only states a mode keeps the values stated below it.
`JabFixConfiguration.configure` sets a library's configuration and moves its Save Actions into it.
They rank directly below the configuration's rules, so moving them does not change what a run applies.

Values reach a rule through `Rule#configuredWith`; a rule that cannot read its values is skipped with a warning.
An id that names no rule is skipped as well, so that a library configured by a newer JabRef still saves.
On the command line, an unknown id or value is a usage error instead.
The modes JabFix ships are placeholders for now.

### Storage

The configuration is the `formatting` block of the library's embedded JSON metadata, `@Comment{jabref-meta-0.1.0 ...}` (see <https://github.com/JabRef/jabref/issues/10371>):

```bibtex
@Comment{jabref-meta-0.1.0
{
  "formatting" : {
    "mode" : "whitespace",
    "rules" : {
      "repeated-whitespace" : "off",
      "title:lower-case" : "check",
      "entry-brackets" : {
        "mode" : "fix",
        "bracket" : "parentheses"
      }
    }
  }
}
}
```

`JsonMetaDataParser` reads the block and `JsonMetaDataSerializer` writes it.
Whatever this JabRef cannot read in the block is written back unchanged.
[ADR-0086](../decisions/0086-per-rule-modes-in-the-formatting-configuration.md) describes the format and why it was chosen.

## Switching rules off for one entry

A comment directly above an entry switches rules off for that entry:

```bibtex
% jabref-format-ignore surrounding-whitespace author,title:repeated-whitespace /comment-.*/:/normalize-.*/
@Article{knuth1984,
  ...
}
```

A token without a colon covers the whole entry; `fields:rules` covers only those fields.
Both sides are comma-separated lists, and an item between slashes is a regular expression that has to match a whole field name (ignoring case) or rule id.
`Suppressions` parses these comments, and `JabFix` drops the findings they cover before applying the fixes, so a suppressed finding is neither reported nor repaired.
`magic-comment` reports a comment that switches nothing off, for example one naming no known rule.

## Command line

`jabkit fix` (`JabFixCommand`) writes the formatted library to standard output, or with `--in-place` back to the file.
With `--check`, it only reports, and exits with 1 unless the library is clean.
`--mode` and `--rule` override the library's configuration for one run; `--save-config` stores them in the library instead:

```shell
jabkit fix --mode whitespace --save-config --in-place references.bib
jabkit fix --in-place references.bib
```

After the first command, the library carries the configuration, so the second command and JabRef's saves apply it without options.
A library that only has Save Actions needs no options either: `jabkit fix` applies them.

Findings are written one per line as `file:line:column:citationKey[:field]: message [rule-id]`, the format of the `jabkit check` commands.
The requirements in [cli.md](../requirements/cli.md) and [save.md](../requirements/save.md) state the expected behavior.

## Outlook

JabRef has four features that judge or change how a library is written, each configured differently:

| Feature           | Reports | Fixes | Configured in     |
|-------------------|---------|-------|-------------------|
| Cleanup entries   | no      | yes   | user preferences  |
| Save Actions      | no      | yes   | library metadata  |
| Check integrity   | yes     | no    | fixed checker set |
| Check consistency | yes     | no    | not configurable  |

The goal is to turn all four into JabFix rules, configured with the library and applied the same way by the GUI, JabKit, and CI:
integrity checkers become report-only rules, cleanup jobs and Save Action formatters become rules with fixes, and the consistency check becomes a library-level rule.
Adding JabFix as a fifth feature, or keeping the implementations and sharing only their settings, would be smaller, but would keep reporting and fixing in separate code.

The consolidation is a large change across jablib, jabgui, and jabkit, with about 40 integrity checkers and 20 cleanup jobs.
Some cleanup jobs are not about style (moving linked files, XMP metadata, biblatex conversion), so it may stay partial.
Settings already stored in libraries and preferences need a migration, and `jabkit check integrity` and `jabkit check consistency` need a deprecation path.

Open work:

- the modes JabFix ships, based on a study of `.bib` files on GitHub,
- built-in rules beyond whitespace,
- library-level rules: `Rule#scan` sees one entry, so `entry-brackets` leaves `@String` and `@Preamble` as they were read, and the writer encloses those it writes anew in braces, whichever bracket the rule is configured with,
- context for rules (file directories, abbreviation list, key patterns), which citation key generation and journal abbreviation need before they can become rules,
- rule values other than strings,
- GUI support: nothing in the GUI edits the configuration or shows what a `check` rule reports, editing the Save Actions in Library Properties writes a `saveActions` item back into a migrated library, and a shared database does not synchronize the configuration,
- not writing metadata that JabRef only inferred, such as the database type, which changes libraries that are otherwise clean.
