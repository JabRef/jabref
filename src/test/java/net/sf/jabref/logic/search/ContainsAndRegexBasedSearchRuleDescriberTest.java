package net.sf.jabref.logic.search;

import net.sf.jabref.logic.search.rules.describer.ContainsAndRegexBasedSearchRuleDescriber;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContainsAndRegexBasedSearchRuleDescriberTest {

    @Test
    public void testNoAst() {
        String query = "a b";
        evaluateNoAst(query, true, true, "This search contains entries in which any field contains the regular expression " +
                "<b>&#97;</b><b>&#98;</b> (case sensitive). " +
                "<p><br>Hint: To search specific fields only, " +
                "enter for example:<p><tt>author=smith and title=electrical</tt>");
        evaluateNoAst(query, true, false, "This search contains entries in which any field contains the term " +
                "<b>&#97;</b><b>&#98;</b> (case sensitive). " +
                "<p><br>Hint: To search specific fields only, enter for " +
                "example:<p><tt>author=smith and title=electrical</tt>");
        evaluateNoAst(query, false, false, "This search contains entries in which any field contains the term " +
                "<b>&#97;</b><b>&#98;</b> (case insensitive). " +
                "<p><br>Hint: To search specific fields only, enter for " +
                "example:<p><tt>author=smith and title=electrical</tt>");
        evaluateNoAst(query, false, true, "This search contains entries in which any field contains the regular " +
                "expression <b>&#97;</b><b>&#98;</b> (case insensitive). " +
                "<p><br>Hint: To search specific fields only, enter for " +
                "example:<p><tt>author=smith and title=electrical</tt>");
    }

    private void evaluateNoAst(String query, boolean caseSensitive, boolean regex, String expected) {
        assertEquals(expected, new ContainsAndRegexBasedSearchRuleDescriber(caseSensitive, regex, query).getDescription());
    }

}
