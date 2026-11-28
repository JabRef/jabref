package org.jabref.logic.lint.rule;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;

import org.jspecify.annotations.NullMarked;

/// Base class for the common kind of rule: one that judges a single field value at a time and can
/// state its repair as a replacement value.
///
/// Subclasses only implement [#normalize] -- the value they would rather see. Walking the entry's
/// fields, deciding whether anything is wrong, and wiring the [Fix] to the [Finding] happens here
/// once, which is what keeps scan and fix in step for every rule derived from this class.
///
/// A rule that wants to reuse one of JabRef's own field formatters can do so in a single line, by
/// delegating [#normalize] to [org.jabref.logic.formatter.Formatter#format].
@NullMarked
public abstract class FieldValueRule implements Rule {

    @Override
    public List<Finding> scan(BibEntry entry) {
        List<Finding> findings = new ArrayList<>();
        for (Field field : entry.getFields()) {
            // A citation key is not a field value: rewriting it invalidates every crossref pointing
            // at it, which is why JabRef's own field formatters pass over it as well.
            if ((field == InternalField.KEY_FIELD) || !appliesTo(field)) {
                continue;
            }
            entry.getField(field).ifPresent(value -> {
                String normalized = normalize(value);
                if (!normalized.equals(value)) {
                    findings.add(Finding.of(this, entry, field, message(value, normalized),
                            target -> target.setField(field, normalized)));
                }
            });
        }
        return findings;
    }

    /// Which fields the rule looks at. The default is all of them; override to narrow it down.
    protected boolean appliesTo(Field field) {
        return true;
    }

    /// The value this rule would rather see.
    ///
    /// @return `value` unchanged when the rule has nothing to report about it
    protected abstract String normalize(String value);

    /// What to tell the user about a value the rule objects to.
    ///
    /// @param value      the value as it stands
    /// @param normalized what [#normalize] made of it
    protected abstract String message(String value, String normalized);
}
