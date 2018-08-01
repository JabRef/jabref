package org.jabref.logic.layout.format;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class WrapFileLinksTest {

    private WrapFileLinks formatter;


    @BeforeEach
    public void setUp() {
        FileLinkPreferences preferences = new FileLinkPreferences(Collections.emptyList(), Collections.emptyList());
        formatter = new WrapFileLinks(preferences);
    }

    @Test
    public void testEmpty() {
        assertEquals("", formatter.format(""));
    }

    @Test
    public void testNull() {
        assertEquals("", formatter.format(null));
    }

    public void testNoFormatSetNonEmptyString() {
        assertThrows(NullPointerException.class, () -> formatter.format("test.pdf"));

    }

    @Test
    public void testFileExtension() {
        formatter.setArgument("\\x");
        assertEquals("pdf", formatter.format("test.pdf"));
    }

    @Test
    public void testFileExtensionNoExtension() {
        formatter.setArgument("\\x");
        assertEquals("", formatter.format("test"));
    }

    @Test
    public void testPlainTextString() {
        formatter.setArgument("x");
        assertEquals("x", formatter.format("test.pdf"));
    }

    @Test
    public void testDescription() {
        formatter.setArgument("\\d");
        assertEquals("Test file", formatter.format("Test file:test.pdf:PDF"));
    }

    @Test
    public void testDescriptionNoDescription() {
        formatter.setArgument("\\d");
        assertEquals("", formatter.format("test.pdf"));
    }

    @Test
    public void testType() {
        formatter.setArgument("\\f");
        assertEquals("PDF", formatter.format("Test file:test.pdf:PDF"));
    }

    @Test
    public void testTypeNoType() {
        formatter.setArgument("\\f");
        assertEquals("", formatter.format("test.pdf"));
    }

    @Test
    public void testIterator() {
        formatter.setArgument("\\i");
        assertEquals("1", formatter.format("Test file:test.pdf:PDF"));
    }

    @Test
    public void testIteratorTwoItems() {
        formatter.setArgument("\\i\n");
        assertEquals("1\n2\n", formatter.format("Test file:test.pdf:PDF;test2.pdf"));
    }

    @Test
    public void testEndingBracket() {
        formatter.setArgument("(\\d)");
        assertEquals("(Test file)", formatter.format("Test file:test.pdf:PDF"));
    }

    @Test
    public void testPath() throws IOException {
        FileLinkPreferences preferences = new FileLinkPreferences(Collections.emptyList(),
                Collections.singletonList("src/test/resources/pdfs/"));
        formatter = new WrapFileLinks(preferences);
        formatter.setArgument("\\p");
        assertEquals(new File("src/test/resources/pdfs/encrypted.pdf").getCanonicalPath(),
                formatter.format("Preferences:encrypted.pdf:PDF"));
    }

    @Test
    public void testPathFallBackToGeneratedDir() throws IOException {
        FileLinkPreferences preferences = new FileLinkPreferences(Collections.singletonList("src/test/resources/pdfs/"),
                Collections.emptyList());
        formatter = new WrapFileLinks(preferences);
        formatter.setArgument("\\p");
        assertEquals(new File("src/test/resources/pdfs/encrypted.pdf").getCanonicalPath(),
                formatter.format("Preferences:encrypted.pdf:PDF"));
    }

    @Test
    public void testPathReturnsRelativePathIfNotFound() {
        FileLinkPreferences preferences = new FileLinkPreferences(Collections.emptyList(),
                Collections.singletonList("src/test/resources/pdfs/"));
        formatter = new WrapFileLinks(preferences);
        formatter.setArgument("\\p");
        assertEquals("test.pdf", formatter.format("Preferences:test.pdf:PDF"));
    }

    @Test
    public void testRelativePath() {
        formatter.setArgument("\\r");
        assertEquals("test.pdf", formatter.format("Test file:test.pdf:PDF"));
    }
}
