package org.jabref.gui.search;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.search.rules.describer.ContainsAndRegexBasedSearchRuleDescriber;
import org.jabref.gui.util.TextUtil;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContainsAndRegexBasedSearchRuleDescriberTest {

    @Test
    public void testNoAst() {
        double textSize = 13;
        String query = "a b";
        evaluateNoAst(query, true, true, TextUtil.createText("This search contains entries in which any field contains the regular expression ", textSize),
                TextUtil.createTextBold("a", textSize), TextUtil.createText(" and ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(" (case sensitive). ", textSize),
                TextUtil.createText("\n\nHint: To search specific fields only, enter for example:\n", textSize), TextUtil.createTextMonospaced("author=smith and title=electrical", textSize));
        evaluateNoAst(query, true, false, TextUtil.createText("This search contains entries in which any field contains the term ", textSize),
                TextUtil.createTextBold("a", textSize), TextUtil.createText(" and ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(" (case sensitive). ", textSize),
                TextUtil.createText("\n\nHint: To search specific fields only, enter for example:\n", textSize), TextUtil.createTextMonospaced("author=smith and title=electrical", textSize));
        evaluateNoAst(query, false, false, TextUtil.createText("This search contains entries in which any field contains the term ", textSize),
                TextUtil.createTextBold("a", textSize), TextUtil.createText(" and ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(" (case insensitive). ", textSize),
                TextUtil.createText("\n\nHint: To search specific fields only, enter for example:\n", textSize), TextUtil.createTextMonospaced("author=smith and title=electrical", textSize));
        evaluateNoAst(query, false, true, TextUtil.createText("This search contains entries in which any field contains the regular expression ", textSize),
                TextUtil.createTextBold("a", textSize), TextUtil.createText(" and ", textSize), TextUtil.createTextBold("b", textSize), TextUtil.createText(" (case insensitive). ", textSize),
                TextUtil.createText("\n\nHint: To search specific fields only, enter for example:\n", textSize), TextUtil.createTextMonospaced("author=smith and title=electrical", textSize));
    }

    private void evaluateNoAst(String query, boolean caseSensitive, boolean regex, Text... expected) {
        TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(caseSensitive, regex, query).getDescription();
        assertEquals("Wrong number of Texts inside the description TextFlow", expected.length, description.getChildren().size());
        Text expectedText;
        for (int i = 0; i < expected.length; i++) {
            expectedText = expected[i];
            Assert.assertEquals(expectedText.toString(), description.getChildren().get(i).toString());
        }
    }

}
