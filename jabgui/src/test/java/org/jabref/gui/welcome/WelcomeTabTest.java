package org.jabref.gui.welcome;

import javafx.scene.text.Font;
import javafx.scene.text.Text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WelcomeTabTest {

    private static final Font FONT = Font.getDefault();

    @Test
    void returnsFullPathWhenThereIsEnoughSpace() {
        String path = "/home/user/Libraries/MyLibrary.bib";
        double width = textWidth(path);
        String result = WelcomeTab.abbreviatePathToFit(path, width, FONT);
        assertEquals(path, result);
    }

    @Test
    void abbreviatesPathWhenWidthIsInsufficient() {
        String path = "/home/user/Documents/Libraries/MyLibrary.bib";
        double width = textWidth(path) / 2;
        String result = WelcomeTab.abbreviatePathToFit(path, width, FONT);
        assertNotEquals(path, result);
        assertNotNull(result);
        assertTrue(result.endsWith("MyLibrary.bib"));
        assertTrue(result.contains("..."));
    }

    @Test
    void preservesNumberedPrefix() {
        String path = "1. /home/user/Documents/Libraries/MyLibrary.bib";
        double width = textWidth(path) / 2;
        String result = WelcomeTab.abbreviatePathToFit(path, width, FONT);
        assertNotNull(result);
        assertTrue(result.startsWith("1. "));
        assertTrue(result.endsWith("MyLibrary.bib"));
    }

    @Test
    void preservesFilenameWhenPathIsAbbreviated() {
        String path = "/home/user/very/long/path/to/MyLibrary.bib";
        double width = textWidth(path) / 2;
        String result = WelcomeTab.abbreviatePathToFit(path, width, FONT);
        assertNotNull(result);
        assertTrue(result.endsWith("MyLibrary.bib"));
    }

    @Test
    void preservesFilenameWithVeryNarrowWidth() {
        String path = "/home/user/Documents/MyLibrary.bib";
        String result = WelcomeTab.abbreviatePathToFit(path, 1, FONT);
        assertNotNull(result);
        assertTrue(result.endsWith("MyLibrary.bib"));
    }

    private static double textWidth(String text) {
        Text renderedText = new Text(text);
        renderedText.setFont(FONT);
        return renderedText.getLayoutBounds().getWidth();
    }
}
