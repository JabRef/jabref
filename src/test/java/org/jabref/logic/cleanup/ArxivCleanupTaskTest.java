package org.jabref.logic.cleanup;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArxivCleanupTaskTest {

    @Test
    public void cleanupMovesArxivFieldsToEprint() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.NOTE, "arXiv: 1503.05173");
        entry.setField(StandardField.VERSION, "1");
        entry.setField(StandardField.INSTITUTION, "arxiv");
        entry.setField(StandardField.EID, "arXiv:1503.05173");

        CleanupJob cleanupJob = new ArxivCleanupTask();
        List<FieldChange> changes = cleanupJob.cleanup(entry);

        assertEquals("1503.05173", entry.getField(StandardField.EPRINT).orElse(""));
        assertEquals("arxiv", entry.getField(StandardField.EPRINTTYPE).orElse(""));
        assertEquals("1", entry.getField(StandardField.EPRINTCLASS).orElse(""));

        assertTrue(changes.stream().anyMatch(change -> change.getField().equals(StandardField.EPRINT) && "1503.05173".equals(change.getNewValue())));
        assertTrue(changes.stream().anyMatch(change -> change.getField().equals(StandardField.EPRINTTYPE) && "arxiv".equals(change.getNewValue())));
        assertTrue(changes.stream().anyMatch(change -> change.getField().equals(StandardField.EPRINTCLASS) && "1".equals(change.getNewValue())));
    }

    // Add more tests as needed
}
