package org.jabref.logic.citationkeypattern;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MakeLabelWithoutDatabaseTest {

    private BibEntry entry;

    @BeforeEach
    void setUp() {
        entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "John Doe");
        entry.setField(StandardField.YEAR, "2016");
        entry.setField(StandardField.TITLE, "An awesome paper on JabRef");
    }

    @Test
    void makeLabelForFileSearch() {
        String label = CitationKeyGenerator.generateKey(entry, "auth");
        assertEquals("Doe", label);
    }

    @Test
    void makeEditorLabelForFileSearch() {
        BibEntry localEntry = new BibEntry();
        localEntry.setField(StandardField.EDITOR, "John Doe");
        localEntry.setField(StandardField.YEAR, "2016");
        localEntry.setField(StandardField.TITLE, "An awesome paper on JabRef");

        String label = CitationKeyGenerator.generateKey(localEntry, "auth");
        assertEquals("Doe", label);
    }
}
