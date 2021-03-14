package org.jabref.model.texparser;

import java.nio.file.Path;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CitationTest {

    Path path;
    Citation citation;

    @BeforeEach
    public void init() {
        path = Path.of("test");
        citation = new Citation(path, 10, 1, 4, "lineText");
    }

    @Test
    public void constructorLineSmallerEqualZeroTest() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> new Citation(path, 0, 1, 5, "lineText"));
        assertEquals("Line has to be greater than 0.", e.getMessage());
    }

    @Test
    public void constructorColStartColEndNotInBoundsTest() {
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> new Citation(path, 10, -3, 2, "lineText"));
        assertEquals("Citation has to be between 0 and line length.", e1.getMessage());
        Exception e2 = assertThrows(IllegalArgumentException.class, () -> new Citation(path, 10, 1, 10, "lineText"));
        assertEquals("Citation has to be between 0 and line length.", e2.getMessage());
    }

    @Test
    public void getPathTest() {
        assertEquals(path, citation.getPath());
    }

    @Test
    public void getLineTest() {
        assertEquals(10, citation.getLine());
    }

    @Test
    public void getColStartTest() {
        assertEquals(1, citation.getColStart());
    }

    @Test
    public void getColEndTest() {
        assertEquals(4, citation.getColEnd());
    }

    @Test
    public void getLineTextTest() {
        assertEquals("lineText", citation.getLineText());
    }

    @Test
    public void getContextTest() {
        assertEquals("lineText", citation.getContext());
    }

    @Test
    public void toStringTest() {
        assertEquals("Citation{path=test, line=10, colStart=1, colEnd=4, lineText='lineText'}", citation.toString());
    }

    @Test
    public void equalsTest() {
        Citation citation1 = new Citation(path, 10, 1, 4, "lineText");
        Citation citation2 = null;
        assertTrue(citation.equals(citation1));
        assertFalse(citation.equals(citation2));
    }

    @Test
    public void hashCodeTest() {
        assertEquals(Objects.hash(path, 10, 1, 4, "lineText"), citation.hashCode());
    }
}
