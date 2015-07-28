package net.sf.jabref.search;

import net.sf.jabref.search.describer.SearchExpressionDescriber;
import net.sf.jabref.search.rules.SearchExpression;
import org.junit.Test;

import static org.junit.Assert.*;

public class SearchExpressionDescriberTest {

    @Test
    public void testSimpleQuery() throws Exception {
        String query = "a=b";
        evaluate(query, true, true, "This group contains entries in which the field <b>&#97;</b> " +
                "contains the Regular Expression <b>&#98;</b>. " +
                "The search is case sensitive.");
        evaluate(query, true, false, "This group contains entries in which the field <b>&#97;</b> " +
                "contains the term <b>&#98;</b>. " +
                "The search is case sensitive.");
        evaluate(query, false, false, "This group contains entries in which the field <b>&#97;</b> " +
                "contains the term <b>&#98;</b>. " +
                "The search is case insensitive.");
        evaluate(query, false, true, "This group contains entries in which the field <b>&#97;</b> " +
                "contains the Regular Expression <b>&#98;</b>. " +
                "The search is case insensitive.");
    }

    @Test
    public void testComplexQuery() throws Exception {
        String query = "not a=b and c=e or e=\"x\"";
        evaluate(query, true, true, "This group contains entries in which not ((the field <b>&#97;</b> "
                + "contains the Regular Expression <b>&#98;</b> and the field <b>&#99;</b> contains the "
                + "Regular Expression <b>&#101;</b>) or the field <b>&#101;</b> contains the Regular Expression "
                + "<b>&#120;</b>). The search is case sensitive.");
        evaluate(query, true, false, "This group contains entries in which not ((the field <b>&#97;</b> "
                + "contains the term <b>&#98;</b> and the field <b>&#99;</b> contains the term <b>&#101;</b>) "
                + "or the field <b>&#101;</b> contains the term <b>&#120;</b>). The search is case sensitive.");
        evaluate(query, false, false, "This group contains entries in which not ((the field <b>&#97;</b> "
                + "contains the term <b>&#98;</b> and the field <b>&#99;</b> contains the term <b>&#101;</b>) "
                + "or the field <b>&#101;</b> contains the term <b>&#120;</b>). The search is case insensitive.");
        evaluate(query, false, true, "This group contains entries in which not ((the field <b>&#97;</b> "
                + "contains the Regular Expression <b>&#98;</b> and the field <b>&#99;</b> contains "
                + "the Regular Expression <b>&#101;</b>) or the field <b>&#101;</b> contains the Regular "
                + "Expression <b>&#120;</b>). The search is case insensitive.");
    }



    private void evaluate(String query, boolean caseSensitive, boolean regex, String expected) {
        SearchExpression searchExpression = new SearchExpression(caseSensitive, regex);
        assertTrue(searchExpression.validateSearchStrings(query));
        SearchExpressionDescriber describer = new SearchExpressionDescriber(caseSensitive, regex, searchExpression.getTree());
        assertEquals(expected, describer.getDescription());
    }
}