package org.jabref.logic.lint.rules;

import org.jabref.logic.formatter.bibtexfields.TrimWhitespaceFormatter;
import org.jabref.logic.lint.rule.FieldValueRule;

import org.jspecify.annotations.NullMarked;

/// Strips whitespace from both ends of a field value.
///
/// BibTeX ignores it, so it carries no meaning; it only makes values compare unequal that are in
/// fact the same, which matters as soon as entries are deduplicated, sorted or diffed. Whitespace
/// *inside* a value is left untouched -- there it can be deliberate.
///
/// Also the smallest example of a rule: a [FieldValueRule] only states what a value should be.
@NullMarked
public class SurroundingWhitespaceRule extends FieldValueRule {
    public static final String ID = "surrounding-whitespace";

    /// The formatter a save has always used for this, so that a library formatted by JabFix and one
    /// saved by JabRef come out the same.
    private static final TrimWhitespaceFormatter TRIM = new TrimWhitespaceFormatter();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Removes whitespace at the start and end of a field value.";
    }

    @Override
    protected String normalize(String value) {
        return TRIM.format(value);
    }

    @Override
    protected String message(String value, String normalized) {
        return "value has leading or trailing whitespace";
    }
}
