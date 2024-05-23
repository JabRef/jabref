package org.jabref.model.openoffice;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitationEntryTest {

    @Test
    void citationEntryInitialPageInfo() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context", "Info");
        assertTrue(citationEntry.getPageInfo().isPresent());
        assertEquals("Info", citationEntry.getPageInfo().get());
        assertEquals("RefMark", citationEntry.getRefMarkName());
        assertEquals("Context", citationEntry.getContext());
    }

    @Test
    void citationEntryOptionalInitialPageInfo() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context", Optional.of("Info"));

        assertEquals(Optional.of("Info"), citationEntry.getPageInfo());
        assertEquals("RefMark", citationEntry.getRefMarkName());
        assertEquals("Context", citationEntry.getContext());
    }

    @Test
    void citationEntryInitalPageInfoChanged() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context", "Info");
        assertEquals(Optional.of("Info"), citationEntry.getPageInfo());
    }

    @Test
    void citationEntryNoInitialPageInfo() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context");
        assertEquals(Optional.empty(), citationEntry.getPageInfo());
    }

    @Test
    void citationEntryEquals() {
        CitationEntry citationEntry1 = new CitationEntry("RefMark", "Context", "Info");
        CitationEntry citationEntry2 = new CitationEntry("RefMark2", "Context", "Info");
        CitationEntry citationEntry3 = new CitationEntry("RefMark", "Other Context", "Other Info");
        assertEquals(citationEntry1, citationEntry1);
        assertEquals(citationEntry1, citationEntry3);
        assertNotEquals(citationEntry1, citationEntry2);
        assertNotEquals("Random String", citationEntry1);
    }

    @Test
    void citationEntryCompareTo() {
        CitationEntry citationEntry1 = new CitationEntry("RefMark", "Context", "Info");
        CitationEntry citationEntry2 = new CitationEntry("RefMark2", "Context", "Info");
        CitationEntry citationEntry3 = new CitationEntry("RefMark", "Other Context", "Other Info");
        assertEquals(0, citationEntry1.compareTo(citationEntry3));
        assertEquals(-1, citationEntry1.compareTo(citationEntry2));
        assertEquals(1, citationEntry2.compareTo(citationEntry1));
    }
}
