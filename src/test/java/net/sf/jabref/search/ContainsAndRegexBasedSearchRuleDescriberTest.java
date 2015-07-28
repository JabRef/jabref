package net.sf.jabref.search;

import net.sf.jabref.search.describer.ContainsAndRegexBasedSearchRuleDescriber;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContainsAndRegexBasedSearchRuleDescriberTest {

    @Test
    public void testNoAst() throws Exception {
        String query = "a b";
        evaluateNoAst(query, true, true, "This group contains entries in which any field contains the regular expression " +
                "<b>&#97;</b><b>&#98;</b> (case sensitive). Entries cannot be manually assigned to or removed " +
                "from this group.<p><br>Hint: To search specific fields only, " +
                "enter for example:<p><tt>author=smith and title=electrical</tt>");
        evaluateNoAst(query, true, false, "This group contains entries in which any field contains the term " +
                "<b>&#97;</b><b>&#98;</b> (case sensitive). Entries cannot be manually assigned to or removed from " +
                "this group.<p><br>Hint: To search specific fields only, enter for " +
                "example:<p><tt>author=smith and title=electrical</tt>");
        evaluateNoAst(query, false, false, "This group contains entries in which any field contains the term " +
                "<b>&#97;</b><b>&#98;</b> (case insensitive). Entries cannot be manually assigned to or removed " +
                "from this group.<p><br>Hint: To search specific fields only, enter for " +
                "example:<p><tt>author=smith and title=electrical</tt>");
        evaluateNoAst(query, false, true, "This group contains entries in which any field contains the regular " +
                "expression <b>&#97;</b><b>&#98;</b> (case insensitive). Entries cannot be manually assigned " +
                "to or removed from this group.<p><br>Hint: To search specific fields only, enter for " +
                "example:<p><tt>author=smith and title=electrical</tt>");
    }

    private void evaluateNoAst(String query, boolean caseSensitive, boolean regex, String expected) {
        assertEquals(expected, new ContainsAndRegexBasedSearchRuleDescriber(caseSensitive, regex, query).getDescription());
    }

}
