package org.jabref.gui.search;

import java.util.List;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * @author jpf
 * @created 11/26/17
 */
public class TextFlowEqualityHelper {

    public static boolean checkIfDescriptionEqualsExpectedTexts(TextFlow description, List<Text> expectedTexts) {
        if (expectedTexts.size() != description.getChildren().size())
            return false;
        Text expectedText;
        for (int i = 0; i < expectedTexts.size(); i++) {
            expectedText = expectedTexts.get(i);
            // the strings contain not only the text but also the font and other properties
            // so comparing them compares the Text object as a whole
            // the equals method is not implemented...
            if (!expectedText.toString().equals(description.getChildren().get(i).toString()))
                return false;
        }
        return true;
    }

    public static boolean checkIfTextsEqualsExpectedTexts(List<Text> texts, List<Text> expectedTexts) {
        if (expectedTexts.size() != texts.size())
            return false;
        Text expectedText;
        for (int i = 0; i < expectedTexts.size(); i++) {
            expectedText = expectedTexts.get(i);
            // the strings contain not only the text but also the font and other properties
            // so comparing them compares the Text object as a whole
            // the equals method is not implemented...
            if (!expectedText.toString().equals(texts.get(i).toString()))
                return false;
        }
        return true;
    }
}
