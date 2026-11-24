package org.jabref.logic.lint.rule;

import java.util.Locale;
import java.util.Optional;

import org.jabref.logic.cleanup.CleanupJob;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.FieldChange;

import org.jspecify.annotations.NullMarked;

/// Runs one of JabRef's Save Actions as a [Rule].
///
/// A Save Action is a formatter applied to a field, which is a [CleanupJob] like any other -- see
/// [CleanupRule] for how it is scanned and repaired. What this class adds is the identity of the
/// rule, the key the formatter is configured under, and what a finding about it says.
@NullMarked
public class SaveActionRule extends CleanupRule {
    private static final int MAX_REPORTED_VALUE_LENGTH = 60;

    private final FieldFormatterCleanup saveAction;

    /// @param saveAction a formatter applied to a field, as configured in JabRef's Save Actions
    public SaveActionRule(FieldFormatterCleanup saveAction) {
        this.saveAction = saveAction;
    }

    /// The formatter's key, e.g. `normalize-page-numbers`.
    ///
    /// Which fields the Save Action covers is its own business and is left out of the id, so that
    /// the same formatter on several fields is one rule to switch off. A single field is narrowed
    /// down where every other rule is too, by `field:rule` in a magic comment (see [Suppressions]).
    @Override
    public String id() {
        return saveAction.getFormatter().getKey().replace('_', '-').toLowerCase(Locale.ROOT);
    }

    @Override
    public String description() {
        return saveAction.getFormatter().getDescription();
    }

    @Override
    protected CleanupJob cleanupJob() {
        return saveAction;
    }

    /// The value as it stands and what the Save Action makes of it, both cut short: a field value
    /// can be as long as an abstract, while a finding is one line.
    @Override
    protected String message(FieldChange change) {
        String oldValue = StringUtil.limitStringLength(change.oldValue(), MAX_REPORTED_VALUE_LENGTH);
        return Optional.ofNullable(change.newValue())
                       .map(newValue -> "\"%s\", should be \"%s\"".formatted(oldValue, StringUtil.limitStringLength(newValue, MAX_REPORTED_VALUE_LENGTH)))
                       .orElse("\"%s\", should be removed".formatted(oldValue));
    }
}
