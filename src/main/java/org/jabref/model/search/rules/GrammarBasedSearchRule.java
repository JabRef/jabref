package org.jabref.model.search.rules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.gui.Globals;
import org.jabref.logic.pdf.search.retrieval.PdfSearcher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.pdf.search.PdfSearchResults;
import org.jabref.model.pdf.search.SearchResult;
import org.jabref.model.search.rules.SearchRules.SearchFlags;
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
@AllowedToUseLogic("Because access to the lucene index is needed")
public class GrammarBasedSearchRule implements SearchRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrammarBasedSearchRule.class);

    private final EnumSet<SearchFlags> searchFlags;

    private ParseTree tree;
    private String query;
    private List<SearchResult> searchResults = new ArrayList<>();

    private final BibDatabaseContext databaseContext;

    public static class ThrowingErrorListener extends BaseErrorListener {

        public static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e)
                throws ParseCancellationException {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }

    public GrammarBasedSearchRule(EnumSet<SearchFlags> searchFlags) throws RecognitionException {
        this.searchFlags = searchFlags;
        databaseContext = Globals.stateManager.getActiveDatabase().orElse(null);
    }

    public static boolean isValid(EnumSet<SearchFlags> searchFlags, String query) {
        return new GrammarBasedSearchRule(searchFlags).validateSearchStrings(query);
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

        if (!searchFlags.contains(SearchRules.SearchFlags.FULLTEXT) || (databaseContext == null)) {
            return;
        }
        try {
            PdfSearcher searcher = PdfSearcher.of(databaseContext);
            PdfSearchResults results = searcher.search(query, 5);
            searchResults = results.getSortedByScore();
        } catch (IOException e) {
            LOGGER.error("Could not retrieve search results!", e);
        }
    }

    @Override
    public boolean applyRule(String query, BibEntry bibEntry) {
        try {
            return new BibtexSearchVisitor(searchFlags, bibEntry).visit(tree);
        } catch (Exception e) {
            LOGGER.debug("Search failed", e);
            return getFulltextResults(query, bibEntry).numSearchResults() > 0;
        }
    }

    @Override
    public PdfSearchResults getFulltextResults(String query, BibEntry bibEntry) {
        return new PdfSearchResults(searchResults.stream().filter(searchResult -> searchResult.isResultFor(bibEntry)).collect(Collectors.toList()));
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

    public EnumSet<SearchFlags> getSearchFlags() {
        return searchFlags;
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

        public Comparator(String field, String value, ComparisonOperator operator, EnumSet<SearchFlags> searchFlags) {
            this.operator = operator;

            int option = searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE) ? 0 : Pattern.CASE_INSENSITIVE;
            this.fieldPattern = Pattern.compile(searchFlags.contains(SearchRules.SearchFlags.REGULAR_EXPRESSION) ? field : "\\Q" + field + "\\E", option);
            this.valuePattern = Pattern.compile(searchFlags.contains(SearchRules.SearchFlags.REGULAR_EXPRESSION) ? value : "\\Q" + value + "\\E", option);
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

        private final EnumSet<SearchFlags> searchFlags;

        private final BibEntry entry;

        public BibtexSearchVisitor(EnumSet<SearchFlags> searchFlags, BibEntry bibEntry) {
            this.searchFlags = searchFlags;
            this.entry = bibEntry;
        }

        public boolean comparison(String field, ComparisonOperator operator, String value) {
            return new Comparator(field, value, operator, searchFlags).compare(entry);
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
                return SearchRules.getSearchRule(searchFlags).applyRule(right, entry);
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
