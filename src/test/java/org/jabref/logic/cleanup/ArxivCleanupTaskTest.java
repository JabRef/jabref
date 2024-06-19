package org.jabref.logic.cleanup;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ArxivCleanupTaskTest {

    @Test
    public void cleanupRemovesArxivPrefix() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.EPRINT, "arXiv:1234.5678v1");

        ArxivCleanupTask cleanup = new ArxivCleanupTask();
        cleanup.cleanup(entry);

        Assertions.assertEquals("1234.5678v1", entry.getField(StandardField.EPRINT).get());
    }

    @Test
    public void cleanupRemovesHttpArxivOrg() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.EPRINT, "http://arxiv.org/abs/1234.5678v1");

        ArxivCleanupTask cleanup = new ArxivCleanupTask();
        cleanup.cleanup(entry);

        Assertions.assertEquals("1234.5678v1", entry.getField(StandardField.EPRINT).get());
    }

    @Test
    public void cleanupRemovesHttpsArxivOrg() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.EPRINT, "https://arxiv.org/abs/1234.5678v1");

        ArxivCleanupTask cleanup = new ArxivCleanupTask();
        cleanup.cleanup(entry);

        Assertions.assertEquals("1234.5678v1", entry.getField(StandardField.EPRINT).get());
    }
}
