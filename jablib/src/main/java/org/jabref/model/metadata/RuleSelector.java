package org.jabref.model.metadata;

import java.util.Optional;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;

import org.jspecify.annotations.NullMarked;

/// What one entry of a library's formatting configuration is about: a rule, on one field or on
/// every field the rule looks at.
///
/// Naming a field narrows a mode to that field, which is how a library says "lower-case everything,
/// but only report it on the title". Which of two selectors that both cover a field wins is
/// [LintSettings]'s business, not this record's.
///
/// @param field  the field the mode is about, or empty for every field the rule looks at
/// @param ruleId the [org.jabref.logic.lint.rule.Rule#id] the mode is about
@NullMarked
public record RuleSelector(Optional<Field> field, String ruleId) {
    /// The rule wherever it looks.
    public static RuleSelector of(String ruleId) {
        return new RuleSelector(Optional.empty(), ruleId);
    }

    /// The rule on that field alone.
    public static RuleSelector on(Field field, String ruleId) {
        return new RuleSelector(Optional.of(field), ruleId);
    }

    /// Whether this is about every field the rule looks at, rather than about one of them.
    ///
    /// Naming no field says so outright. So does naming `all` or `all-text-fields`, which a Save
    /// Action may be configured with: the rule has already decided which fields of that class it
    /// visits, so such a selector is about all of them and not about a field called `all`. Without
    /// this, a Save Action configured for every text field would be named by a selector that no
    /// finding of it ever matches, and it would quietly stop running.
    public boolean coversEveryField() {
        return field.map(named -> (named == InternalField.INTERNAL_ALL_FIELD)
                                || (named == InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD))
                    .orElse(true);
    }
}
