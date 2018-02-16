package org.jabref.gui.search;

import java.util.Arrays;
import java.util.List;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.search.rules.describer.GrammarBasedSearchRuleDescriber;
import org.jabref.gui.util.TooltipTextUtil;
import org.jabref.model.search.rules.GrammarBasedSearchRule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GrammarBasedSearchRuleDescriberTest {

    private TextFlow createDescription(String query, boolean caseSensitive, boolean regExp) {
        GrammarBasedSearchRule grammarBasedSearchRule = new GrammarBasedSearchRule(caseSensitive, regExp);
        assertTrue(grammarBasedSearchRule.validateSearchStrings(query));
        GrammarBasedSearchRuleDescriber describer = new GrammarBasedSearchRuleDescriber(caseSensitive, regExp, grammarBasedSearchRule.getTree());
        return describer.getDescription();
    }

    @Test
    public void testSimpleQueryCaseSensitiveRegex() {
        double textSize = 13;
        String query = "a=b";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the regular expression "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "),
                TooltipTextUtil.createText("The search is case sensitive."));
        TextFlow description = createDescription(query, true, true);

        assertTrue(TextFlowEqualityHelper.checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testSimpleQueryCaseSensitive() {
        double textSize = 13;
        String query = "a=b";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "),
                TooltipTextUtil.createText("The search is case sensitive."));
        TextFlow description = createDescription(query, true, false);

        assertTrue(TextFlowEqualityHelper.checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testSimpleQuery() {
        double textSize = 13;
        String query = "a=b";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "),
                TooltipTextUtil.createText("The search is case insensitive."));
        TextFlow description = createDescription(query, false, false);

        assertTrue(TextFlowEqualityHelper.checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testSimpleQueryRegex() {
        double textSize = 13;
        String query = "a=b";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the regular expression "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "),
                TooltipTextUtil.createText("The search is case insensitive."));
        TextFlow description = createDescription(query, false, true);

        assertTrue(TextFlowEqualityHelper.checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testComplexQueryCaseSensitiveRegex() {
        double textSize = 13;
        String query = "not a=b and c=e or e=\"x\"";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("not "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the regular expression "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" and "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("c", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the regular expression "),
                TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" or "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the regular expression "),
                TooltipTextUtil.createText("x", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "), TooltipTextUtil.createText("The search is case sensitive."));
        TextFlow description = createDescription(query, true, true);

        assertTrue(TextFlowEqualityHelper.checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testComplexQueryRegex() {
        double textSize = 13;
        String query = "not a=b and c=e or e=\"x\"";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("not "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the regular expression "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" and "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("c", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the regular expression "),
                TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" or "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the regular expression "),
                TooltipTextUtil.createText("x", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "), TooltipTextUtil.createText("The search is case insensitive."));
        TextFlow description = createDescription(query, false, true);

        assertTrue(TextFlowEqualityHelper.checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testComplexQueryCaseSensitive() {
        double textSize = 13;
        String query = "not a=b and c=e or e=\"x\"";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("not "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" and "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("c", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" or "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("x", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "), TooltipTextUtil.createText("The search is case sensitive."));
        TextFlow description = createDescription(query, true, false);

        assertTrue(TextFlowEqualityHelper.checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testComplexQuery() {
        double textSize = 13;
        String query = "not a=b and c=e or e=\"x\"";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("not "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" and "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("c", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" or "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("x", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "), TooltipTextUtil.createText("The search is case insensitive."));
        TextFlow description = createDescription(query, false, false);

        assertTrue(TextFlowEqualityHelper.checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }
}
