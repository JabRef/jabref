package net.sf.jabref.logic.util.io;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


public class SimpleFileListEntryTest {

    @Test
    public void testTwoTermConstructor() {
        SimpleFileListEntry entry = new SimpleFileListEntry("test", "test.pdf");
        assertEquals("test", entry.getDescription());
        assertEquals("test.pdf", entry.getLink());
        assertEquals("", entry.getTypeName());
    }

    @Test
    public void testThreeTermConstructor() {
        SimpleFileListEntry entry = new SimpleFileListEntry("test", "test.pdf", "PDF");
        assertEquals("test", entry.getDescription());
        assertEquals("test.pdf", entry.getLink());
        assertEquals("PDF", entry.getTypeName());
    }

    @Test
    public void testEmptyListConstructor() {
        List<String> list = new ArrayList<>();
        SimpleFileListEntry entry = new SimpleFileListEntry(list);
        assertEquals("", entry.getDescription());
        assertEquals("", entry.getLink());
        assertEquals("", entry.getTypeName());
    }

    @Test
    public void testOneTermListConstructor() {
        List<String> list = new ArrayList<>();
        list.add("test.pdf");
        SimpleFileListEntry entry = new SimpleFileListEntry(list);
        assertEquals("", entry.getDescription());
        assertEquals("test.pdf", entry.getLink());
        assertEquals("", entry.getTypeName());
    }

    @Test
    public void testTwoTermListConstructor() {
        List<String> list = new ArrayList<>();
        list.add("test");
        list.add("test.pdf");
        SimpleFileListEntry entry = new SimpleFileListEntry(list);
        assertEquals("test", entry.getDescription());
        assertEquals("test.pdf", entry.getLink());
        assertEquals("", entry.getTypeName());
    }

    @Test
    public void testThreeTermListConstructor() {
        List<String> list = new ArrayList<>();
        list.add("test");
        list.add("test.pdf");
        list.add("PDF");
        SimpleFileListEntry entry = new SimpleFileListEntry(list);
        assertEquals("test", entry.getDescription());
        assertEquals("test.pdf", entry.getLink());
        assertEquals("PDF", entry.getTypeName());
    }

    @Test
    public void testGetStringArrayRepresentation() {
        List<String> list = new ArrayList<>();
        list.add("test");
        list.add("test.pdf");
        list.add("PDF");
        SimpleFileListEntry entry = new SimpleFileListEntry(list);
        assertArrayEquals(new String[] {"test", "test.pdf", "PDF"}, entry.getStringArrayRepresentation());
    }

    @Test
    public void testSettersAndGetters() {
        List<String> list = new ArrayList<>();
        SimpleFileListEntry entry = new SimpleFileListEntry(list);
        assertEquals("", entry.getDescription());
        entry.setDescription("test");
        assertEquals("test", entry.getDescription());
        assertEquals("", entry.getLink());
        entry.setLink("test.pdf");
        assertEquals("test.pdf", entry.getLink());
        assertEquals("", entry.getTypeName());
        entry.setTypeName("PDF");
        assertEquals("PDF", entry.getTypeName());
    }

    @Test
    public void testToString() {
        SimpleFileListEntry entry = new SimpleFileListEntry("test", "test.pdf", "PDF");
        assertEquals("test : test.pdf : PDF", entry.toString());
    }

}
