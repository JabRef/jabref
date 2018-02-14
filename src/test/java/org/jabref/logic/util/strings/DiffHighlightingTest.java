package org.jabref.logic.util.strings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DiffHighlightingTest {

    @Test
    public void testGenerateDiffHighlightingBothNullReturnsNull() {
        assertNull(DiffHighlighting.generateDiffHighlighting(null, null, ""));
    }

    @Test
    public void testNullSeparatorThrowsNPE() {
        assertThrows(NullPointerException.class, () -> DiffHighlighting.generateDiffHighlighting("", "", null));
    }

    @Test
    public void testGenerateDiffHighlightingNoDiff() {
        assertEquals("foo", DiffHighlighting.generateDiffHighlighting("foo", "foo", ""));
    }

    @Test
    public void testGenerateDiffHighlightingSingleWordAddTextWordDiff() {
        assertEquals("<span class=del>foo</span> <span class=add>foobar</span>",
                DiffHighlighting.generateDiffHighlighting("foo", "foobar", " "));
    }

    @Test
    public void testGenerateDiffHighlightingSingleWordAddTextCharacterDiff() {
        assertEquals("foo<span class=add>bar</span>", DiffHighlighting.generateDiffHighlighting("foo", "foobar", ""));
    }

    @Test
    public void testGenerateDiffHighlightingSingleWordDeleteTextWordDiff() {
        assertEquals("<span class=del>foobar</span> <span class=add>foo</span>",
                DiffHighlighting.generateDiffHighlighting("foobar", "foo", " "));
    }

    @Test
    public void testGenerateDiffHighlightingSingleWordDeleteTextCharacterDiff() {
        assertEquals("foo<span class=del>bar</span>", DiffHighlighting.generateDiffHighlighting("foobar", "foo", ""));
    }

    @Test
    public void generateSymmetricHighlightingSingleWordAddTextWordDiff() {
        assertEquals("<span class=change>foo</span>",
                DiffHighlighting.generateSymmetricHighlighting("foo", "foobar", " "));
    }

    @Test
    public void generateSymmetricHighlightingSingleWordAddTextCharacterDiff() {
        assertEquals("foo", DiffHighlighting.generateSymmetricHighlighting("foo", "foobar", ""));
    }

    @Test
    public void generateSymmetricHighlightingSingleWordDeleteTextWordDiff() {
        assertEquals("<span class=change>foobar</span>",
                DiffHighlighting.generateSymmetricHighlighting("foobar", "foo", " "));
    }

    @Test
    public void generateSymmetricHighlightingSingleWordDeleteTextCharacterDiff() {
        assertEquals("foo<span class=add>bar</span>", DiffHighlighting.generateSymmetricHighlighting("foobar", "foo", ""));
    }

    @Test
    public void generateSymmetricHighlightingMultipleWordsDeleteTextCharacterDiff() {
        assertEquals("foo<span class=add>bar</span> and <span class=add>some</span>thing",
                DiffHighlighting.generateSymmetricHighlighting("foobar and something", "foo and thing", ""));
    }

    @Test
    public void generateSymmetricHighlightingMultipleWordsDeleteTextWordDiff() {
        assertEquals("foo <span class=add>bar</span> and <span class=add>some</span> thing",
                DiffHighlighting.generateSymmetricHighlighting("foo bar and some thing", "foo and thing", " "));
    }
}
