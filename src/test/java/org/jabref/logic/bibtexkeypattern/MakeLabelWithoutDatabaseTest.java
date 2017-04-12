package org.jabref.logic.bibtexkeypattern;

import org.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MakeLabelWithoutDatabaseTest {

    // private GlobalBibtexKeyPattern pattern;
    private BibEntry entry;
    private String patternString;

    @Before
    public void setUp() {
        entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        entry.setField("title", "An awesome paper on JabRef");
    }

    @Test
    public void makeLabelForFileSearch() {
        String label = BibtexKeyPatternUtil.makeLabel(entry,
                /*value=*/ "auth",
                /*keywordDelimiter=*/ ',',
                /*database=*/ null);
        assertEquals("Doe", label);
    }

}
