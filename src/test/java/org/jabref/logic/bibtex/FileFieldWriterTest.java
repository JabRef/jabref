package org.jabref.logic.bibtex;

import java.nio.file.Path;

import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FileFieldWriterTest {

    @Test
    public void testQuoteStandard() {
        assertEquals("a", FileFieldWriter.quote("a"));
    }

    @Test
    public void testQuoteAllCharacters() {
        assertEquals("a\\:\\;\\\\", FileFieldWriter.quote("a:;\\"));
    }

    @Test
    public void testQuoteEmpty() {
        assertEquals("", FileFieldWriter.quote(""));
    }

    @Test
    public void testQuoteNull() {
        assertNull(FileFieldWriter.quote(null));
    }

    @Test
    public void testEncodeStringArray() {
        assertEquals("a:b;c:d", FileFieldWriter.encodeStringArray(new String[][] {{"a", "b"}, {"c", "d"}}), "Encoding of stringArray failed.");
        assertEquals("a:;c:d", FileFieldWriter.encodeStringArray(new String[][] {{"a", ""}, {"c", "d"}}), "Encoding of stringArray failed. Check empty string case.");
        assertEquals("a:" + null + ";c:d", FileFieldWriter.encodeStringArray(new String[][] {{"a", null}, {"c", "d"}}), "Encoding of stringArray failed. Check null case.");
        assertEquals("a:\\:b;c\\;:d", FileFieldWriter.encodeStringArray(new String[][] {{"a", ":b"}, {"c;", "d"}}), "Encoding of stringArray failed. Check escaped character case.");
    }

    @Test
    public void testFileFieldWriterGetStringRepresentation() {
        LinkedFile file = new LinkedFile("test", Path.of("X:\\Users\\abc.pdf"), "PDF");
        assertEquals("test:X\\:/Users/abc.pdf:PDF", FileFieldWriter.getStringRepresentation(file));
    }
}
