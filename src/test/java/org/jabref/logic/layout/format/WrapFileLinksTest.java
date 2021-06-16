package org.jabref.logic.layout.format;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WrapFileLinksTest {

    private WrapFileLinks formatter;

    @BeforeEach
    void setUp() {
        FileLinkPreferences preferences = new FileLinkPreferences("", Collections.emptyList());
        formatter = new WrapFileLinks(preferences);
    }

    @Test
    void testEmpty() {
        assertEquals("", formatter.format(""));
    }

    @Test
    void testNull() {
        assertEquals("", formatter.format(null));
    }

    void testNoFormatSetNonEmptyString() {
        assertThrows(NullPointerException.class, () -> formatter.format("test.pdf"));
    }

    @Test
    void testFileExtension() {
        formatter.setArgument("\\x");
        assertEquals("pdf", formatter.format("test.pdf"));
    }

    @Test
    void testFileExtensionNoExtension() {
        formatter.setArgument("\\x");
        assertEquals("", formatter.format("test"));
    }

    @Test
    void testPlainTextString() {
        formatter.setArgument("x");
        assertEquals("x", formatter.format("test.pdf"));
    }

    @Test
    void testDescription() {
        formatter.setArgument("\\d");
        assertEquals("Test file", formatter.format("Test file:test.pdf:PDF"));
    }

    @Test
    void testDescriptionNoDescription() {
        formatter.setArgument("\\d");
        assertEquals("", formatter.format("test.pdf"));
    }

    @Test
    void testType() {
        formatter.setArgument("\\f");
        assertEquals("PDF", formatter.format("Test file:test.pdf:PDF"));
    }

    @Test
    void testTypeNoType() {
        formatter.setArgument("\\f");
        assertEquals("", formatter.format("test.pdf"));
    }

    @Test
    void testIterator() {
        formatter.setArgument("\\i");
        assertEquals("1", formatter.format("Test file:test.pdf:PDF"));
    }

    @Test
    void testIteratorTwoItems() {
        formatter.setArgument("\\i\n");
        assertEquals("1\n2\n", formatter.format("Test file:test.pdf:PDF;test2.pdf"));
    }

    @Test
    void testEndingBracket() {
        formatter.setArgument("(\\d)");
        assertEquals("(Test file)", formatter.format("Test file:test.pdf:PDF"));
    }

    @Test
    void testPath() throws IOException {
        FileLinkPreferences preferences = new FileLinkPreferences("",
                Collections.singletonList(Path.of("src/test/resources/pdfs/")));
        formatter = new WrapFileLinks(preferences);
        formatter.setArgument("\\p");
        assertEquals(new File("src/test/resources/pdfs/encrypted.pdf").getCanonicalPath(),
                formatter.format("Preferences:encrypted.pdf:PDF"));
    }

    @Test
    void testPathFallBackToGeneratedDir() throws IOException {
        FileLinkPreferences preferences = new FileLinkPreferences("src/test/resources/pdfs/",
                Collections.emptyList());
        formatter = new WrapFileLinks(preferences);
        formatter.setArgument("\\p");
        assertEquals(new File("src/test/resources/pdfs/encrypted.pdf").getCanonicalPath(),
                formatter.format("Preferences:encrypted.pdf:PDF"));
    }

    @Test
    void testPathReturnsRelativePathIfNotFound() {
        FileLinkPreferences preferences = new FileLinkPreferences("",
                Collections.singletonList(Path.of("src/test/resources/pdfs/")));
        formatter = new WrapFileLinks(preferences);
        formatter.setArgument("\\p");
        assertEquals("test.pdf", formatter.format("Preferences:test.pdf:PDF"));
    }

    @Test
    void testRelativePath() {
        formatter.setArgument("\\r");
        assertEquals("test.pdf", formatter.format("Test file:test.pdf:PDF"));
    }
}
