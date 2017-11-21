package org.jabref.gui.search;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.search.rules.describer.GrammarBasedSearchRuleDescriber;
import org.jabref.gui.util.TextUtil;
import org.jabref.model.search.rules.GrammarBasedSearchRule;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GrammarBasedSearchRuleDescriberTest {

    @Test
    public void testSimpleQuery() {
        double textSize = 13;
        String query = "a=b";
        evaluate(query, true, true, TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("a", textSize),
                TextUtil.createText(" contains the regular expression ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(". ", textSize),
                TextUtil.createText("The search is case sensitive.", textSize));
        evaluate(query, true, false, TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("a", textSize),
                TextUtil.createText(" contains the term ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(". ", textSize),
                TextUtil.createText("The search is case sensitive.", textSize));
        evaluate(query, false, false, TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("a", textSize),
                TextUtil.createText(" contains the term ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(". ", textSize),
                TextUtil.createText("The search is case insensitive.", textSize));
        evaluate(query, false, true, TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("a", textSize),
                TextUtil.createText(" contains the regular expression ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(". ", textSize),
                TextUtil.createText("The search is case insensitive.", textSize));
    }

    @Test
    public void testComplexQuery() {
        double textSize = 13;
        String query = "not a=b and c=e or e=\"x\"";
        evaluate(query, true, true, TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("not ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("a", textSize),
                TextUtil.createText(" contains the regular expression ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(" and ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("c", textSize), TextUtil.createText(" contains the regular expression ", textSize),
                TextUtil.createTextBold("e", textSize), TextUtil.createText(" or ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("e", textSize), TextUtil.createText(" contains the regular expression ", textSize),
                TextUtil.createTextBold("x", textSize), TextUtil.createText(". ", textSize), TextUtil.createText("The search is case sensitive.", textSize));
        evaluate(query, false, true, TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("not ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("a", textSize),
                TextUtil.createText(" contains the regular expression ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(" and ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("c", textSize), TextUtil.createText(" contains the regular expression ", textSize),
                TextUtil.createTextBold("e", textSize), TextUtil.createText(" or ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("e", textSize), TextUtil.createText(" contains the regular expression ", textSize),
                TextUtil.createTextBold("x", textSize), TextUtil.createText(". ", textSize), TextUtil.createText("The search is case insensitive.", textSize));
        evaluate(query, true, false, TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("not ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("a", textSize),
                TextUtil.createText(" contains the term ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(" and ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("c", textSize), TextUtil.createText(" contains the term ", textSize), TextUtil.createTextBold("e", textSize),
                TextUtil.createText(" or ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("e", textSize), TextUtil.createText(" contains the term ", textSize), TextUtil.createTextBold("x", textSize), TextUtil.createText(". ", textSize), TextUtil.createText("The search is case sensitive.", textSize));
        evaluate(query, false, false, TextUtil.createText("This search contains entries in which ", textSize), TextUtil.createText("not ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("a", textSize),
                TextUtil.createText(" contains the term ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(" and ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("c", textSize), TextUtil.createText(" contains the term ", textSize), TextUtil.createTextBold("e", textSize),
                TextUtil.createText(" or ", textSize), TextUtil.createText("the field ", textSize), TextUtil.createTextBold("e", textSize), TextUtil.createText(" contains the term ", textSize), TextUtil.createTextBold("x", textSize), TextUtil.createText(". ", textSize), TextUtil.createText("The search is case insensitive.", textSize));
    }


    private void evaluate(String query, boolean caseSensitive, boolean regex, Text... expected) {
        GrammarBasedSearchRule grammarBasedSearchRule = new GrammarBasedSearchRule(caseSensitive, regex);
        assertTrue(grammarBasedSearchRule.validateSearchStrings(query));
        GrammarBasedSearchRuleDescriber describer = new GrammarBasedSearchRuleDescriber(caseSensitive, regex, grammarBasedSearchRule.getTree());
        TextFlow description = describer.getDescription();
        assertEquals("Wrong number of Texts inside the description TextFlow", expected.length, description.getChildren().size());
        Text expectedText;
        for (int i = 0; i < expected.length; i++) {
            expectedText = expected[i];
            Assert.assertEquals(expectedText.toString(), description.getChildren().get(i).toString());
        }
    }
}
