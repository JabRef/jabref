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
package net.sf.jabref.search.rules;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.PatternSyntaxException;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.search.SearchExpressionLexer;
import net.sf.jabref.search.SearchExpressionParser;
import net.sf.jabref.search.SearchExpressionTreeParser;
import net.sf.jabref.search.SearchRule;

public class SearchExpression implements SearchRule {

    private final SearchExpressionTreeParser treeParser = new SearchExpressionTreeParser();

    public AST getAst() {
        return ast;
    }

    private AST ast = null;

    public SearchExpression(boolean caseSensitive, boolean regex, String query)
            throws PatternSyntaxException, IOException, TokenStreamException, antlr.RecognitionException {

        // parse search expression
        SearchExpressionParser parser = new SearchExpressionParser(new SearchExpressionLexer(new StringReader(query)));
        parser.caseSensitive = caseSensitive;
        parser.regex = regex;
        parser.searchExpression(); // this is the "global" rule
        ast = parser.getAST(); // remember abstract syntax tree
    }

    @Override
    public int applyRule(String query, BibtexEntry bibtexEntry) {
        try {
            return treeParser.apply(ast, bibtexEntry);
        } catch (RecognitionException e) {
            return 0; // this should never occur
        }
    }

    @Override
    public boolean validateSearchStrings(String query) {
        return true;
    }
}
