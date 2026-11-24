---
nav_order: 75
parent: Decision Records
status: "accepted"
---
# Per-rule modes in the formatting configuration
`adr~per-rule-modes-in-the-formatting-configuration~1`

Needs: utest

## Context and Problem Statement

JabFix applies rules when a library is saved, and a rule runs only where the library asks for it: JabFix does nothing to a library that says nothing, and a rule the configuration does not name does not run. Asking has two degrees — a rule can report what it finds and leave the value alone, or report and repair.

The configuration is therefore a list of what to run, not a list of what to suppress. That is the opposite of how most linters read, where a shipped set is on and the configuration switches parts of it off, and it is what makes the shape a question worth settling before anything is written to a file.

The configuration is stored in the library's metadata as JSON, next to the Save Actions it already carries (<https://github.com/MikaNeumann/jabref-jabfix/issues/1>, and <https://github.com/JabRef/jabref/issues/10371> for turning `jabref-meta` into JSON as a whole).

Rules will also take values. `surrounding-whitespace` needs none, but a future rule will want a maximum length, a preferred page-range separator, or a list of fields to skip. Whatever shape the configuration takes now has to hold those without being rebuilt.

So: how does the stored configuration say that one rule only reports while another repairs?

## Decision Drivers

* One rule, one answer. The file should not be able to say two contradictory things about the same rule.
* Values are coming, and they belong to the rule they configure. Where they go should follow from the shape rather than need a new place.
* The configuration is read and edited by hand: a `.bib` file is a text file people review in diffs.
* Rules are off until named, so the file is read as "what does this library run?". The shape should answer that question directly, and should have an answer for "run everything JabFix ships with" that is not a list of every id.
* An older JabRef has to save a library configured by a newer one. An unknown rule id, and an unknown mode, are passed over rather than rejected.
* The stored shape should map to the in-memory model without a translation layer that exists only for the file format.
* A library also switches rules off for a single entry, through the magic comments above it. Those are suppressions of something already switched on, which is a different question from what this file answers; the two need not look alike, but they should not contradict each other in spirit.

## Considered Options

* One list per mode
* A mode per rule, as the rule's value, opening into an object where the rule takes values
* A mode per rule, always an object
* A list of rule objects

## Decision Outcome

Chosen option: "A mode per rule, as the rule's value, opening into an object where the rule takes values", because it is the only shape in which one rule cannot be configured twice, it gives the values rules will take a place that follows from the shape rather than a collection of their own, and it is what readers already know from other linters.

```json
"formatting": {
  "rules": {
    "title-case": "check",
    "surrounding-whitespace": "fix",
    "title:lower-case": "fix",
    "line-length": {"mode": "check", "max": 120}
  }
}
```

Four things this fixes beyond the choice of shape:

* The block is named `formatting`, not after JabFix. A library says what it wants done to it, not which part of JabRef does it, and the name survives the component being renamed or absorbed.
* The rules sit under a `rules` key rather than directly in the block, so that the block has room for something that is not a rule -- a named preset, say -- without the reader having to know which keys are rule ids and which are not.
* There is no `enabled` flag: the block's presence is the switch. A library that wants nothing done to it carries no block, which is also the state of every library written before this existed.
* A rule the block does not name does not run. Naming one `"off"` says the same thing out loud, which is worth having where a library wants to record that a rule is deliberately not run.

### What a key selects

A key is `rule-id`, which is about the rule wherever it looks, or `field:rule-id`, which is about that field alone. A Save Action is bound to a field, and Save Actions are entries of this block like any other rule, so a key that could not name a field would not be able to express one. It also lets a library repair everywhere and only report on a single field, which is a thing libraries want for its own sake.

The rule id is what stands after the **last** colon: a rule id is kebab-case and never contains one, while a BibTeX field name may, so `note:de:lower-case` reads the way a person would expect. The magic comments above an entry tell the two apart by asking which fields that entry carries, which is a thing a library-wide key has no access to.

Where two keys are about the same rule and the same field, the more specific one wins: a key naming the field beats one naming none, so a narrow exception may do more or less than the broad entry beside it. A key naming `all` or `all-text-fields` is about every field the rule visits, since a Save Action may be configured with those and its findings are about the concrete fields it went through.

The comma-separated lists and `/regex/` items that the per-entry magic comments accept are deliberately **not** read here yet. They may be one day; until then a key that looks like one is not read at all, rather than read as a field name that happens to contain a comma, so that the same key cannot quietly mean one thing now and another later.

A mode is spelled in lower case, and only in lower case. Taking `FIX` as well would mean writing a file back differently from how it was read, for a library that did nothing wrong.

### What an entry holds

An entry is a mode, or an object holding the mode under `mode` and, beside it, the values the rule takes under their own names:

```json
"formatting": {
  "rules": {
    "surrounding-whitespace": "fix",
    "entry-brackets": {"mode": "fix", "bracket": "parentheses"}
  }
}
```

`entry-brackets` takes one value, `bracket`, which is `braces` or `parentheses`. A rule that takes a value says what it does without one, so `"entry-brackets": "fix"` stays valid and goes by braces.

An entry is written back as an object only where it holds values, and then with its mode first. `{"mode": "fix"}` therefore reads like `"fix"` and comes back as that: such a file is put into this form once, like one whose keys somebody reordered by hand.

An entry is not read, and goes back as it came without the rule running, where it is

* an object without a mode, or with a mode this JabFix does not know, since nothing then says how far the rule goes,
* an object holding anything but strings, such as the `max` above. No rule takes anything else yet, and running a rule without a value it was given would be doing something the library did not ask for,
* an object holding values under a key naming a field. A rule is built once for a run and not once per field, so they could not be acted on, and the same key could quietly mean one thing now and another later.

What a value means is up to the rule. A rule that cannot read what it is given, because it takes no value of that name or does not know that value, is passed over with a warning rather than run without it: `"bracket": "angle"` may come from a newer JabFix and `"brackets": "parentheses"` may be a typo, and repairing towards braces would rewrite, on every save, a library that asked for something else. The entry is written back as it came.

A layer stating a mode alone leaves the values stated below it standing, as ESLint does for a severity on its own, and one stating a value replaces that value alone. `jabkit fix --rule entry-brackets=check` therefore reports against the bracket the library chose rather than against braces, and `--save-config` stores it that way.

The command line states values after the mode, each as `:name=value`, as in `--rule entry-brackets=fix:bracket=parentheses`. The comma is not available for this, since it separates the rules of a list: `entry-brackets=fix,bracket=parentheses` is a second rule called `bracket`. What a user typed is held against the rule, as a misspelled id is: a value it cannot read, or one given on `field:rule`, is a usage error rather than passed over. Such a value cannot contain a colon or a comma, which no value needs yet.

The bracket has to reach more than the rule, which can only repair what was read: a save writes an edited entry anew, and a formatter run every entry. `BibDatabaseWriter` therefore asks the rule which bracket to write, wherever the rule runs at all, whether it reports or repairs. Otherwise a library asking for parentheses would be written in braces by the very run meant to bring it in line, and `--check` could never be satisfied with it.

That leaves the writer with braces where the rule does not run, because it is switched off or cannot read its value: an entry written anew is enclosed in them as it always was, which in a formatter run means every entry, whatever it was read with. A magic comment switching the rule off for an entry keeps the rule from reporting it and from touching what was read, but it does not choose what the writer encloses that entry in when it writes it anew, in this bracket or in braces.

### Consequences

* Good, because a rule has exactly one entry per field it is configured on, so the file cannot state a contradiction about the same rule on the same field.
* Bad, because one precedence rule does have to be remembered after all, since a key naming a field and one naming none can both be about a field. It is the familiar one -- the more specific wins -- and it buys the narrow exceptions that make the field in a key worth having.
* Good, because a rule that grows a value grows its entry from a string into an object, and moves nowhere.
* Good, because the block reads as the answer to "what does this library have done to it?", which is the question rules-off-by-default makes the reader ask.
* Good, because the shape is the one ESLint and markdownlint use, so it needs little explanation and no new vocabulary.
* Bad, because one key holds either a string or an object, so the parser and any JSON schema carry a union type.
* Bad, because switching formatting off while keeping the configuration means removing the block or its rules: there is no flag to flip, which a library that wants to stop formatting temporarily would have liked.
* Neutral, because "everything" is not said by listing rules but by naming a mode — see below.

### Confirmation

A round trip: a library carrying a block with several rules, one of them written as an object with a value, is written, read back, and compares equal, and another test pins the text of that form. A second test writes a block naming a rule id this JabRef does not know, a mode it does not know and a value that is no string, and shows that the library still saves and that the unknown parts survive the round trip rather than being dropped. A third shows that a library without the block is saved exactly as it was before the block existed.

## Pros and Cons of the Options

### One list per mode

```json
"formatting": {
  "check": ["title-case"],
  "fix": ["surrounding-whitespace", "page-ranges"]
}
```

A rule in neither list does not run.

* Good, because each list answers one question outright: these are reported, those are repaired.
* Good, because it resembles what `jabref-meta` stores today, where an item is a list of strings.
* Good, because switching a rule off is deleting it from a list, which is the smallest possible edit and an obvious diff.
* Bad, because a rule id can appear in both lists, so the format can state a contradiction and the reader needs a precedence rule to resolve it.
* Bad, because values have no home: they need a third collection keyed by rule id, and one rule's business is then spread over places that can disagree about which rules exist.
* Bad, because every further mode is another list, and every list is another place to look when answering "what does this library do with rule X?".
* Bad, because "run everything" is a list of every id, which goes stale as soon as JabFix ships another rule.

### A mode per rule, as the rule's value, opening into an object where the rule takes values

```json
"formatting": {
  "rules": {
    "title-case": "check",
    "surrounding-whitespace": "fix",
    "line-length": {"mode": "check", "max": 120}
  }
}
```

This is the shape ESLint uses for severity and options, and markdownlint for switching a rule off or configuring it.

* Good, because one rule has exactly one entry: a contradiction cannot be expressed.
* Good, because values have an obvious place, and adding them to a rule does not move it anywhere.
* Good, because it maps one to one onto the in-memory map of mode per rule id, which widens to a record of mode plus values when values arrive.
* Good, because the shape is familiar from other linters, so the file needs little explanation.
* Neutral, because answering "which rules are reported only?" means reading the map rather than one list; for the size of a rule set this is a non-issue.
* Neutral, because "run everything" needs a key of its own either way — see the open point below.
* Bad, because one key has two shapes, a string or an object, so the parser and any schema carry a union type.

### A mode per rule, always an object

```json
"formatting": {
  "title-case": {"mode": "check"},
  "surrounding-whitespace": {"mode": "fix"}
}
```

* Good, because the shape is uniform: one rule, one object, whether or not it takes values.
* Good, because it keeps the contradiction-free property of the option above.
* Bad, because the common case is noisy: `{"mode": "fix"}` where `"fix"` would do, in a file people edit by hand.

### A list of rule objects

```json
"formatting": [
  {"rule": "title-case", "mode": "check"},
  {"rule": "surrounding-whitespace", "mode": "fix"}
]
```

* Good, because the order of entries is preserved, should the configuration ever need to express one.
* Neutral, because the rule set's order is decided by the code, not by the configuration, so preserved order buys nothing today.
* Bad, because the same rule can appear twice, which is the contradiction problem again, now without even a precedence rule to state.
* Bad, because looking a rule up means scanning the list, both for the code and for the person reading the file.

## How a library says "everything"

Because rules are off until named, the shape alone has no short way to say "run what JabFix recommends". The block therefore names a mode beside the rules — a set of rules with their modes that JabFix ships:

```json
"formatting": {
  "mode": "whitespace",
  "rules": {
    "repeated-whitespace": "off"
  }
}
```

A rule the block names outranks what the mode says about it, so the mode is a starting point the library narrows down. A command line names a mode and rules the same way, `--mode` taking the place of the library's mode and each rule it names outranking what the library says about that rule; a rule only the library names goes on applying. Nothing that neither a mode nor the rules enable runs, and `jabkit fix` warns when that is everything.

Which modes JabFix ships with is still open; the current ones are placeholders.

## More Information

The per-entry syntax stays as it is: a magic comment above an entry switches rules off for that entry (`% jabref-format-ignore surrounding-whitespace author:page-ranges`). That is a suppression of a rule the library switched on, so it reads as "not here", and it does not need the modes this block carries.

The in-memory settings follow this decision: they carry a mode and the values of the rule per selector, and a rule they do not name does not run.

What a library says about itself and what one run of a command asks for are kept apart, because this block is written back: `jabkit fix --rule lower-case=off` must not leave the library configured that way, nor give a formatting block to a library that never had one. The library's own configuration is what is serialized; a command overrides it for that save alone, unless it is asked to store what it was given (`--save-config`).

The block is stored in the embedded JSON metadata comment, `@Comment{jabref-meta-0.1.0 ...}`, which <https://github.com/JabRef/jabref/issues/10371> settles on and which a `jabref-meta:` item could not hold: its separator is the same `;` a save action uses inside its own value, so nothing that nests fits.

Prior art: ESLint configures a rule as `"semi": "error"` or `"semi": ["error", "always"]`, and markdownlint as `"MD013": false` or `"MD013": {"line_length": 120}`. Both start from the short form and open into the long one exactly when a rule takes something beyond its severity.

<!-- markdownlint-disable-file MD022 -->
