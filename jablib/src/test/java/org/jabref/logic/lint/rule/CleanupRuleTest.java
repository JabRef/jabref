package org.jabref.logic.lint.rule;

import java.util.List;

import org.jabref.logic.cleanup.CleanupJob;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CleanupRuleTest {

    /// A cleanup over a whole entry, which is what a formatter applied to one field cannot express.
    private static final CleanupJob DROP_THE_NOTE = entry ->
            entry.clearField(StandardField.NOTE).map(List::of).orElse(List.of());

    private final CleanupRule rule = new CleanupRule() {
        @Override
        public String id() {
            return "no-note";
        }

        @Override
        public String description() {
            return "Removes the note field.";
        }

        @Override
        protected CleanupJob cleanupJob() {
            return DROP_THE_NOTE;
        }

        @Override
        protected String message(FieldChange change) {
            return "the note is removed";
        }
    };

    @Test
    void reportsWhatTheCleanupWouldChangeWithoutChangingIt() {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.NOTE, "to be removed");

        List<Finding> findings = rule.scan(entry);

        assertEquals(List.of(StandardField.NOTE), findings.stream().map(finding -> finding.field().orElseThrow()).toList());
        assertEquals("to be removed", entry.getField(StandardField.NOTE).orElseThrow());
    }

    @Test
    void theFixAppliesWhatTheCleanupReported() {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.NOTE, "to be removed");

        FieldChange change = rule.scan(entry).getFirst().fix().orElseThrow().applyTo(entry).orElseThrow();

        assertFalse(entry.hasField(StandardField.NOTE));
        assertEquals("to be removed", change.oldValue());
    }

    @Test
    void anEntryTheCleanupLeavesAloneIsNotReported() {
        assertEquals(List.of(), rule.scan(new BibEntry(StandardEntryType.Article)));
    }
}
