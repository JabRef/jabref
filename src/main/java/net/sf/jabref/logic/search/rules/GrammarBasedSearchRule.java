/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.logic.search.rules;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.search.SearchBaseVisitor;
import net.sf.jabref.logic.search.SearchRule;
import net.sf.jabref.search.SearchLexer;
import net.sf.jabref.search.SearchParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The search query must be specified in an expression that is acceptable by the Search.g4 grammar.
 */
public class GrammarBasedSearchRule implements SearchRule {

    private static final Log LOGGER = LogFactory.getLog(GrammarBasedSearchRule.class);


    public static class ThrowingErrorListener extends BaseErrorListener {

        public static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                int line, int charPositionInLine, String msg, RecognitionException e)
                throws ParseCancellationException {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }

    private final boolean caseSensitiveSearch;
    private final boolean regExpSearch;

    private ParseTree tree;
    private String query;

    public GrammarBasedSearchRule(boolean caseSensitiveSearch, boolean regExpSearch) throws RecognitionException {
        this.caseSensitiveSearch = caseSensitiveSearch;
        this.regExpSearch = regExpSearch;
    }

    public static boolean isValid(boolean caseSensitive, boolean regExp, String query) {
        return new GrammarBasedSearchRule(caseSensitive, regExp).validateSearchStrings(query);
    }

    public boolean isCaseSensitiveSearch() {
        return this.caseSensitiveSearch;
    }

    public boolean isRegExpSearch() {
        return this.regExpSearch;
    }

    public ParseTree getTree() {
        return this.tree;
    }

    public String getQuery() {
        return this.query;
    }

    private void init(String query) throws ParseCancellationException {
        if(Objects.equals(this.query, query)) {
            return;
        }

        SearchLexer lexer = new SearchLexer(new ANTLRInputStream(query));
        lexer.removeErrorListeners(); // no infos on file system
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
        SearchParser parser = new SearchParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners(); // no infos on file system
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        parser.setErrorHandler(new BailErrorStrategy()); // ParseCancellationException on parse errors
        tree = parser.start();
        this.query = query;
    }

    @Override
    public boolean applyRule(String query, BibEntry bibEntry) {
        try {
            return new BibtexSearchVisitor(caseSensitiveSearch, regExpSearch, bibEntry).visit(tree);
        } catch (Exception e) {
            LOGGER.debug("Search failed", e);
            return false;
        }
    }

    @Override
    public boolean validateSearchStrings(String query) {
        try {
            init(query);
            return true;
        } catch (ParseCancellationException e) {
            return false;
        }
    }

    public enum ComparisonOperator {
        EXACT, CONTAINS, DOES_NOT_CONTAIN;

        public static ComparisonOperator build(String value) {
            if ("CONTAINS".equalsIgnoreCase(value) || "=".equals(value)) {
                return CONTAINS;
            } else if ("MATCHES".equalsIgnoreCase(value) || "==".equals(value)) {
                return EXACT;
            } else {
                return DOES_NOT_CONTAIN;
            }
        }
    }

    public static class Comparator {

        private final ComparisonOperator operator;
        private final Pattern fieldPattern;
        private final Pattern valuePattern;

        public Comparator(String field, String value, ComparisonOperator operator, boolean caseSensitive, boolean regex) {
            this.operator = operator;

            this.fieldPattern = Pattern.compile(regex ? field : "\\Q" + field + "\\E", caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            this.valuePattern = Pattern.compile(regex ? value : "\\Q" + value + "\\E", caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        }

        public boolean compare(BibEntry entry) {
            // specification of fields to search is done in the search expression itself
            String[] searchKeys = entry.getFieldNames().toArray(new String[entry.getFieldNames().size()]);

            boolean noSuchField = true;
            // this loop iterates over all regular keys, then over pseudo keys like "type"
            for (int i = 0; i < (searchKeys.length + 1); i++) {
                String content;
                if ((i - searchKeys.length) == 0) {
                    // PSEUDOFIELD_TYPE
                    if (!fieldPattern.matcher("entrytype").matches()) {
                        continue;
                    }
                    content = entry.getType().getName();
                } else {
                    String searchKey = searchKeys[i];
                    if (!fieldPattern.matcher(searchKey).matches()) {
                        continue;
                    }
                    content = entry.getField(searchKey);
                }
                noSuchField = false;
                if (content == null) {
                    continue; // paranoia
                }

                if(matchInField(content)) {
                    return true;
                }
            }

            return noSuchField && (operator == ComparisonOperator.DOES_NOT_CONTAIN);
        }

        public boolean matchInField(String content) {
            Matcher matcher = valuePattern.matcher(content);
            if (operator == ComparisonOperator.CONTAINS) {
                return matcher.find();
            } else if (operator == ComparisonOperator.EXACT) {
                return matcher.matches();
            } else if (operator == ComparisonOperator.DOES_NOT_CONTAIN) {
                return !matcher.find();
            } else {
                throw new IllegalStateException("MUST NOT HAPPEN");
            }
        }

    }


    /**
     * Search results in boolean. It may be later on converted to an int.
     */
    static class BibtexSearchVisitor extends SearchBaseVisitor<Boolean> {

        private final boolean caseSensitive;
        private final boolean regex;

        private final BibEntry entry;

        public BibtexSearchVisitor(boolean caseSensitive, boolean regex, BibEntry bibEntry) {
            this.caseSensitive = caseSensitive;
            this.regex = regex;
            this.entry = bibEntry;
        }

        public boolean comparison(String field, ComparisonOperator operator, String value) {
            return new Comparator(field, value, operator, caseSensitive, regex).compare(entry);
        }

        @Override public Boolean visitStart(SearchParser.StartContext ctx) {
            return visit(ctx.expression());
        }

        @Override
        public Boolean visitComparison(SearchParser.ComparisonContext ctx) {
            return comparison(ctx.left.getText(), ComparisonOperator.build(ctx.operator.getText()), ctx.right.getText());
        }

        @Override
        public Boolean visitUnaryExpression(SearchParser.UnaryExpressionContext ctx) {
            return !visit(ctx.expression()); // negate
        }

        @Override
        public Boolean visitParenExpression(SearchParser.ParenExpressionContext ctx) {
            return visit(ctx.expression()); // ignore parenthesis
        }

        @Override
        public Boolean visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
            if ("AND".equalsIgnoreCase(ctx.operator.getText())) {
                return visit(ctx.left) && visit(ctx.right); // and
            } else {
                return visit(ctx.left) || visit(ctx.right); // or
            }
        }

    }

}
