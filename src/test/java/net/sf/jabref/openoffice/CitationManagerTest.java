package net.sf.jabref.openoffice;

import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Test;

import net.sf.jabref.openoffice.CitationManager.CitEntry;

public class CitationManagerTest {

    @Test
    public void testCitEntryInitalPageInfo() {
        CitEntry citEntry = new CitEntry("RefMark", "Context", Optional.of("Info"));
        assertFalse(citEntry.pageInfoChanged());
        assertTrue(citEntry.getPageInfo().isPresent());
        assertEquals("Info", citEntry.getPageInfo().get());
        assertEquals("RefMark", citEntry.getRefMarkName());
        assertEquals("Context", citEntry.getContext());
    }

    @Test
    public void testCitEntryInitalPageInfoChanged() {
        CitEntry citEntry = new CitEntry("RefMark", "Context", Optional.of("Info"));
        citEntry.setPageInfo(Optional.of("Other info"));
        assertTrue(citEntry.pageInfoChanged());
        assertTrue(citEntry.getPageInfo().isPresent());
        assertEquals("Other info", citEntry.getPageInfo().get());
    }

    @Test
    public void testCitEntryInitalPageInfoRemoved() {
        CitEntry citEntry = new CitEntry("RefMark", "Context", Optional.of("Info"));
        citEntry.setPageInfo(Optional.empty());
        assertTrue(citEntry.pageInfoChanged());
        assertFalse(citEntry.getPageInfo().isPresent());
    }

    @Test
    public void testCitEntryNoInitalPageInfo() {
        CitEntry citEntry = new CitEntry("RefMark", "Context", Optional.empty());
        assertFalse(citEntry.pageInfoChanged());
        assertFalse(citEntry.getPageInfo().isPresent());
    }

    @Test
    public void testCitEntryNoInitalPageInfoChanged() {
        CitEntry citEntry = new CitEntry("RefMark", "Context", Optional.empty());
        citEntry.setPageInfo(Optional.of("Other info"));
        assertTrue(citEntry.pageInfoChanged());
        assertTrue(citEntry.getPageInfo().isPresent());
        assertEquals("Other info", citEntry.getPageInfo().get());
    }

    @Test
    public void testCitEntryEquals() {
        CitEntry citEntry1 = new CitEntry("RefMark", "Context", Optional.of("Info"));
        CitEntry citEntry2 = new CitEntry("RefMark2", "Context", Optional.of("Info"));
        CitEntry citEntry3 = new CitEntry("RefMark", "Other Context", Optional.of("Other Info"));
        assertTrue(citEntry1.equals(citEntry1));
        assertTrue(citEntry1.equals(citEntry3));
        assertFalse(citEntry1.equals(citEntry2));
        assertFalse(citEntry1.equals(null));
    }

    @Test
    public void testCitEntryCompareTo() {
        CitEntry citEntry1 = new CitEntry("RefMark", "Context", Optional.of("Info"));
        CitEntry citEntry2 = new CitEntry("RefMark2", "Context", Optional.of("Info"));
        CitEntry citEntry3 = new CitEntry("RefMark", "Other Context", Optional.of("Other Info"));
        assertEquals(0, citEntry1.compareTo(citEntry1));
        assertEquals(0, citEntry1.compareTo(citEntry3));
        assertEquals(-1, citEntry1.compareTo(citEntry2));
        assertEquals(1, citEntry2.compareTo(citEntry1));
    }
}
