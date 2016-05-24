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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.search.SearchBaseVisitor;
import net.sf.jabref.search.SearchLexer;
import net.sf.jabref.search.SearchParser;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The search query must be specified in an expression that is acceptable by the Search.g4 grammar.
 */
public class GrammarBasedSearchRule implements SearchRule {

    private static final Log LOGGER = LogFactory.getLog(GrammarBasedSearchRule.class);

    private final boolean caseSensitiveSearch;
    private final boolean regExpSearch;

    private ParseTree tree;
    private String query;


    public static class ThrowingErrorListener extends BaseErrorListener {

        public static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                int line, int charPositionInLine, String msg, RecognitionException e)
                throws ParseCancellationException {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }

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
        if (Objects.equals(this.query, query)) {
            return;
        }

        SearchLexer lexer = new SearchLexer(new ANTLRInputStream(query));
        lexer.removeErrorListeners(); // no infos on file system
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
        SearchParser parser = new SearchParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners(); // no infos on file system
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        parser.setErrorHandler(new BailErrorStrategy()); // ParseCancelationException on parse errors
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

            int option = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            this.fieldPattern = Pattern.compile(regex ? field : "\\Q" + field + "\\E", option);
            this.valuePattern = Pattern.compile(regex ? value : "\\Q" + value + "\\E", option);
        }

        public boolean compare(BibEntry entry) {
            // special case for searching for entrytype=phdthesis
            if (fieldPattern.matcher("entrytype").matches()) {
                return matchFieldValue(entry.getType());
            }

            // specification of fieldsKeys to search is done in the search expression itself
            Set<String> fieldsKeys = entry.getFieldNames();

            List<String> matchedFieldKeys = fieldsKeys.stream().filter(matchFieldKey()).collect(Collectors.toList());

            for (String field : matchedFieldKeys) {
                String fieldValue = entry.getField(field);
                if (fieldValue == null) {
                    continue; // paranoia
                }

                if (matchFieldValue(fieldValue)) {
                    return true;
                }
            }

            // special case of asdf!=whatever and entry does not contain asdf
            return matchedFieldKeys.isEmpty() && (operator == ComparisonOperator.DOES_NOT_CONTAIN);
        }

        private Predicate<String> matchFieldKey() {
            return s -> fieldPattern.matcher(s).matches();
        }

        public boolean matchFieldValue(String content) {
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

        @Override
        public Boolean visitStart(SearchParser.StartContext ctx) {
            return visit(ctx.expression());
        }

        @Override
        public Boolean visitComparison(SearchParser.ComparisonContext ctx) {
            // remove possible enclosing " symbols
            String right = ctx.right.getText();
            if(right.startsWith("\"") && right.endsWith("\"")) {
                right = right.substring(1, right.length() - 2);
            }

            return comparison(ctx.left.getText(), ComparisonOperator.build(ctx.operator.getText()), right);

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
