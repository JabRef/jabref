package org.jabref.model.search.rules;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.pdf.search.retrieval.PdfSearcher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.pdf.search.PdfSearchResults;
import org.jabref.model.pdf.search.SearchResult;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchLexer;
import org.jabref.search.SearchParser;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The search query must be specified in an expression that is acceptable by the Search.g4 grammar.
 * <p>
 * This class implements the "Advanced Search Mode" described in the help
 */
public class GrammarBasedSearchRule implements SearchRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrammarBasedSearchRule.class);

    private final boolean caseSensitiveSearch;
    private final boolean regExpSearch;
    private final boolean fulltext;

    private ParseTree tree;
    private String query;
    private List<SearchResult> searchResults;
    private BibDatabaseContext databaseContext;

    public static class ThrowingErrorListener extends BaseErrorListener {

        public static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e)
                throws ParseCancellationException {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }

    public GrammarBasedSearchRule(boolean caseSensitiveSearch, boolean regExpSearch, boolean fulltext, BibDatabaseContext databaseContext) throws RecognitionException {
        this.caseSensitiveSearch = caseSensitiveSearch;
        this.regExpSearch = regExpSearch;
        this.fulltext = fulltext;
        this.databaseContext = databaseContext;
    }

    public static boolean isValid(boolean caseSensitive, boolean regExp, boolean fulltext, String query) {
        return new GrammarBasedSearchRule(caseSensitive, regExp, fulltext, null).validateSearchStrings(query);
    }

    public boolean isCaseSensitiveSearch() {
        return this.caseSensitiveSearch;
    }

    public boolean isRegExpSearch() {
        return this.regExpSearch;
    }

    public boolean isFUlltextSearch() {
        return this.fulltext;
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

        if (!fulltext) {
            return;
        }
        try {
            PdfSearcher searcher = PdfSearcher.of(databaseContext);
            PdfSearchResults results = searcher.search(query, 100);
            searchResults = results.getSortedByScore();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean applyRule(String query, BibEntry bibEntry) {
        try {
            return new BibtexSearchVisitor(caseSensitiveSearch, regExpSearch, fulltext, bibEntry, databaseContext).visit(tree);
        } catch (Exception e) {
            LOGGER.debug("Search failed", e);
            return false;
        }
    }

    @Override
    public PdfSearchResults getFulltextResults(String query, BibEntry bibEntry) {
        Vector<SearchResult> searchResults = new Vector<>();
        for (SearchResult searchResult : searchResults) {
            if (searchResult.isResultFor(bibEntry)) {
                searchResults.add(searchResult);
            }
        }
        return new PdfSearchResults(searchResults);
    }

    @Override
    public boolean validateSearchStrings(String query) {
        try {
            init(query);
            return true;
        } catch (ParseCancellationException e) {
            LOGGER.debug("Search query invalid", e);
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
            if (fieldPattern.matcher(InternalField.TYPE_HEADER.getName()).matches()) {
                return matchFieldValue(entry.getType().getName());
            }

            // special case for searching a single keyword
            if (fieldPattern.matcher("anykeyword").matches()) {
                return entry.getKeywords(',').stream().map(Keyword::toString).anyMatch(this::matchFieldValue);
            }

            // specification of fieldsKeys to search is done in the search expression itself
            Set<Field> fieldsKeys = entry.getFields();

            // special case for searching allfields=cat and title=dog
            if (!fieldPattern.matcher("anyfield").matches()) {
                // Filter out the requested fields
                fieldsKeys = fieldsKeys.stream().filter(matchFieldKey()).collect(Collectors.toSet());
            }

            for (Field field : fieldsKeys) {
                Optional<String> fieldValue = entry.getLatexFreeField(field);
                if (fieldValue.isPresent()) {
                    if (matchFieldValue(fieldValue.get())) {
                        return true;
                    }
                }
            }

            // special case of asdf!=whatever and entry does not contain asdf
            return fieldsKeys.isEmpty() && (operator == ComparisonOperator.DOES_NOT_CONTAIN);
        }

        private Predicate<Field> matchFieldKey() {
            return field -> fieldPattern.matcher(field.getName()).matches();
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
        private final boolean fulltext;

        private final BibEntry entry;

        private final BibDatabaseContext databaseContext;

        public BibtexSearchVisitor(boolean caseSensitive, boolean regex, boolean fulltext, BibEntry bibEntry, BibDatabaseContext databaseContext) {
            this.caseSensitive = caseSensitive;
            this.regex = regex;
            this.fulltext = fulltext;
            this.entry = bibEntry;
            this.databaseContext = databaseContext;
        }

        public boolean comparison(String field, ComparisonOperator operator, String value) {
            return new Comparator(field, value, operator, caseSensitive, regex).compare(entry);
        }

        @Override
        public Boolean visitStart(SearchParser.StartContext ctx) {
            return visit(ctx.expression());
        }

        @Override
        public Boolean visitComparison(SearchParser.ComparisonContext context) {
            // remove possible enclosing " symbols
            String right = context.right.getText();
            if (right.startsWith("\"") && right.endsWith("\"")) {
                right = right.substring(1, right.length() - 1);
            }

            Optional<SearchParser.NameContext> fieldDescriptor = Optional.ofNullable(context.left);
            if (fieldDescriptor.isPresent()) {
                return comparison(fieldDescriptor.get().getText(), ComparisonOperator.build(context.operator.getText()), right);
            } else {
                return SearchRules.getSearchRule(caseSensitive, regex, fulltext, databaseContext).applyRule(right, entry);
            }
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
