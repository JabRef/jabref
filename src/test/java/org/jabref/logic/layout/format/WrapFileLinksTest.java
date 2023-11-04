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
        formatter = new WrapFileLinks(Collections.emptyList(), "");
    }

    @Test
    void testEmpty() {
        assertEquals("", formatter.format(""));
    }

    @Test
    void testNull() {
        assertEquals("", formatter.format(null));
    }

    @Test
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
        formatter = new WrapFileLinks(Collections.singletonList(Path.of("src/test/resources/pdfs/")), "");
        formatter.setArgument("\\p");
        assertEquals(new File("src/test/resources/pdfs/encrypted.pdf").getCanonicalPath(),
                formatter.format("Preferences:encrypted.pdf:PDF"));
    }

    @Test
    void testPathFallBackToGeneratedDir() throws IOException {
        formatter = new WrapFileLinks(Collections.emptyList(), "src/test/resources/pdfs/");
        formatter.setArgument("\\p");
        assertEquals(new File("src/test/resources/pdfs/encrypted.pdf").getCanonicalPath(),
                formatter.format("Preferences:encrypted.pdf:PDF"));
    }

    @Test
    void testPathReturnsRelativePathIfNotFound() {
        formatter = new WrapFileLinks(Collections.singletonList(Path.of("src/test/resources/pdfs/")), "");
        formatter.setArgument("\\p");
        assertEquals("test.pdf", formatter.format("Preferences:test.pdf:PDF"));
    }

    @Test
    void testRelativePath() {
        formatter.setArgument("\\r");
        assertEquals("test.pdf", formatter.format("Test file:test.pdf:PDF"));
    }
}
