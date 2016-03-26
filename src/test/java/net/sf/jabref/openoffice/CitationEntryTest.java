package net.sf.jabref.openoffice;

import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Test;

public class CitationEntryTest {

    @Test
    public void testCitEntryInitalPageInfo() {
        CitationEntry citEntry = new CitationEntry("RefMark", "Context", "Info");
        assertFalse(citEntry.pageInfoChanged());
        assertTrue(citEntry.getPageInfo().isPresent());
        assertEquals("Info", citEntry.getPageInfo().get());
        assertEquals("RefMark", citEntry.getRefMarkName());
        assertEquals("Context", citEntry.getContext());
    }

    @Test
    public void testCitEntryOptionalInitalPageInfo() {
        CitationEntry citEntry = new CitationEntry("RefMark", "Context", Optional.of("Info"));
        assertFalse(citEntry.pageInfoChanged());
        assertTrue(citEntry.getPageInfo().isPresent());
        assertEquals("Info", citEntry.getPageInfo().get());
        assertEquals("RefMark", citEntry.getRefMarkName());
        assertEquals("Context", citEntry.getContext());
    }

    @Test
    public void testCitEntryInitalPageInfoChanged() {
        CitationEntry citEntry = new CitationEntry("RefMark", "Context", "Info");
        citEntry.setPageInfo("Other info");
        assertTrue(citEntry.pageInfoChanged());
        assertTrue(citEntry.getPageInfo().isPresent());
        assertEquals("Other info", citEntry.getPageInfo().get());
    }

    @Test
    public void testCitEntryInitalPageInfoRemoved() {
        CitationEntry citEntry = new CitationEntry("RefMark", "Context", "Info");
        citEntry.setPageInfo(null);
        assertTrue(citEntry.pageInfoChanged());
        assertFalse(citEntry.getPageInfo().isPresent());
    }

    @Test
    public void testCitEntryNoInitalPageInfo() {
        CitationEntry citEntry = new CitationEntry("RefMark", "Context");
        assertFalse(citEntry.pageInfoChanged());
        assertFalse(citEntry.getPageInfo().isPresent());
    }

    @Test
    public void testCitEntryNoInitalPageInfoChanged() {
        CitationEntry citEntry = new CitationEntry("RefMark", "Context");
        citEntry.setPageInfo("Other info");
        assertTrue(citEntry.pageInfoChanged());
        assertTrue(citEntry.getPageInfo().isPresent());
        assertEquals("Other info", citEntry.getPageInfo().get());
    }

    @Test
    public void testCitEntryEquals() {
        CitationEntry citEntry1 = new CitationEntry("RefMark", "Context", "Info");
        CitationEntry citEntry2 = new CitationEntry("RefMark2", "Context", "Info");
        CitationEntry citEntry3 = new CitationEntry("RefMark", "Other Context", "Other Info");
        assertEquals(citEntry1, citEntry1);
        assertEquals(citEntry1, citEntry3);
        assertNotEquals(citEntry1, citEntry2);
        assertNotEquals(citEntry1, "Random String");
    }

    @Test
    public void testCitEntryCompareTo() {
        CitationEntry citEntry1 = new CitationEntry("RefMark", "Context", "Info");
        CitationEntry citEntry2 = new CitationEntry("RefMark2", "Context", "Info");
        CitationEntry citEntry3 = new CitationEntry("RefMark", "Other Context", "Other Info");
        assertEquals(0, citEntry1.compareTo(citEntry1));
        assertEquals(0, citEntry1.compareTo(citEntry3));
        assertEquals(-1, citEntry1.compareTo(citEntry2));
        assertEquals(1, citEntry2.compareTo(citEntry1));
    }
}
