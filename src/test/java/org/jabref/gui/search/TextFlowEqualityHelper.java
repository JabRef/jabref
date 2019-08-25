package org.jabref.gui.search;

import java.util.List;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.junit.jupiter.api.Assertions;

public class TextFlowEqualityHelper {

    public static void assertEquals(List<Text> expectedTexts, TextFlow description) {
        if (expectedTexts.size() != description.getChildren().size()) {
            Assertions.assertEquals(expectedTexts, description.getChildren());
            return;
        }
        Text expectedText;
        for (int i = 0; i < expectedTexts.size(); i++) {
            expectedText = expectedTexts.get(i);
            // the strings contain not only the text but also the font and other properties
            // so comparing them compares the Text object as a whole
            // the equals method is not implemented...
            if (!expectedText.toString().equals(description.getChildren().get(i).toString())) {
                Assertions.assertEquals(expectedTexts, description.getChildren());
                return;
            }
        }
    }

    public static boolean checkIfTextsEqualsExpectedTexts(List<Text> texts, List<Text> expectedTexts) {
        if (expectedTexts.size() != texts.size()) {
            return false;
        }
        Text expectedText;
        for (int i = 0; i < expectedTexts.size(); i++) {
            expectedText = expectedTexts.get(i);
            // the strings contain not only the text but also the font and other properties
            // so comparing them compares the Text object as a whole
            // the equals method is not implemented...
            if (!expectedText.toString().equals(texts.get(i).toString())) {
                return false;
            }
        }
        return true;
    }
}
