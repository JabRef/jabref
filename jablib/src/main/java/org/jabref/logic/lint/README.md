# JabFix

JabFix is a linter and formatter for BibTeX libraries.
Each check is a rule with a stable id that reports findings and, where a safe repair exists, attaches it.
`--check` and formatting therefore run the same code and cannot disagree.
Defaults will be based on a study of `.bib` files on GitHub.

## What is here

```text
JabFix.java          applies a RuleSet to the entries of a library
JabFixResult.java    findings + the field changes the repairs made
rule/                API: Rule, Finding, Fix, RuleSet, FieldValueRule, CleanupRule, SaveActionRule,
                     MagicCommentRule, Suppressions
rules/               the built-in rules
```

A `FieldValueRule` only states what a field value should be.
A `CleanupRule` runs one of JabRef's `CleanupJob`s as a rule, which is how the cleanups a save has always applied become reportable; `SaveActionRule` is the one identified by the formatter key it is configured under.

Rules run once each, in `RuleSet` order, and must be idempotent.

## Who applies the rules

`BibDatabaseWriter` does, for a library that asks for it.
The asking is `LintSettings` in the library's `MetaData`, next to its Save Actions:

```java
databaseContext.getMetaData().setLintSettings(new LintSettings(Map.of(
        "surrounding-whitespace", RuleMode.FIX)));

BibDatabaseWriter writer = new BibDatabaseWriter(...);
writer.writeDatabase(databaseContext);
List<Finding> findings = writer.getFindings();
```

The writer resolves the settings into a rule set itself, with `RuleSet.forLibrary`, which carries the library's own Save Actions into the run, and `RuleSet.asConfiguredBy`, which lets each rule go as far as the library allows.

`LintSettings` holds one `RuleMode` per rule id, and a rule runs only where the library names it:

| Mode    | What the rule does                                      |
|---------|---------------------------------------------------------|
| `OFF`   | does not run — the same as not naming it, said out loud |
| `CHECK` | reports what it finds and changes nothing               |
| `FIX`   | reports what it finds and repairs it                    |

So a library can have three rules report and three others repair, in one place per rule.
A library that carries no settings has nothing done to it, which is how every library written before this reads.
A checked rule is wrapped by `CheckOnlyRule`, which takes the `Fix` off its findings, so the run then handles them the way it already handles a rule that knows no repair: `jabkit fix` says them out loud instead of letting them pass unseen in the output.
This is also where the value a rule takes will go, once rules take any — it belongs to the entry that configures the rule, not to a list beside it.

An id naming no rule of this JabFix is passed over rather than rejected, so that a library configured by a newer JabFix still saves with an older one; an id a user typed is held against the rules instead, which is what makes `jabkit fix --disable typo` a usage error.

For such a library the rules are the only thing that changes an entry: the writer applies neither the Save Actions nor its whitespace cleanup on its own, and it generates no citation keys and abbreviates no journals, because nothing may be changed that no rule reported.
What a repair changed is reported as a `FieldChange`, the same way a Save Action's change is, so that it can be undone, and every mutation goes through the writer's mutation scheduler, so a GUI can keep them on the JavaFX thread.

A library that carries no settings is saved the way it always was; carrying them at all is what asks for anything to be done, so there is no flag to switch off.
Layout is normalized by `BibDatabaseWriter` either way, so a library JabFix has already formatted produces no diff.

`LintSettings` are not written to the `.bib` file yet — `jabkit fix` sets them on the library it has just read.
Storing them in a `jabref-meta` entry is the next step.

## Switching rules off for one entry

A comment directly above an entry switches rules off for it:

```bibtex
% jabref-format-ignore surrounding-whitespace author,title:page-ranges /comment-.*/:/normalize-.*/
@Article{knuth1984,
  ...
}
```

A token without a colon covers the whole entry, `fields:rules` only those fields.
Both sides are comma-separated lists, and an item between slashes is a regex that has to match a whole field name (ignoring case) or rule id.
A field name may itself contain a colon (`note:de`); where the entry has such a field, its name is read as a whole.

`Suppressions` reads the comments of an entry and `JabFix` drops what they cover before the repairs are applied, so a suppressed finding is neither reported nor repaired.

A comment that switches nothing off is reported by `magic-comment`, because the entry would be repaired as if it were not there: an id or regex that names no rule of the run, or a field that is neither a BibTeX field nor one the entry carries.

## The CLI

`jabkit fix [--check | --in-place] [--enable RULE,...] [--disable RULE,...] [--check-only RULE,...] FILE`.

Findings are written one per line, in the `file:line:column:citationKey[:field]: message [rule-id]` format the `check` commands use, so an editor or a CI log scraper can jump to the place at fault.
Where that place is comes from the `ParserResult` of the file the library was read from, through `ErrorFormat`, which the integrity and consistency checks report through as well.

`--enable`, `--disable` and `--check-only` set the mode of a rule for that run, and cover the library's Save Actions too, since they are rules of the run.
Without `--enable` the command names every rule the library has, which is what makes a bare `jabkit fix` format it; with it, the named rules are the only ones that run.
`--check-only` names a rule as well, so one left out of `--enable` still reports what it finds.
A rule named by several of them is switched off: the strictest wins.
`--check` is the whole run: every rule reports and nothing is written, whatever the modes say.

## Goal

JabRef has four features that judge or change how a library is written, each configured differently:

| Feature           | Reports | Fixes | Configured in      |
|-------------------|---------|-------|--------------------|
| Cleanup entries   | no      | yes   | user preferences   |
| Save actions      | no      | yes   | library metadata   |
| Check integrity   | yes     | no    | fixed checker set  |
| Check consistency | yes     | no    | not configurable   |

The goal is to turn all four into JabFix rules, configured with the library and applied the same way by the GUI, JabKit and CI.
Integrity checkers become report-only rules, cleanup jobs and Save Actions formatters become rules with fixes, and the consistency check becomes a library-level rule.

Still missing:

- built-in rules beyond whitespace,
- library-level rules (`Rule#scan` sees one entry),
- context for rules (file directories, abbreviation list, key patterns), which citation key generation and journal abbreviation need before they can become rules,
- configuration beyond the command line and the comments above an entry, including rule parameters and where the settings are stored,
- GUI integration: `BibDatabaseWriter` reads the settings, but no GUI save writes them yet,
- leaving out metadata JabRef only inferred (the database type); writing it back changes libraries that are otherwise clean.

## Consolidation into JabFix

Options of integrating JabFix into JabRef.

### Context

The four features overlap, are configured separately, and split reporting from fixing.
A team cannot define its conventions once and have them applied everywhere.

### Options

1. Add JabFix as a fifth feature.
2. Keep the implementations, but read settings from one shared file.
3. Re-implement all four as JabFix rules (proposed).

Options 1 and 2 are smaller, but keep reporting and fixing in separate code.

### Pros

- One configuration, versioned with the project.
- Every rule can report and fix: Check integrity gains fixes, Save Actions gain a check mode.
- Checks can be disabled individually by id.
- Existing formatters, checkers and output writers can be wrapped rather than rewritten, as `CleanupRule` does for JabRef's cleanup jobs.

### Cons

- Large change across jablib, jabgui and jabkit (about 40 checkers, over 20 cleanup jobs); needs several pull requests.
- The rule API needs library-level scope and injected context.
- Some cleanup jobs are not about style (moving linked files, XMP metadata, biblatex conversion). Either the scope grows or the consolidation stays partial.
- Settings already stored in libraries and preferences need a precedence and a migration.
- One rule model has to serve an on-demand dialog, a silent step on save, and a list of messages.
- `jabkit check integrity` and `jabkit check consistency` need a deprecation path.
