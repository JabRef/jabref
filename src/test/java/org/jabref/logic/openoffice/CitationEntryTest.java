package org.jabref.logic.openoffice;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitationEntryTest {

    @Test
    void testCitationEntryInitalPageInfo() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context", "Info");
        assertFalse(citationEntry.pageInfoChanged());
        assertTrue(citationEntry.getPageInfo().isPresent());
        assertEquals("Info", citationEntry.getPageInfo().get());
        assertEquals("RefMark", citationEntry.getRefMarkName());
        assertEquals("Context", citationEntry.getContext());
    }

    @Test
    void testCitationEntryOptionalInitalPageInfo() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context", Optional.of("Info"));
        assertFalse(citationEntry.pageInfoChanged());
        assertTrue(citationEntry.getPageInfo().isPresent());
        assertEquals("Info", citationEntry.getPageInfo().get());
        assertEquals("RefMark", citationEntry.getRefMarkName());
        assertEquals("Context", citationEntry.getContext());
    }

    @Test
    void testCitationEntryInitalPageInfoChanged() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context", "Info");
        citationEntry.setPageInfo("Other info");
        assertTrue(citationEntry.pageInfoChanged());
        assertTrue(citationEntry.getPageInfo().isPresent());
        assertEquals("Other info", citationEntry.getPageInfo().get());
    }

    @Test
    void testCitationEntryInitalPageInfoRemoved() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context", "Info");
        citationEntry.setPageInfo(null);
        assertTrue(citationEntry.pageInfoChanged());
        assertFalse(citationEntry.getPageInfo().isPresent());
    }

    @Test
    void testCitationEntryNoInitalPageInfo() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context");
        assertFalse(citationEntry.pageInfoChanged());
        assertFalse(citationEntry.getPageInfo().isPresent());
    }

    @Test
    void testCitationEntryNoInitalPageInfoChanged() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context");
        citationEntry.setPageInfo("Other info");
        assertTrue(citationEntry.pageInfoChanged());
        assertTrue(citationEntry.getPageInfo().isPresent());
        assertEquals("Other info", citationEntry.getPageInfo().get());
    }

    @Test
    void testCitationEntryEquals() {
        CitationEntry citationEntry1 = new CitationEntry("RefMark", "Context", "Info");
        CitationEntry citationEntry2 = new CitationEntry("RefMark2", "Context", "Info");
        CitationEntry citationEntry3 = new CitationEntry("RefMark", "Other Context", "Other Info");
        assertEquals(citationEntry1, citationEntry1);
        assertEquals(citationEntry1, citationEntry3);
        assertNotEquals(citationEntry1, citationEntry2);
        assertNotEquals(citationEntry1, "Random String");
    }

    @Test
    void testCitationEntryCompareTo() {
        CitationEntry citationEntry1 = new CitationEntry("RefMark", "Context", "Info");
        CitationEntry citationEntry2 = new CitationEntry("RefMark2", "Context", "Info");
        CitationEntry citationEntry3 = new CitationEntry("RefMark", "Other Context", "Other Info");
        assertEquals(0, citationEntry1.compareTo(citationEntry3));
        assertEquals(-1, citationEntry1.compareTo(citationEntry2));
        assertEquals(1, citationEntry2.compareTo(citationEntry1));
    }
}
