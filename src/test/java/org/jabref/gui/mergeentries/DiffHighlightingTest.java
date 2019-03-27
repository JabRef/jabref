package org.jabref.gui.mergeentries;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.text.Text;

import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

@GUITest
@ExtendWith(ApplicationExtension.class)
class DiffHighlightingTest {

    public static void assertEquals(List<Text> expected, List<Text> actual) {
        // Need to compare string values since Texts with the same string are not considered equal
        Assertions.assertEquals(expected.toString(), actual.toString());

        // Moreover, make sure that style classes are correct
        List<String> expectedStyles = expected.stream().map(text -> text.getStyleClass().toString()).collect(Collectors.toList());
        List<String> actualStyles = actual.stream().map(text -> text.getStyleClass().toString()).collect(Collectors.toList());
        Assertions.assertEquals(expectedStyles, actualStyles);
    }

    @Test
    void testGenerateDiffHighlightingBothNullThrowsNPE() {
        Assertions.assertThrows(NullPointerException.class, () -> DiffHighlighting.generateDiffHighlighting(null, null, ""));
    }

    @Test
    void testNullSeparatorThrowsNPE() {
        Assertions.assertThrows(NullPointerException.class, () -> DiffHighlighting.generateDiffHighlighting("", "", null));
    }

    @Test
    void testGenerateDiffHighlightingNoDiff() {
        assertEquals(
                Arrays.asList(
                        DiffHighlighting.forUnchanged("f"),
                        DiffHighlighting.forUnchanged("o"),
                        DiffHighlighting.forUnchanged("o")
                ),
                DiffHighlighting.generateDiffHighlighting("foo", "foo", ""));
    }

    @Test
    void testGenerateDiffHighlightingSingleWordAddTextWordDiff() {
        assertEquals(
                Arrays.asList(
                        DiffHighlighting.forRemoved("foo "),
                        DiffHighlighting.forAdded("foobar")
                ),
                DiffHighlighting.generateDiffHighlighting("foo", "foobar", " "));
    }

    @Test
    void testGenerateDiffHighlightingSingleWordAddTextCharacterDiff() {
        assertEquals(
                Arrays.asList(
                        DiffHighlighting.forUnchanged("f"),
                        DiffHighlighting.forUnchanged("o"),
                        DiffHighlighting.forUnchanged("o"),
                        DiffHighlighting.forAdded("bar")
                ),
                DiffHighlighting.generateDiffHighlighting("foo", "foobar", ""));
    }

    @Test
    void testGenerateDiffHighlightingSingleWordDeleteTextWordDiff() {
        assertEquals(
                Arrays.asList(
                        DiffHighlighting.forRemoved("foobar "),
                        DiffHighlighting.forAdded("foo")
                ),
                DiffHighlighting.generateDiffHighlighting("foobar", "foo", " "));
    }

    @Test
    void testGenerateDiffHighlightingSingleWordDeleteTextCharacterDiff() {
        assertEquals(
                Arrays.asList(
                        DiffHighlighting.forUnchanged("f"),
                        DiffHighlighting.forUnchanged("o"),
                        DiffHighlighting.forUnchanged("o"),
                        DiffHighlighting.forRemoved("b"),
                        DiffHighlighting.forRemoved("a"),
                        DiffHighlighting.forRemoved("r")
                ),
                DiffHighlighting.generateDiffHighlighting("foobar", "foo", ""));
    }

    @Test
    void generateSymmetricHighlightingSingleWordAddTextWordDiff() {
        assertEquals(
                Collections.singletonList(DiffHighlighting.forChanged("foo ")),
                DiffHighlighting.generateSymmetricHighlighting("foo", "foobar", " "));
    }

    @Test
    void generateSymmetricHighlightingSingleWordAddTextCharacterDiff() {
        assertEquals(
                Arrays.asList(
                        DiffHighlighting.forUnchanged("f"),
                        DiffHighlighting.forUnchanged("o"),
                        DiffHighlighting.forUnchanged("o")
                ),
                DiffHighlighting.generateSymmetricHighlighting("foo", "foobar", ""));
    }

    @Test
    void generateSymmetricHighlightingSingleWordDeleteTextWordDiff() {
        assertEquals(
                Collections.singletonList(DiffHighlighting.forChanged("foobar ")),
                DiffHighlighting.generateSymmetricHighlighting("foobar", "foo", " "));
    }

    @Test
    void generateSymmetricHighlightingSingleWordDeleteTextCharacterDiff() {
        assertEquals(
                Arrays.asList(
                        DiffHighlighting.forUnchanged("f"),
                        DiffHighlighting.forUnchanged("o"),
                        DiffHighlighting.forUnchanged("o"),
                        DiffHighlighting.forAdded("b"),
                        DiffHighlighting.forAdded("a"),
                        DiffHighlighting.forAdded("r")
                ),
                DiffHighlighting.generateSymmetricHighlighting("foobar", "foo", ""));
    }

    @Test
    void generateSymmetricHighlightingMultipleWordsDeleteTextCharacterDiff() {
        assertEquals(
                Arrays.asList(
                        DiffHighlighting.forUnchanged("f"),
                        DiffHighlighting.forUnchanged("o"),
                        DiffHighlighting.forUnchanged("o"),
                        DiffHighlighting.forAdded("b"),
                        DiffHighlighting.forAdded("a"),
                        DiffHighlighting.forAdded("r"),
                        DiffHighlighting.forUnchanged(" "),
                        DiffHighlighting.forUnchanged("a"),
                        DiffHighlighting.forUnchanged("n"),
                        DiffHighlighting.forUnchanged("d"),
                        DiffHighlighting.forUnchanged(" "),
                        DiffHighlighting.forAdded("s"),
                        DiffHighlighting.forAdded("o"),
                        DiffHighlighting.forAdded("m"),
                        DiffHighlighting.forAdded("e"),
                        DiffHighlighting.forUnchanged("t"),
                        DiffHighlighting.forUnchanged("h"),
                        DiffHighlighting.forUnchanged("i"),
                        DiffHighlighting.forUnchanged("n"),
                        DiffHighlighting.forUnchanged("g")
                ),
                DiffHighlighting.generateSymmetricHighlighting("foobar and something", "foo and thing", ""));
    }

    @Test
    void generateSymmetricHighlightingMultipleWordsDeleteTextWordDiff() {
        assertEquals(
                Arrays.asList(
                        DiffHighlighting.forUnchanged("foo "),
                        DiffHighlighting.forAdded("bar "),
                        DiffHighlighting.forUnchanged("and "),
                        DiffHighlighting.forAdded("some "),
                        DiffHighlighting.forUnchanged("thing ")
                ),
                DiffHighlighting.generateSymmetricHighlighting("foo bar and some thing", "foo and thing", " "));
    }
}
