package org.jabref.gui.search;

import java.util.Arrays;
import java.util.List;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import org.jabref.gui.search.rules.describer.GrammarBasedSearchRuleDescriber;
import org.jabref.gui.util.TooltipTextUtil;
import org.jabref.model.search.rules.GrammarBasedSearchRule;
import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertTrue;

@GUITest
@ExtendWith(ApplicationExtension.class)
class GrammarBasedSearchRuleDescriberTest {

    @Start
    void onStart(Stage stage) {
        // Needed to init JavaFX thread
        stage.show();
    }

    private TextFlow createDescription(String query, boolean caseSensitive, boolean regExp) {
        GrammarBasedSearchRule grammarBasedSearchRule = new GrammarBasedSearchRule(caseSensitive, regExp);
        assertTrue(grammarBasedSearchRule.validateSearchStrings(query));
        GrammarBasedSearchRuleDescriber describer = new GrammarBasedSearchRuleDescriber(caseSensitive, regExp, grammarBasedSearchRule.getTree());
        return describer.getDescription();
    }

    @Test
    void testSimpleQueryCaseSensitiveRegex() {
        String query = "a=b";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the regular expression "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "),
                TooltipTextUtil.createText("The search is case sensitive."));
        TextFlow description = createDescription(query, true, true);

        TextFlowEqualityHelper.assertEquals(expectedTexts, description);
    }

    @Test
    void testSimpleQueryCaseSensitive() {
        String query = "a=b";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "),
                TooltipTextUtil.createText("The search is case sensitive."));
        TextFlow description = createDescription(query, true, false);

        TextFlowEqualityHelper.assertEquals(expectedTexts, description);
    }

    @Test
    void testSimpleQuery() {
        String query = "a=b";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "),
                TooltipTextUtil.createText("The search is case insensitive."));
        TextFlow description = createDescription(query, false, false);

        TextFlowEqualityHelper.assertEquals(expectedTexts, description);
    }

    @Test
    void testSimpleQueryRegex() {
        String query = "a=b";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the regular expression "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "),
                TooltipTextUtil.createText("The search is case insensitive."));
        TextFlow description = createDescription(query, false, true);

        TextFlowEqualityHelper.assertEquals(expectedTexts, description);
    }

    @Test
    void testComplexQueryCaseSensitiveRegex() {
        String query = "not a=b and c=e or e=\"x\"";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("not "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the regular expression "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" and "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("c", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the regular expression "),
                TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" or "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the regular expression "),
                TooltipTextUtil.createText("x", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "), TooltipTextUtil.createText("The search is case sensitive."));
        TextFlow description = createDescription(query, true, true);

        TextFlowEqualityHelper.assertEquals(expectedTexts, description);
    }

    @Test
    void testComplexQueryRegex() {
        String query = "not a=b and c=e or e=\"x\"";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("not "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the regular expression "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" and "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("c", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the regular expression "),
                TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" or "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the regular expression "),
                TooltipTextUtil.createText("x", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "), TooltipTextUtil.createText("The search is case insensitive."));
        TextFlow description = createDescription(query, false, true);

        TextFlowEqualityHelper.assertEquals(expectedTexts, description);
    }

    @Test
    void testComplexQueryCaseSensitive() {
        String query = "not a=b and c=e or e=\"x\"";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("not "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" and "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("c", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" or "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("x", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "), TooltipTextUtil.createText("The search is case sensitive."));
        TextFlow description = createDescription(query, true, false);

        TextFlowEqualityHelper.assertEquals(expectedTexts, description);
    }

    @Test
    void testComplexQuery() {
        String query = "not a=b and c=e or e=\"x\"";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which "), TooltipTextUtil.createText("not "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" and "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("c", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" or "), TooltipTextUtil.createText("the field "), TooltipTextUtil.createText("e", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" contains the term "), TooltipTextUtil.createText("x", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(". "), TooltipTextUtil.createText("The search is case insensitive."));
        TextFlow description = createDescription(query, false, false);

        TextFlowEqualityHelper.assertEquals(expectedTexts, description);
    }
}
