package org.jabref.gui.search;

import java.util.Arrays;
import java.util.List;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.search.rules.describer.ContainsAndRegexBasedSearchRuleDescriber;
import org.jabref.gui.util.TextUtil;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ContainsAndRegexBasedSearchRuleDescriberTest {

    private boolean checkIfDescriptionEqualsExpectedTexts(TextFlow description, List<Text> expectedTexts) {
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

    @Test
    public void testNoAst() {
        double textSize = 13;
        String query = "a b";
        List<Text> expectedTexts = Arrays.asList(TextUtil.createText("This search contains entries in which any field contains the term ", textSize),
                TextUtil.createTextBold("a", textSize), TextUtil.createText(" and ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(" (case insensitive). ", textSize),
                TextUtil.createText("\n\nHint: To search specific fields only, enter for example:\n", textSize), TextUtil.createTextMonospaced("author=smith and title=electrical", textSize));
        TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(false, false, query).getDescription();

        assertTrue(checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testNoAstRegex() {
        double textSize = 13;
        String query = "a b";
        List<Text> expectedTexts = Arrays.asList(TextUtil.createText("This search contains entries in which any field contains the regular expression ", textSize),
                TextUtil.createTextBold("a", textSize), TextUtil.createText(" and ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(" (case insensitive). ", textSize),
                TextUtil.createText("\n\nHint: To search specific fields only, enter for example:\n", textSize), TextUtil.createTextMonospaced("author=smith and title=electrical", textSize));
        TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(false, true, query).getDescription();

        assertTrue(checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testNoAstRegexCaseSensitive() {
        double textSize = 13;
        String query = "a b";
        List<Text> expectedTexts = Arrays.asList(TextUtil.createText("This search contains entries in which any field contains the regular expression ", textSize),
                TextUtil.createTextBold("a", textSize), TextUtil.createText(" and ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(" (case sensitive). ", textSize),
                TextUtil.createText("\n\nHint: To search specific fields only, enter for example:\n", textSize), TextUtil.createTextMonospaced("author=smith and title=electrical", textSize));
        TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(true, true, query).getDescription();

        assertTrue(checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testNoAstCaseSensitive() {
        double textSize = 13;
        String query = "a b";
        List<Text> expectedTexts = Arrays.asList(TextUtil.createText("This search contains entries in which any field contains the term ", textSize),
                TextUtil.createTextBold("a", textSize), TextUtil.createText(" and ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(" (case sensitive). ", textSize),
                TextUtil.createText("\n\nHint: To search specific fields only, enter for example:\n", textSize), TextUtil.createTextMonospaced("author=smith and title=electrical", textSize));
        TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(true, false, query).getDescription();

        assertTrue(checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }
}
