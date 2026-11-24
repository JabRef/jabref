package org.jabref.logic.lint.rule;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.LintSettings;

import org.jspecify.annotations.NullMarked;

/// Another rule, as far as the library lets it go on each field.
///
/// A rule keeps deciding what is wrong; this decides what happens to what it found. A library says
/// it per field, so one rule can have three answers at once -- repairing most of an entry, only
/// reporting on one field, switched off on another -- which is why this cannot be settled once for
/// the whole rule. A finding whose repair is taken away looks like one of a rule that knows no
/// repair, which callers already have to handle.
///
/// The findings keep naming the rule that reported them, so a report reads the same either way.
///
/// The rule is scanned in full even where every field is switched off, and the findings are dropped
/// afterwards. Scanning has no side effects, so this is only ever work wasted, never a difference
/// in behaviour -- but it is where a field filter would go if a rule ever becomes expensive enough
/// to want one.
@NullMarked
record ConfiguredRule(Rule rule, LintSettings settings) implements Rule {

    @Override
    public String id() {
        return rule.id();
    }

    @Override
    public String description() {
        return rule.description();
    }

    @Override
    public List<Finding> scan(BibEntry entry) {
        return rule.scan(entry).stream()
                   .flatMap(finding -> switch (settings.modeOf(rule.id(), finding.field())) {
                       case OFF ->
                               Stream.<Finding>empty();
                       case CHECK ->
                               Stream.of(finding.withoutFix());
                       case FIX ->
                               Stream.of(finding);
                   })
                   .toList();
    }
}
