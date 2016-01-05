package net.sf.jabref.logic.util.io;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

public class SimpleFileListTest {

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testIncompleteSingleItemConstructor() {
        SimpleFileList list = new SimpleFileList("test.pdf");
        assertEquals(1, list.size());
        assertFalse(list.isEmpty());
    }

    @Test
    public void testSingleItemConstructor() {
        SimpleFileList list = new SimpleFileList("test:test.pdf:PDF");
        assertEquals(1, list.size());
        assertFalse(list.isEmpty());
    }

    @Test
    public void testNullConstructor() {
        SimpleFileList list = new SimpleFileList(null);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testMultiItemConstructors() {
        SimpleFileList list = new SimpleFileList("test:test.pdf:PDF;presentation:file.ppt:PPT");
        assertEquals(2, list.size());
        assertFalse(list.isEmpty());
    }

    @Test
    public void testIncompleteMultiItemConstructors() {
        SimpleFileList list = new SimpleFileList("test.pdf;presentation:file.ppt:PPT");
        assertEquals(2, list.size());
        assertFalse(list.isEmpty());
        assertEquals("pdf", list.getEntry(0).getTypeName().toLowerCase());
    }

    @Test
    public void testStringArrayRepresentation() {
        SimpleFileList list = new SimpleFileList("test:test.pdf:pdf");
        assertEquals("test:test.pdf:PDF", list.getStringRepresentation());
    }

    @Test
    public void testIncompleteStringArrayRepresentation() {
        SimpleFileList list = new SimpleFileList("test.pdf;presentation:file.ppt");
        assertEquals(2, list.size());
        assertFalse(list.isEmpty());
        assertEquals(":test.pdf:PDF;presentation:file.ppt:PowerPoint", list.getStringRepresentation());
    }

    @Test
    public void testRemoveItem() {
        SimpleFileList list = new SimpleFileList("test:test.pdf:PDF;presentation:file.ppt:PPT");
        assertEquals(2, list.size());
        assertFalse(list.isEmpty());
        assertEquals("test.pdf", list.getEntry(0).getLink());
        list.removeEntry(0);
        assertEquals(1, list.size());
        assertFalse(list.isEmpty());
        assertEquals("file.ppt", list.getEntry(0).getLink());
        list.removeEntry(0);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testAddEntry() {
        SimpleFileList list = new SimpleFileList(null);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
        list.addEntry(new SimpleFileListEntry("test", "test.pdf", "PDF"));
        assertEquals(1, list.size());
        assertFalse(list.isEmpty());
        assertEquals("test", list.getEntry(0).getDescription().toLowerCase());
        assertEquals("test.pdf", list.getEntry(0).getLink().toLowerCase());
        assertEquals("pdf", list.getEntry(0).getTypeName().toLowerCase());
    }

}
