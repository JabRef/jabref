package org.jabref.gui.mergeentries;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.text.Text;

import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void generateDiffHighlightingBothNullThrowsNPE() {
        assertThrows(NullPointerException.class, () -> DiffHighlighting.generateDiffHighlighting(null, null, ""));
    }

    @Test
    void nullSeparatorThrowsNPE() {
        assertThrows(NullPointerException.class, () -> DiffHighlighting.generateDiffHighlighting("", "", null));
    }

    @Test
    void generateDiffHighlightingNoDiff() {
        assertEquals(
                Arrays.asList(
                        DiffHighlighting.forUnchanged("f"),
                        DiffHighlighting.forUnchanged("o"),
                        DiffHighlighting.forUnchanged("o")
                ),
                DiffHighlighting.generateDiffHighlighting("foo", "foo", ""));
    }

    @Test
    void generateDiffHighlightingSingleWordAddTextWordDiff() {
        assertEquals(
                Arrays.asList(
                        DiffHighlighting.forRemoved("foo "),
                        DiffHighlighting.forAdded("foobar")
                ),
                DiffHighlighting.generateDiffHighlighting("foo", "foobar", " "));
    }

    @Test
    void generateDiffHighlightingSingleWordAddTextCharacterDiff() {
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
    void generateDiffHighlightingSingleWordDeleteTextWordDiff() {
        assertEquals(
                Arrays.asList(
                        DiffHighlighting.forRemoved("foobar "),
                        DiffHighlighting.forAdded("foo")
                ),
                DiffHighlighting.generateDiffHighlighting("foobar", "foo", " "));
    }

    @Test
    void generateDiffHighlightingSingleWordDeleteTextCharacterDiff() {
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
}
