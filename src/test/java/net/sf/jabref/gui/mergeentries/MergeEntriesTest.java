package net.sf.jabref.gui.mergeentries;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class MergeEntriesTest {

    @Test
    public void testGenerateDiffHighlightingBothNullReturnsNull() {
        assertNull(MergeEntries.generateDiffHighlighting(null, null, ""));
    }

    @Test(expected = NullPointerException.class)
    public void testNullSeparatorThrowsNPE() {
        assertNull(MergeEntries.generateDiffHighlighting("", "", null));
    }

    @Test
    public void testGenerateDiffHighlightingNoDiff() {
        assertEquals("foo", MergeEntries.generateDiffHighlighting("foo", "foo", ""));
    }

    @Test
    public void testGenerateDiffHighlightingSingleWordAddTextWordDiff() {
        assertEquals("<span class=del>foo</span> <span class=add>foobar</span>",
                MergeEntries.generateDiffHighlighting("foo", "foobar", " "));
    }

    @Test
    public void testGenerateDiffHighlightingSingleWordAddTextCharacterDiff() {
        assertEquals("foo<span class=add>bar</span>", MergeEntries.generateDiffHighlighting("foo", "foobar", ""));
    }

    @Test
    public void testGenerateDiffHighlightingSingleWordDeleteTextWordDiff() {
        assertEquals("<span class=del>foobar</span> <span class=add>foo</span>",
                MergeEntries.generateDiffHighlighting("foobar", "foo", " "));
    }

    @Test
    public void testGenerateDiffHighlightingSingleWordDeleteTextCharacterDiff() {
        assertEquals("foo<span class=del>bar</span>", MergeEntries.generateDiffHighlighting("foobar", "foo", ""));
    }
}
