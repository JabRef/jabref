package net.sf.jabref.openoffice;

import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Test;

import net.sf.jabref.openoffice.CitationManager.CitEntry;

public class CitationManagerTest {

    @Test
    public void testCitEntryInitalPageInfo() {
        CitEntry citEntry = new CitEntry("RefMark", "Context", "Info");
        assertFalse(citEntry.pageInfoChanged());
        assertTrue(citEntry.getPageInfo().isPresent());
        assertEquals("Info", citEntry.getPageInfo().get());
        assertEquals("RefMark", citEntry.getRefMarkName());
        assertEquals("Context", citEntry.getContext());
    }

    @Test
    public void testCitEntryOptionalInitalPageInfo() {
        CitEntry citEntry = new CitEntry("RefMark", "Context", Optional.of("Info"));
        assertFalse(citEntry.pageInfoChanged());
        assertTrue(citEntry.getPageInfo().isPresent());
        assertEquals("Info", citEntry.getPageInfo().get());
        assertEquals("RefMark", citEntry.getRefMarkName());
        assertEquals("Context", citEntry.getContext());
    }

    @Test
    public void testCitEntryInitalPageInfoChanged() {
        CitEntry citEntry = new CitEntry("RefMark", "Context", "Info");
        citEntry.setPageInfo("Other info");
        assertTrue(citEntry.pageInfoChanged());
        assertTrue(citEntry.getPageInfo().isPresent());
        assertEquals("Other info", citEntry.getPageInfo().get());
    }

    @Test
    public void testCitEntryInitalPageInfoRemoved() {
        CitEntry citEntry = new CitEntry("RefMark", "Context", "Info");
        citEntry.setPageInfo(null);
        assertTrue(citEntry.pageInfoChanged());
        assertFalse(citEntry.getPageInfo().isPresent());
    }

    @Test
    public void testCitEntryNoInitalPageInfo() {
        CitEntry citEntry = new CitEntry("RefMark", "Context");
        assertFalse(citEntry.pageInfoChanged());
        assertFalse(citEntry.getPageInfo().isPresent());
    }

    @Test
    public void testCitEntryNoInitalPageInfoChanged() {
        CitEntry citEntry = new CitEntry("RefMark", "Context");
        citEntry.setPageInfo("Other info");
        assertTrue(citEntry.pageInfoChanged());
        assertTrue(citEntry.getPageInfo().isPresent());
        assertEquals("Other info", citEntry.getPageInfo().get());
    }

    @Test
    public void testCitEntryEquals() {
        CitEntry citEntry1 = new CitEntry("RefMark", "Context", "Info");
        CitEntry citEntry2 = new CitEntry("RefMark2", "Context", "Info");
        CitEntry citEntry3 = new CitEntry("RefMark", "Other Context", "Other Info");
        assertEquals(citEntry1, citEntry1);
        assertEquals(citEntry1, citEntry3);
        assertFalse(citEntry1.equals(citEntry2));
        assertFalse(citEntry1.equals("Random string"));
    }

    @Test
    public void testCitEntryCompareTo() {
        CitEntry citEntry1 = new CitEntry("RefMark", "Context", "Info");
        CitEntry citEntry2 = new CitEntry("RefMark2", "Context", "Info");
        CitEntry citEntry3 = new CitEntry("RefMark", "Other Context", "Other Info");
        assertEquals(0, citEntry1.compareTo(citEntry1));
        assertEquals(0, citEntry1.compareTo(citEntry3));
        assertEquals(-1, citEntry1.compareTo(citEntry2));
        assertEquals(1, citEntry2.compareTo(citEntry1));
    }
}
