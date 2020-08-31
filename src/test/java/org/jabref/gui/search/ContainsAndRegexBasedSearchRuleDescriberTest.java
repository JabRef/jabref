package org.jabref.gui.search;

import java.util.Arrays;
import java.util.List;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import org.jabref.gui.search.rules.describer.ContainsAndRegexBasedSearchRuleDescriber;
import org.jabref.gui.util.TooltipTextUtil;
import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

@GUITest
@ExtendWith(ApplicationExtension.class)
class ContainsAndRegexBasedSearchRuleDescriberTest {

    @Start
    void onStart(Stage stage) {
        // Needed to init JavaFX thread
        stage.show();
    }

    @Test
    void testSimpleTerm() {
        String query = "test";
        List<Text> expectedTexts = Arrays.asList(
                TooltipTextUtil.createText("This search contains entries in which any field contains the term "),
                TooltipTextUtil.createText("test", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" (case insensitive). "),
                TooltipTextUtil.createText("\n\nHint: To search specific fields only, enter for example:"),
                TooltipTextUtil.createText(" author=smith and title=electrical", TooltipTextUtil.TextType.MONOSPACED));
        TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(false, false, query).getDescription();

        TextFlowEqualityHelper.assertEquals(expectedTexts, description);
    }

    @Test
    void testNoAst() {
        String query = "a b";
        List<Text> expectedTexts = Arrays.asList(
                TooltipTextUtil.createText("This search contains entries in which any field contains the term "),
                TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" and "),
                TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD),
                TooltipTextUtil.createText(" (case insensitive). "),
                TooltipTextUtil.createText("\n\nHint: To search specific fields only, enter for example:"),
                TooltipTextUtil.createText(" author=smith and title=electrical", TooltipTextUtil.TextType.MONOSPACED));
        TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(false, false, query).getDescription();

        TextFlowEqualityHelper.assertEquals(expectedTexts, description);
    }

    @Test
    void testNoAstRegex() {
        String query = "a b";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which any field contains the regular expression "),
                TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" and "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" (case insensitive). "),
                TooltipTextUtil.createText("\n\nHint: To search specific fields only, enter for example:"),
                TooltipTextUtil.createText(" author=smith and title=electrical", TooltipTextUtil.TextType.MONOSPACED));
        TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(false, true, query).getDescription();

        TextFlowEqualityHelper.assertEquals(expectedTexts, description);
    }

    @Test
    void testNoAstRegexCaseSensitive() {
        String query = "a b";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which any field contains the regular expression "),
                TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" and "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" (case sensitive). "),
                TooltipTextUtil.createText("\n\nHint: To search specific fields only, enter for example:"),
                TooltipTextUtil.createText(" author=smith and title=electrical", TooltipTextUtil.TextType.MONOSPACED));
        TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(true, true, query).getDescription();

        TextFlowEqualityHelper.assertEquals(expectedTexts, description);
    }

    @Test
    void testNoAstCaseSensitive() {
        String query = "a b";
        List<Text> expectedTexts = Arrays.asList(TooltipTextUtil.createText("This search contains entries in which any field contains the term "),
                TooltipTextUtil.createText("a", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" and "), TooltipTextUtil.createText("b", TooltipTextUtil.TextType.BOLD), TooltipTextUtil.createText(" (case sensitive). "),
                TooltipTextUtil.createText("\n\nHint: To search specific fields only, enter for example:"),
                TooltipTextUtil.createText(" author=smith and title=electrical", TooltipTextUtil.TextType.MONOSPACED));
        TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(true, false, query).getDescription();

        TextFlowEqualityHelper.assertEquals(expectedTexts, description);
    }
}
