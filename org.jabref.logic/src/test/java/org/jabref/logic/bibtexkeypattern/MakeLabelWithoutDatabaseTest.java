package org.jabref.logic.bibtexkeypattern;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class MakeLabelWithoutDatabaseTest {

    private BibEntry entry;

    @BeforeEach
    public void setUp() {
        entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        entry.setField("title", "An awesome paper on JabRef");
    }

    @Test
    public void makeLabelForFileSearch() {
        String label = BibtexKeyGenerator.generateKey(entry, "auth");
        assertEquals("Doe", label);
    }

    @Test
    public void makeEditorLabelForFileSearch() {
        BibEntry localEntry = new BibEntry();
        localEntry.setField("editor", "John Doe");
        localEntry.setField("year", "2016");
        localEntry.setField("title", "An awesome paper on JabRef");

        String label = BibtexKeyGenerator.generateKey(localEntry, "auth");
        assertEquals("Doe", label);
    }

}
