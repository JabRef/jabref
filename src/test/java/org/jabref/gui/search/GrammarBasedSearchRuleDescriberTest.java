package org.jabref.gui.search;

import java.util.Arrays;
import java.util.List;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.search.rules.describer.GrammarBasedSearchRuleDescriber;
import org.jabref.gui.util.TextUtil;
import org.jabref.model.search.rules.GrammarBasedSearchRule;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class GrammarBasedSearchRuleDescriberTest {

    private TextFlow createDescription(String query, boolean caseSensitive, boolean regExp) {
        GrammarBasedSearchRule grammarBasedSearchRule = new GrammarBasedSearchRule(caseSensitive, regExp);
        assertTrue(grammarBasedSearchRule.validateSearchStrings(query));
        GrammarBasedSearchRuleDescriber describer = new GrammarBasedSearchRuleDescriber(caseSensitive, regExp, grammarBasedSearchRule.getTree());
        return describer.getDescription();
    }

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
    public void testSimpleQueryCaseSensitiveRegex() {
        double textSize = 13;
        String query = "a=b";
        List<Text> expectedTexts = Arrays.asList(TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("a", textSize, TextUtil.TextType.BOLD),
                TextUtil.createText(" contains the regular expression ", textSize), TextUtil.createText("b", textSize, TextUtil.TextType.BOLD), TextUtil.createText(". ", textSize),
                TextUtil.createText("The search is case sensitive.", textSize));
        TextFlow description = createDescription(query, true, true);

        assertTrue(checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testSimpleQueryCaseSensitive() {
        double textSize = 13;
        String query = "a=b";
        List<Text> expectedTexts = Arrays.asList(TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("a", textSize, TextUtil.TextType.BOLD),
                TextUtil.createText(" contains the term ", textSize), TextUtil.createText("b", textSize, TextUtil.TextType.BOLD), TextUtil.createText(". ", textSize),
                TextUtil.createText("The search is case sensitive.", textSize));
        TextFlow description = createDescription(query, true, false);

        assertTrue(checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testSimpleQuery() {
        double textSize = 13;
        String query = "a=b";
        List<Text> expectedTexts = Arrays.asList(TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("a", textSize, TextUtil.TextType.BOLD),
                TextUtil.createText(" contains the term ", textSize), TextUtil.createText("b", textSize, TextUtil.TextType.BOLD), TextUtil.createText(". ", textSize),
                TextUtil.createText("The search is case insensitive.", textSize));
        TextFlow description = createDescription(query, false, false);

        assertTrue(checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testSimpleQueryRegex() {
        double textSize = 13;
        String query = "a=b";
        List<Text> expectedTexts = Arrays.asList(TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("a", textSize, TextUtil.TextType.BOLD),
                TextUtil.createText(" contains the regular expression ", textSize), TextUtil.createText("b", textSize, TextUtil.TextType.BOLD), TextUtil.createText(". ", textSize),
                TextUtil.createText("The search is case insensitive.", textSize));
        TextFlow description = createDescription(query, false, true);

        assertTrue(checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testComplexQueryCaseSensitiveRegex() {
        double textSize = 13;
        String query = "not a=b and c=e or e=\"x\"";
        List<Text> expectedTexts = Arrays.asList(TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("not ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("a", textSize, TextUtil.TextType.BOLD),
                TextUtil.createText(" contains the regular expression ", textSize), TextUtil.createText("b", textSize, TextUtil.TextType.BOLD), TextUtil.createText(" and ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("c", textSize, TextUtil.TextType.BOLD), TextUtil.createText(" contains the regular expression ", textSize),
                TextUtil.createText("e", textSize, TextUtil.TextType.BOLD), TextUtil.createText(" or ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("e", textSize, TextUtil.TextType.BOLD), TextUtil.createText(" contains the regular expression ", textSize),
                TextUtil.createText("x", textSize, TextUtil.TextType.BOLD), TextUtil.createText(". ", textSize), TextUtil.createText("The search is case sensitive.", textSize));
        TextFlow description = createDescription(query, true, true);

        assertTrue(checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testComplexQueryRegex() {
        double textSize = 13;
        String query = "not a=b and c=e or e=\"x\"";
        List<Text> expectedTexts = Arrays.asList(TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("not ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("a", textSize, TextUtil.TextType.BOLD),
                TextUtil.createText(" contains the regular expression ", textSize), TextUtil.createText("b", textSize, TextUtil.TextType.BOLD), TextUtil.createText(" and ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("c", textSize, TextUtil.TextType.BOLD), TextUtil.createText(" contains the regular expression ", textSize),
                TextUtil.createText("e", textSize, TextUtil.TextType.BOLD), TextUtil.createText(" or ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("e", textSize, TextUtil.TextType.BOLD), TextUtil.createText(" contains the regular expression ", textSize),
                TextUtil.createText("x", textSize, TextUtil.TextType.BOLD), TextUtil.createText(". ", textSize), TextUtil.createText("The search is case insensitive.", textSize));
        TextFlow description = createDescription(query, false, true);

        assertTrue(checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testComplexQueryCaseSensitive() {
        double textSize = 13;
        String query = "not a=b and c=e or e=\"x\"";
        List<Text> expectedTexts = Arrays.asList(TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("not ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("a", textSize, TextUtil.TextType.BOLD),
                TextUtil.createText(" contains the term ", textSize), TextUtil.createText("b", textSize, TextUtil.TextType.BOLD), TextUtil.createText(" and ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("c", textSize, TextUtil.TextType.BOLD), TextUtil.createText(" contains the term ", textSize), TextUtil.createText("e", textSize, TextUtil.TextType.BOLD),
                TextUtil.createText(" or ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("e", textSize, TextUtil.TextType.BOLD), TextUtil.createText(" contains the term ", textSize), TextUtil.createText("x", textSize, TextUtil.TextType.BOLD), TextUtil.createText(". ", textSize), TextUtil.createText("The search is case sensitive.", textSize));
        TextFlow description = createDescription(query, true, false);

        assertTrue(checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testComplexQuery() {
        double textSize = 13;
        String query = "not a=b and c=e or e=\"x\"";
        List<Text> expectedTexts = Arrays.asList(TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("not ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("a", textSize, TextUtil.TextType.BOLD),
                TextUtil.createText(" contains the term ", textSize), TextUtil.createText("b", textSize, TextUtil.TextType.BOLD), TextUtil.createText(" and ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("c", textSize, TextUtil.TextType.BOLD), TextUtil.createText(" contains the term ", textSize), TextUtil.createText("e", textSize, TextUtil.TextType.BOLD),
                TextUtil.createText(" or ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createText("e", textSize, TextUtil.TextType.BOLD), TextUtil.createText(" contains the term ", textSize), TextUtil.createText("x", textSize, TextUtil.TextType.BOLD), TextUtil.createText(". ", textSize), TextUtil.createText("The search is case insensitive.", textSize));
        TextFlow description = createDescription(query, false, false);

        assertTrue(checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }
}
