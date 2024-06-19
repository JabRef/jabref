// ArxivCleanupTest.java
package org.jabref.logic.cleanup;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArxivCleanupTest {

    @Test
    public void testMoveArxivFields() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.NOTE, "arXiv: 1503.05173");
        entry.setField(StandardField.INSTITUTION, "arxiv");
        entry.setField(StandardField.VERSION, "1");
        entry.setField(StandardField.EID, "arXiv:1503.05173");

        ArxivCleanup arxivCleanup = new ArxivCleanup();
        List<FieldChange> changes = arxivCleanup.cleanup(entry);

        assertEquals("arXiv: 1503.05173", entry.getField(StandardField.EPRINT).orElse(""));
        assertEquals("arxiv", entry.getField(StandardField.EPRINTTYPE).orElse(""));
        assertEquals("1", entry.getField(StandardField.EPRINTCLASS).orElse(""));
        assertTrue(changes.size() > 0);
    }
}
