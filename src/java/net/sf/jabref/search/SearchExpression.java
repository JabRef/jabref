/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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

    public boolean validateSearchStrings(Map<String, String> searchStrings) {
        return true;
    }
}
