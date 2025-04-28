package org.jabref.logic.layout.format;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WrapFileLinksTest {

    private WrapFileLinks formatter;

    @BeforeEach
    void setUp() {
        formatter = new WrapFileLinks(List.of(), "");
    }

    @Test
    void empty() {
        assertEquals("", formatter.format(""));
    }

    @Test
    void testNull() {
        assertEquals("", formatter.format(null));
    }

    @Test
    void noFormatSetNonEmptyString() {
        assertThrows(NullPointerException.class, () -> formatter.format("test.pdf"));
    }

    @Test
    void fileExtension() {
        formatter.setArgument("\\x");
        assertEquals("pdf", formatter.format("test.pdf"));
    }

    @Test
    void fileExtensionNoExtension() {
        formatter.setArgument("\\x");
        assertEquals("", formatter.format("test"));
    }

    @Test
    void plainTextString() {
        formatter.setArgument("x");
        assertEquals("x", formatter.format("test.pdf"));
    }

    @Test
    void description() {
        formatter.setArgument("\\d");
        assertEquals("Test file", formatter.format("Test file:test.pdf:PDF"));
    }

    @Test
    void descriptionNoDescription() {
        formatter.setArgument("\\d");
        assertEquals("", formatter.format("test.pdf"));
    }

    @Test
    void type() {
        formatter.setArgument("\\f");
        assertEquals("PDF", formatter.format("Test file:test.pdf:PDF"));
    }

    @Test
    void typeNoType() {
        formatter.setArgument("\\f");
        assertEquals("", formatter.format("test.pdf"));
    }

    @Test
    void iterator() {
        formatter.setArgument("\\i");
        assertEquals("1", formatter.format("Test file:test.pdf:PDF"));
    }

    @Test
    void iteratorTwoItems() {
        formatter.setArgument("\\i\n");
        assertEquals("1\n2\n", formatter.format("Test file:test.pdf:PDF;test2.pdf"));
    }

    @Test
    void endingBracket() {
        formatter.setArgument("(\\d)");
        assertEquals("(Test file)", formatter.format("Test file:test.pdf:PDF"));
    }

    @Test
    void path() throws IOException {
        formatter = new WrapFileLinks(List.of(Path.of("src/test/resources/pdfs/")), "");
        formatter.setArgument("\\p");
        assertEquals(Path.of("src/test/resources/pdfs/encrypted.pdf").toRealPath().toString(),
                formatter.format("Preferences:encrypted.pdf:PDF"));
    }

    @Test
    void pathFallBackToGeneratedDir() throws IOException {
        formatter = new WrapFileLinks(List.of(), "src/test/resources/pdfs/");
        formatter.setArgument("\\p");
        assertEquals(Path.of("src/test/resources/pdfs/encrypted.pdf").toRealPath().toString(),
                formatter.format("Preferences:encrypted.pdf:PDF"));
    }

    @Test
    void pathReturnsRelativePathIfNotFound() {
        formatter = new WrapFileLinks(List.of(Path.of("src/test/resources/pdfs/")), "");
        formatter.setArgument("\\p");
        assertEquals("test.pdf", formatter.format("Preferences:test.pdf:PDF"));
    }

    @Test
    void relativePath() {
        formatter.setArgument("\\r");
        assertEquals("test.pdf", formatter.format("Test file:test.pdf:PDF"));
    }
}
