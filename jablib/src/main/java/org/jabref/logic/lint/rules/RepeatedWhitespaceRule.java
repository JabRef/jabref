package org.jabref.logic.lint.rules;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.cleanup.CleanupJob;
import org.jabref.logic.cleanup.NormalizeWhitespacesCleanup;
import org.jabref.logic.lint.rule.CleanupRule;
import org.jabref.model.FieldChange;

import org.jspecify.annotations.NullMarked;

/// Collapses repeated whitespace inside a field value to a single space.
///
/// BibTeX pays no attention to how much whitespace separates two words, so a doubled space is
/// invisible in the typeset result and only makes values compare unequal that are in fact the same.
///
/// Which fields that leaves alone -- the ones holding no text, a URL or several lines -- is decided
/// by [NormalizeWhitespacesCleanup], the very cleanup a save has always applied, so a library
/// formatted by JabFix and one saved by JabRef come out the same. Whitespace at the ends of a value
/// is left to [SurroundingWhitespaceRule].
@NullMarked
public class RepeatedWhitespaceRule extends CleanupRule {
    public static final String ID = "repeated-whitespace";

    private final NormalizeWhitespacesCleanup cleanup;

    /// @param fieldPreferences tells the cleanup which fields hold text that may be wrapped
    public RepeatedWhitespaceRule(FieldPreferences fieldPreferences) {
        this.cleanup = new NormalizeWhitespacesCleanup(fieldPreferences);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Collapses repeated whitespace inside a field value to a single space.";
    }

    @Override
    protected CleanupJob cleanupJob() {
        return cleanup;
    }

    @Override
    protected String message(FieldChange change) {
        return "value contains repeated whitespace";
    }
}
