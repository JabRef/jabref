package org.jabref.gui.search;

import java.util.Arrays;
import java.util.List;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.search.rules.describer.ContainsAndRegexBasedSearchRuleDescriber;
import org.jabref.gui.util.TooltipTextUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContainsAndRegexBasedSearchRuleDescriberTest {

    @Test
    public void testNoAst() {
        String query = "a b";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which any field contains the term "),
                TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" and "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" (case insensitive). "),
                TooltipTextUtil.createText("\n\nHint: To search specific fields only, enter for example:\n"), TooltipTextUtil.createText("author=smith and title=electrical", TooltipTextUtil.TextType.MONOSPACED));
        TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(false, false, query).getDescription();

        assertTrue(TextFlowEqualityHelper.checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testNoAstRegex() {
        String query = "a b";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which any field contains the regular expression "),
                TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" and "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" (case insensitive). "),
                TooltipTextUtil.createText("\n\nHint: To search specific fields only, enter for example:\n"), TooltipTextUtil.createText("author=smith and title=electrical", TooltipTextUtil.TextType.MONOSPACED));
        TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(false, true, query).getDescription();

        assertTrue(TextFlowEqualityHelper.checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testNoAstRegexCaseSensitive() {
        String query = "a b";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which any field contains the regular expression "),
                TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" and "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" (case sensitive). "),
                TooltipTextUtil.createText("\n\nHint: To search specific fields only, enter for example:\n"), TooltipTextUtil.createText("author=smith and title=electrical", TooltipTextUtil.TextType.MONOSPACED));
        TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(true, true, query).getDescription();

        assertTrue(TextFlowEqualityHelper.checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }

    @Test
    public void testNoAstCaseSensitive() {
        String query = "a b";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which any field contains the term "),
                TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" and "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" (case sensitive). "),
                TooltipTextUtil.createText("\n\nHint: To search specific fields only, enter for example:\n"), TooltipTextUtil.createText("author=smith and title=electrical", TooltipTextUtil.TextType.MONOSPACED));
        TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(true, false, query).getDescription();

        assertTrue(TextFlowEqualityHelper.checkIfDescriptionEqualsExpectedTexts(description, expectedTexts));
    }
}
