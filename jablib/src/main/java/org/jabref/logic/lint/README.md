# JabFix

JabFix is a linter and formatter for BibTeX libraries.
Each check is a rule with a stable id that reports findings and, where a safe repair exists, attaches it.
`--check` and formatting therefore run the same code and cannot disagree.
Defaults will be based on a study of `.bib` files on GitHub.

## What is here

```text
JabFix.java          applies a RuleSet to the entries of a library
JabFixResult.java    findings + the field changes the repairs made
JabFixMode.java      the modes: named sets of rules with their modes
JabFixConfiguration  what a run applies, layer by layer, and configuring a library
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
databaseContext.getMetaData().setFormatting(
        LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.FIX));

BibDatabaseWriter writer = new BibDatabaseWriter(...);
writer.writeDatabase(databaseContext);
List<Finding> findings = writer.getFindings();
```

The writer resolves the settings into a rule set itself: `JabFixConfiguration.applied` puts the layers below together, and `RuleSet.configuredBy` builds the rules they name, each going as far as they allow.

`setFormatting` is what the library says about itself, and it is what is written back to the file; `JabFixConfiguration.configure` sets it and moves the library's Save Actions into it on the way.
A command that formats a library hands the rules of its run to the writer with `asFormatterRun` instead, so that `jabkit fix --rule lower-case=off` does not leave the library configured that way, and so that formatting a library does not hand it a configuration it never had.

A run applies four layers, each outranking the ones before it:

1. a mode — `JabFixMode`, a set of rules with their modes that JabFix ships — the run's where it names one, otherwise the library's,
2. the library's Save Actions, while it still has its `saveActions` item: each repairs on its field,
3. the rules the library's configuration names,
4. the rules the run names.

A rule none of them names does not run: JabFix is off by default, and `jabkit fix` warns when nothing is enabled.
A layer outranks another per rule — a selector about every field replaces everything the layer below says about that rule, one about a field only that field — so a rule only the library names still runs when the command line names others.
The Save Actions rank right below the configuration's rules, which is why moving them into it changes nothing a run applies.
The modes are placeholders for now.

`LintSettings` holds the mode and one `RuleMode` per `RuleSelector` — a rule, on one field or on every field it looks at:

| Mode    | What the rule does                                      |
|---------|---------------------------------------------------------|
| `OFF`   | does not run — the same as not naming it, said out loud |
| `CHECK` | reports what it finds and changes nothing               |
| `FIX`   | reports what it finds and repairs it                    |

So a library can have three rules report and three others repair, in one place per rule.
Every formatter a Save Action can use is a rule too, by its key in kebab-case (`title:lower-case`), whether or not the library has such a Save Action.
Naming a field narrows an entry to that field, and the more specific entry wins, so a library can have a rule repair everywhere and only report on its titles.
A selector naming `all` or `all-text-fields` is about every field the rule visits, since a Save Action may be configured with those while its findings name the concrete fields it went through.
A rule is wrapped in `ConfiguredRule`, which settles per finding how far the rule goes there — taking the `Fix` off a checked one, so the run handles it the way it already handles a rule that knows no repair: `jabkit fix` says it out loud instead of letting it pass unseen in the output.
This is also where the value a rule takes will go, once rules take any — it belongs to the entry that configures the rule, not to a list beside it.

An id naming no rule of this JabFix is passed over rather than rejected, so that a library configured by a newer JabFix still saves with an older one; an id a user typed is held against the rules instead, which is what makes `jabkit fix --rule typo=off` a usage error.

For such a library the writer applies neither the Save Actions nor its whitespace cleanup on its own; the rules do.
Citation keys are still generated and journals abbreviated on an ordinary save, so that configuring a library does not switch off what its other users rely on; only a formatter run (`jabkit fix`) leaves them out, because it changes nothing that no rule reported.
What a repair changed is reported as a `FieldChange`, the same way a Save Action's change is, so that it can be undone, and every mutation goes through the writer's mutation scheduler, so a GUI can keep them on the JavaFX thread.

A library that carries no settings is saved by the GUI the way it always was; carrying them at all, even an empty block, is what hands it to JabFix.
Layout is normalized by `BibDatabaseWriter` either way, so a library JabFix has already formatted produces no diff.
The exception is an entry nothing changed, which a save writes back as it was read, so `entry-parentheses` replaces the `( )` of `@Article( ... )` in what was read rather than leaving it to the writer.

## Where the settings are stored

In the library, as a `formatting` block of the embedded JSON metadata:

```bibtex
@Comment{jabref-meta-0.1.0
{
  "formatting" : {
    "mode" : "whitespace",
    "rules" : {
      "repeated-whitespace" : "off",
      "title:lower-case" : "check"
    }
  }
}
}
```

`mode` names a `JabFixMode`; one this JabFix does not know is passed over, like a rule it does not know.
A key of `rules` is `rule-id`, or `field:rule-id` for one field; the rule id is what stands after the last colon, since a rule id never contains one while a BibTeX field name may.
The comma lists and `/regex/` items the magic comments above an entry accept are not read here yet, and a key that looks like one is not read at all rather than read as a field name containing a comma.
A mode is spelled in lower case, and only in lower case.

What this JabFix cannot read — a selector spelled some other way, a mode it does not know, or something standing beside the rules — is written back as it came, so a library configured by a newer JabFix is handed back with what it arrived with.

A `saveActions` item that is still there is read beside this block, as the layer right below its rules, and `JabFixConfiguration.configure` moves it into the block.
The `rules` key exists so that the mode can stand beside them; the decision is ADR-0075.

`@Comment{jabref-meta-0.1.0 ...}` is the single embedded JSON object of <https://github.com/JabRef/jabref/issues/10371>.
A `jabref-meta:` item could not hold this: its separator is the same `;` a Save Action uses inside its own value, so nothing that nests fits.

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

A comment that switches nothing off is reported by `magic-comment`, because the entry would be repaired as if it were not there: an id or regex that names no rule JabFix knows, or a field that is neither a BibTeX field nor one the entry carries.
`magic-comment` is a rule like the others, so it runs where a mode or the rules enable it.

## The CLI

`jabkit fix [--check | --in-place] [--mode MODE] [--rule RULE=MODE,...] [--save-config] FILE`.

The CLI is meant to need one knob: pick a mode once with `jabkit fix --mode whitespace --save-config --in-place FILE`, and from then on `jabkit fix --in-place FILE` and JabRef's own saves apply it.

`--mode` takes the place of the library's mode.
`--rule` gives a rule, or a rule on one field as `field:rule`, the mode `off`, `check` or `fix`, spelled as in the stored configuration, and outranks what the library says about that rule.
`--check` is the whole run: every rule reports and nothing is written, whatever the modes say.
`--save-config` stores `--mode` and `--rule` in the library through `JabFixConfiguration.configure`, which moves its Save Actions in as well; the run applies the same either way.

A library that only has Save Actions needs no option at all: `jabkit fix` applies them, and nothing moves them unless `--save-config` is given.

Findings are written one per line, in the `file:line:column:citationKey[:field]: message [rule-id]` format the `check` commands use, so an editor or a CI log scraper can jump to the place at fault.
Where that place is comes from the `ParserResult` of the file the library was read from, through `ErrorFormat`, which the integrity and consistency checks report through as well.

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

- the modes JabFix ships with (the current ones are placeholders),
- built-in rules beyond whitespace,
- library-level rules (`Rule#scan` sees one entry, so `@String( ... )` and `@Preamble( ... )` keep their parentheses),
- context for rules (file directories, abbreviation list, key patterns), which citation key generation and journal abbreviation need before they can become rules,
- rule parameters, so that a rule can take a value and not only a mode,
- a graphical way to configure the block, which would call `JabFixConfiguration.configure` and so migrate the `saveActions` item in passing,
- GUI integration: a save reads the block and writes it back, but nothing in the GUI edits it or shows what a `check` rule reports; Library Properties writes a `saveActions` item back into a migrated library, and a shared database does not synchronize the block,
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
