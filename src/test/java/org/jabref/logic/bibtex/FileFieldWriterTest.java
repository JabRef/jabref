package org.jabref.logic.bibtex;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FileFieldWriterTest {

    @Test
    public void quoteStandard() {
        assertEquals("a", FileFieldWriter.quote("a"));
    }

    @Test
    public void quoteAllCharacters() {
        assertEquals("a\\:\\;\\\\", FileFieldWriter.quote("a:;\\"));
    }

    @Test
    public void quoteEmpty() {
        assertEquals("", FileFieldWriter.quote(""));
    }

    @Test
    public void quoteNull() {
        assertNull(FileFieldWriter.quote(null));
    }

    private static Stream<Arguments> getEncodingTestData() {
        return Stream.of(
                Arguments.of("a:b;c:d", new String[][]{{"a", "b"}, {"c", "d"}}),
                Arguments.of("a:;c:d", new String[][]{{"a", ""}, {"c", "d"}}),
                Arguments.of("a:" + null + ";c:d", new String[][]{{"a", null}, {"c", "d"}}),
                Arguments.of("a:\\:b;c\\;:d", new String[][]{{"a", ":b"}, {"c;", "d"}})
        );
    }

    @ParameterizedTest
    @MethodSource("getEncodingTestData")
    public void encodeStringArray(String expected, String[][] values) {
        assertEquals(expected, FileFieldWriter.encodeStringArray(values));
    }

    @Test
    public void fileFieldWriterGetStringRepresentation() {
        LinkedFile file = new LinkedFile("test", Path.of("X:\\Users\\abc.pdf"), "PDF");
        assertEquals("test:X\\:/Users/abc.pdf:PDF", FileFieldWriter.getStringRepresentation(file));
    }
}
