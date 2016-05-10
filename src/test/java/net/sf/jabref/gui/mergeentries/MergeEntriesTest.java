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

    @Test
    public void generateSymmetricHighlightingSingleWordAddTextWordDiff() {
        assertEquals("<span class=change>foo</span>",
                MergeEntries.generateSymmetricHighlighting("foo", "foobar", " "));
    }

    @Test
    public void generateSymmetricHighlightingSingleWordAddTextCharacterDiff() {
        assertEquals("foo", MergeEntries.generateSymmetricHighlighting("foo", "foobar", ""));
    }

    @Test
    public void generateSymmetricHighlightingSingleWordDeleteTextWordDiff() {
        assertEquals("<span class=change>foobar</span>",
                MergeEntries.generateSymmetricHighlighting("foobar", "foo", " "));
    }

    @Test
    public void generateSymmetricHighlightingSingleWordDeleteTextCharacterDiff() {
        assertEquals("foo<span class=add>bar</span>", MergeEntries.generateSymmetricHighlighting("foobar", "foo", ""));
    }

    @Test
    public void generateSymmetricHighlightingMultipleWordsDeleteTextCharacterDiff() {
        assertEquals("foo<span class=add>bar</span> and <span class=add>some</span>thing",
                MergeEntries.generateSymmetricHighlighting("foobar and something", "foo and thing", ""));
    }

    @Test
    public void generateSymmetricHighlightingMultipleWordsDeleteTextWordDiff() {
        assertEquals("foo <span class=add>bar</span> and <span class=add>some</span> thing",
                MergeEntries.generateSymmetricHighlighting("foo bar and some thing", "foo and thing", " "));
    }
}
