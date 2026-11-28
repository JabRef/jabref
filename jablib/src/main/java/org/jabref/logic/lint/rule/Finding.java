package org.jabref.logic.lint.rule;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.jspecify.annotations.NullMarked;

/// Something a [Rule] objects to, together with the repair for it -- if the rule knows one.
///
/// @param rule    the rule that reported this
/// @param entry   the entry the finding was reported on
/// @param field   the field at fault, empty when the finding concerns the entry as a whole
/// @param message what is wrong, phrased for a person reading a report
/// @param fix     how to repair it, empty when the rule can only report the problem
@NullMarked
public record Finding(Rule rule, BibEntry entry, Optional<Field> field, String message, Optional<Fix> fix) {
    /// Convenience factory for the common case: a fixable problem with one specific field.
    public static Finding of(Rule rule, BibEntry entry, Field field, String message, Fix fix) {
        return new Finding(rule, entry, Optional.of(field), message, Optional.of(fix));
    }

    public boolean isFixable() {
        return fix.isPresent();
    }

    /// The same finding, reported but not repaired, which is what a library asks for when it wants a
    /// rule checked only.
    public Finding withoutFix() {
        return new Finding(rule, entry, field, message, Optional.empty());
    }

    /// @return the key of the offending entry, or an empty string for an entry that has none
    public String citationKey() {
        return entry.getCitationKey().orElse("");
    }
}
