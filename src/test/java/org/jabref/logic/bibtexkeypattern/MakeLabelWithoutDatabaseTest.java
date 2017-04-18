package org.jabref.logic.bibtexkeypattern;

import org.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MakeLabelWithoutDatabaseTest {

    private BibEntry entry;

    @Before
    public void setUp() {
        entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        entry.setField("title", "An awesome paper on JabRef");
    }

    @Test
    public void makeLabelForFileSearch() {
        String label =
            BibtexKeyPatternUtil.makeLabel(entry, "auth", ',', null);
        assertEquals("Doe", label);
    }

    @Test
    public void makeEditorLabelForFileSearch() {
        BibEntry localEntry = new BibEntry();
        localEntry.setField("editor", "John Doe");
        localEntry.setField("year", "2016");
        localEntry.setField("title", "An awesome paper on JabRef");

        String label =
            BibtexKeyPatternUtil.makeLabel(localEntry, "auth", ',', null);
        assertEquals("Doe", label);
    }

}
