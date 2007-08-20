/**
 * SearchExpression.java
 *
 * @author Created by Omnicore CodeGuide
 */

package net.sf.jabref.search;

import java.io.StringReader;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.SearchRule;
import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;

public class SearchExpression implements SearchRule {
	private SearchExpressionTreeParser treeParser = new SearchExpressionTreeParser();
	private AST ast = null;
	private JabRefPreferences prefs = null;

	public SearchExpression(JabRefPreferences prefs, Hashtable<String, String> searchOptions)
		throws TokenStreamException, RecognitionException,
		PatternSyntaxException {
		this.prefs = prefs;
		// parse search expression
		SearchExpressionParser parser = new SearchExpressionParser(
			new SearchExpressionLexer(new StringReader(searchOptions.elements()
				.nextElement()))); // supports only single entry
		parser.caseSensitive = this.prefs.getBoolean("caseSensitiveSearch");
		parser.regex = this.prefs.getBoolean("regExpSearch");
		parser.searchExpression(); // this is the "global" rule
		ast = parser.getAST(); // remember abstract syntax tree
	}

	public int applyRule(Map<String, String> searchStrings, BibtexEntry bibtexEntry) {
		try {
			return treeParser.apply(ast, bibtexEntry);
		} catch (RecognitionException e) {
			return 0; // this should never occur
		}
	}
}
