package net.sf.jabref.search;

import antlr.TokenStreamException;
import antlr.collections.AST;
import net.sf.jabref.search.rules.SearchExpression;
import org.junit.Test;

import java.io.IOException;

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
        evaluate(query, true, true, "This group contains entries in which the field <b>&#97;</b> " +
                "doesn't contain the Regular Expression <b>&#98;</b> and (the field <b>&#99;</b> " +
                "contains the Regular Expression <b>&#101;</b> or the field <b>&#101;</b> " +
                "contains the Regular Expression <b>&#120;</b>). The search is case sensitive.");
        evaluate(query, true, false, "This group contains entries in which the field <b>&#97;</b> " +
                "doesn't contain the term <b>&#98;</b> and (the field <b>&#99;</b> contains " +
                "the term <b>&#101;</b> or the field <b>&#101;</b> contains the term <b>&#120;</b>). " +
                "The search is case sensitive.");
        evaluate(query, false, false, "This group contains entries in which the field <b>&#97;</b> " +
                "doesn't contain the term <b>&#98;</b> and (the field <b>&#99;</b> contains " +
                "the term <b>&#101;</b> or the field <b>&#101;</b> contains the term <b>&#120;</b>). " +
                "The search is case insensitive.");
        evaluate(query, false, true, "This group contains entries in which the field <b>&#97;</b> " +
                "doesn't contain the Regular Expression <b>&#98;</b> and (the field <b>&#99;</b> " +
                "contains the Regular Expression <b>&#101;</b> or the field <b>&#101;</b> contains " +
                "the Regular Expression <b>&#120;</b>). The search is case insensitive.");
    }

    @Test
    public void testNoAst() throws Exception {
        String query = "a=b";
        evaluateNoAst(query, true, true, "This group contains entries in which any field contains the regular expression " +
                "<b>&#97;&#61;&#98;</b> (case sensitive). Entries cannot be manually assigned to or removed " +
                "from this group.<p><br>Hint: To search specific fields only, " +
                "enter for example:<p><tt>author=smith and title=electrical</tt>");
        evaluateNoAst(query, true, false, "This group contains entries in which any field contains the term " +
                "<b>&#97;&#61;&#98;</b> (case sensitive). Entries cannot be manually assigned to or removed from " +
                "this group.<p><br>Hint: To search specific fields only, enter for " +
                "example:<p><tt>author=smith and title=electrical</tt>");
        evaluateNoAst(query, false, false, "This group contains entries in which any field contains the term " +
                "<b>&#97;&#61;&#98;</b> (case insensitive). Entries cannot be manually assigned to or removed " +
                "from this group.<p><br>Hint: To search specific fields only, enter for " +
                "example:<p><tt>author=smith and title=electrical</tt>");
        evaluateNoAst(query, false, true, "This group contains entries in which any field contains the regular " +
                "expression <b>&#97;&#61;&#98;</b> (case insensitive). Entries cannot be manually assigned " +
                "to or removed from this group.<p><br>Hint: To search specific fields only, enter for " +
                "example:<p><tt>author=smith and title=electrical</tt>");
    }

    private void evaluateNoAst(String query, boolean caseSensitive, boolean regex, String expected) throws IOException, TokenStreamException, antlr.RecognitionException {
        SearchExpressionDescriber describer = new SearchExpressionDescriber(caseSensitive, regex, query, null);
        assertEquals(expected, describer.getDescriptionForPreview());
    }

    private void evaluate(String query, boolean caseSensitive, boolean regex, String expected) throws IOException, TokenStreamException, antlr.RecognitionException {
        SearchExpression searchExpression = new SearchExpression(caseSensitive, regex);
        searchExpression.validateSearchStrings(query);
        AST ast = searchExpression.getAst();
        SearchExpressionDescriber describer = new SearchExpressionDescriber(caseSensitive, regex, query, ast);
        assertEquals(expected, describer.getDescriptionForPreview());
    }
}