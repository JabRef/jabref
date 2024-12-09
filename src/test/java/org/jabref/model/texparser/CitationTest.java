package org.jabref.model.texparser;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CitationTest {

    Path path;
    int line;
    String key;
    String lineText;
    Citation citation;

    @BeforeEach
    void init() {
        path = Path.of("test");
        line = 10;
        lineText = "lineText";
        citation = new Citation(path, line, 1, 4, lineText);
    }

    private static Stream<Arguments> colStartColEndNotInBounds() {
        return Stream.of(
                arguments(-1, 2),
                arguments(1, 9)
        );
    }

    private static Stream<Arguments> colStartColEndInBounds() {
        return Stream.of(
                arguments(0, 2),
                arguments(1, 8)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void constructorLineSmallerEqualZeroTest(int line) {
        Exception e = assertThrows(IllegalArgumentException.class, () -> new Citation(path, line, 1, 5, lineText));
        assertEquals("Line has to be greater than 0.", e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void constructorLineLargerZeroTest(int line) {
        Citation citation = new Citation(path, line, 1, 5, lineText);
    }

    @ParameterizedTest
    @MethodSource("colStartColEndNotInBounds")
    void constructorColStartColEndNotInBoundsTest(int colStart, int colEnd) {
        Exception e = assertThrows(IllegalArgumentException.class, () -> new Citation(path, line, colStart, colEnd, lineText));
        assertEquals("Citation has to be between 0 and line length.", e.getMessage());
    }

    @ParameterizedTest
    @MethodSource("colStartColEndInBounds")
    void constructorColStartColEndInBoundsTest(int colStart, int colEnd) {
        Citation citation = new Citation(path, line, colStart, colEnd, lineText);
    }

    @Test
    void getPathTest() {
        assertEquals(path, citation.path());
    }

    @Test
    void getLineTest() {
        assertEquals(10, citation.line());
    }

    @Test
    void getColStartTest() {
        assertEquals(1, citation.colStart());
    }

    @Test
    void getColEndTest() {
        assertEquals(4, citation.colEnd());
    }

    @Test
    void getLineTextTest() {
        assertEquals(lineText, citation.lineText());
    }

    @Test
    void getContextTest() {
        assertEquals(lineText, citation.getContext());
    }

    @Test
    void equalsTest() {
        Citation citation1 = new Citation(path, line, 1, 4, lineText);
        Citation citation2 = null;
        assertEquals(citation, citation1);
        assertNotEquals(citation, citation2);
    }
}
