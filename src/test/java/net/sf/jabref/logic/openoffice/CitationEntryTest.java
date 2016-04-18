package net.sf.jabref.logic.openoffice;

import java.util.Optional;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CitationEntryTest {

    @Test
    public void testCitationEntryInitalPageInfo() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context", "Info");
        assertFalse(citationEntry.pageInfoChanged());
        assertTrue(citationEntry.getPageInfo().isPresent());
        assertEquals("Info", citationEntry.getPageInfo().get());
        assertEquals("RefMark", citationEntry.getRefMarkName());
        assertEquals("Context", citationEntry.getContext());
    }

    @Test
    public void testCitationEntryOptionalInitalPageInfo() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context", Optional.of("Info"));
        assertFalse(citationEntry.pageInfoChanged());
        assertTrue(citationEntry.getPageInfo().isPresent());
        assertEquals("Info", citationEntry.getPageInfo().get());
        assertEquals("RefMark", citationEntry.getRefMarkName());
        assertEquals("Context", citationEntry.getContext());
    }

    @Test
    public void testCitationEntryInitalPageInfoChanged() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context", "Info");
        citationEntry.setPageInfo("Other info");
        assertTrue(citationEntry.pageInfoChanged());
        assertTrue(citationEntry.getPageInfo().isPresent());
        assertEquals("Other info", citationEntry.getPageInfo().get());
    }

    @Test
    public void testCitationEntryInitalPageInfoRemoved() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context", "Info");
        citationEntry.setPageInfo(null);
        assertTrue(citationEntry.pageInfoChanged());
        assertFalse(citationEntry.getPageInfo().isPresent());
    }

    @Test
    public void testCitationEntryNoInitalPageInfo() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context");
        assertFalse(citationEntry.pageInfoChanged());
        assertFalse(citationEntry.getPageInfo().isPresent());
    }

    @Test
    public void testCitationEntryNoInitalPageInfoChanged() {
        CitationEntry citationEntry = new CitationEntry("RefMark", "Context");
        citationEntry.setPageInfo("Other info");
        assertTrue(citationEntry.pageInfoChanged());
        assertTrue(citationEntry.getPageInfo().isPresent());
        assertEquals("Other info", citationEntry.getPageInfo().get());
    }

    @Test
    public void testCitationEntryEquals() {
        CitationEntry citationEntry1 = new CitationEntry("RefMark", "Context", "Info");
        CitationEntry citationEntry2 = new CitationEntry("RefMark2", "Context", "Info");
        CitationEntry citationEntry3 = new CitationEntry("RefMark", "Other Context", "Other Info");
        assertEquals(citationEntry1, citationEntry1);
        assertEquals(citationEntry1, citationEntry3);
        assertNotEquals(citationEntry1, citationEntry2);
        assertNotEquals(citationEntry1, "Random String");
    }

    @Test
    public void testCitationEntryCompareTo() {
        CitationEntry citationEntry1 = new CitationEntry("RefMark", "Context", "Info");
        CitationEntry citationEntry2 = new CitationEntry("RefMark2", "Context", "Info");
        CitationEntry citationEntry3 = new CitationEntry("RefMark", "Other Context", "Other Info");
        assertEquals(0, citationEntry1.compareTo(citationEntry1));
        assertEquals(0, citationEntry1.compareTo(citationEntry3));
        assertEquals(-1, citationEntry1.compareTo(citationEntry2));
        assertEquals(1, citationEntry2.compareTo(citationEntry1));
    }
}
