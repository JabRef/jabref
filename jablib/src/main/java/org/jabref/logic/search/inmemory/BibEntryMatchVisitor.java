package org.jabref.logic.search.inmemory;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jabref.logic.search.query.SearchQueryConversion;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.SearchFlags;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Walks a Search.g4 parse tree against a single [BibEntry] and returns whether the entry matches.
///
/// Operator semantics mirror [org.jabref.logic.search.query.SearchToSqlVisitor].
/// Pseudo-fields supported: `any` / `anyfield`, `key`, `entrytype`, `anykeyword`.
class BibEntryMatchVisitor extends SearchBaseVisitor<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibEntryMatchVisitor.class);
    private static final String GROUPS_FIELD_NAME = StandardField.GROUPS.getName();
    private static final char KEYWORD_SEPARATOR = ',';

    private enum MatchType { INEXACT, EXACT, REGEX }

    private final BibEntry entry;
    private final EnumSet<SearchFlags> searchBarFlags;

    BibEntryMatchVisitor(BibEntry entry, EnumSet<SearchFlags> searchBarFlags) {
        this.entry = entry;
        this.searchBarFlags = searchBarFlags;
    }

    @Override
    public Boolean visitStart(SearchParser.StartContext ctx) {
        // start : EOF | andExpression EOF — empty query matches everything
        if (ctx.andExpression() == null) {
            return true;
        }
        return visit(ctx.andExpression());
    }

    @Override
    public Boolean visitImplicitAndExpression(SearchParser.ImplicitAndExpressionContext ctx) {
        return ctx.expression().stream().allMatch(expr -> Boolean.TRUE.equals(visit(expr)));
    }

    @Override
    public Boolean visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        return visit(ctx.andExpression());
    }

    @Override
    public Boolean visitNegatedExpression(SearchParser.NegatedExpressionContext ctx) {
        return !Boolean.TRUE.equals(visit(ctx.expression()));
    }

    @Override
    public Boolean visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
        boolean left = Boolean.TRUE.equals(visit(ctx.left));
        if (ctx.bin_op.getType() == SearchParser.AND) {
            return left && Boolean.TRUE.equals(visit(ctx.right));
        }
        return left || Boolean.TRUE.equals(visit(ctx.right));
    }

    @Override
    public Boolean visitComparisonExpression(SearchParser.ComparisonExpressionContext ctx) {
        return visit(ctx.comparison());
    }

    @Override
    public Boolean visitComparison(SearchParser.ComparisonContext ctx) {
        String term = SearchQueryConversion.unescapeSearchValue(ctx.searchValue());

        if (ctx.FIELD() == null) {
            // Unfielded bareword: apply search-bar flags
            boolean caseSensitive = searchBarFlags.contains(SearchFlags.CASE_SENSITIVE);
            MatchType matchType = searchBarFlags.contains(SearchFlags.REGULAR_EXPRESSION)
                    ? MatchType.REGEX
                    : MatchType.INEXACT;
            return matchAnyField(term, matchType, caseSensitive);
        }

        String fieldName = ctx.FIELD().getText().toLowerCase(Locale.ROOT);
        int operator = ctx.operator().getStart().getType();
        OperatorFlags flags = mapOperator(operator);

        // field = "" / field != "" — presence/absence
        if (term.isEmpty()) {
            boolean present = isFieldPresent(fieldName);
            return flags.negation ? present : !present;
        }

        boolean matched = matchField(fieldName, term, flags.matchType, flags.caseSensitive);
        return flags.negation != matched;
    }

    private boolean isFieldPresent(String fieldName) {
        return switch (fieldName) {
            case "key", "citationkey" -> entry.getCitationKey().isPresent();
            case "entrytype" -> true; // every entry has a type
            case "any", "anyfield" -> entry.getFields().stream()
                                           .anyMatch(f -> !GROUPS_FIELD_NAME.equals(f.getName()));
            case "anykeyword" -> entry.getField(StandardField.KEYWORDS).isPresent();
            default -> entry.getFields().stream()
                            .anyMatch(f -> f.getName().equalsIgnoreCase(fieldName));
        };
    }

    private boolean matchField(String fieldName, String term, MatchType matchType, boolean caseSensitive) {
        return switch (fieldName) {
            case "key", "citationkey" -> entry.getCitationKey()
                    .map(v -> matchValue(v, term, matchType, caseSensitive))
                    .orElse(false);
            case "entrytype" -> matchValue(entry.getType().getName(), term, matchType, caseSensitive);
            case "any", "anyfield" -> matchAnyField(term, matchType, caseSensitive);
            case "anykeyword" -> matchAnyKeyword(term, matchType, caseSensitive);
            default -> matchNamedField(fieldName, term, matchType, caseSensitive);
        };
    }

    private boolean matchNamedField(String fieldName, String term, MatchType matchType, boolean caseSensitive) {
        for (Field field : entry.getFields()) {
            if (!field.getName().equalsIgnoreCase(fieldName)) {
                continue;
            }
            Optional<String> value = entry.getField(field);
            if (value.isPresent() && matchValue(value.get(), term, matchType, caseSensitive)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchAnyField(String term, MatchType matchType, boolean caseSensitive) {
        for (Field field : entry.getFields()) {
            if (GROUPS_FIELD_NAME.equals(field.getName())) {
                continue;
            }
            Optional<String> value = entry.getField(field);
            if (value.isPresent() && matchValue(value.get(), term, matchType, caseSensitive)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchAnyKeyword(String term, MatchType matchType, boolean caseSensitive) {
        List<String> keywords = entry.getKeywords(KEYWORD_SEPARATOR).stream().map(Object::toString).toList();
        return keywords.stream().anyMatch(k -> matchValue(k, term, matchType, caseSensitive));
    }

    private static boolean matchValue(String value, String term, MatchType matchType, boolean caseSensitive) {
        return switch (matchType) {
            case INEXACT -> caseSensitive
                    ? value.contains(term)
                    : value.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT));
            case EXACT -> caseSensitive ? value.equals(term) : value.equalsIgnoreCase(term);
            case REGEX -> matchRegex(value, term, caseSensitive);
        };
    }

    private static boolean matchRegex(String value, String pattern, boolean caseSensitive) {
        try {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            return Pattern.compile(pattern, flags).matcher(value).find();
        } catch (PatternSyntaxException e) {
            LOGGER.debug("Invalid regex pattern '{}': {}", pattern, e.getMessage());
            return false;
        }
    }

    private record OperatorFlags(MatchType matchType, boolean caseSensitive, boolean negation) { }

    private static OperatorFlags mapOperator(int tokenType) {
        return switch (tokenType) {
            case SearchParser.EQUAL, SearchParser.CONTAINS -> new OperatorFlags(MatchType.INEXACT, false, false);
            case SearchParser.CEQUAL -> new OperatorFlags(MatchType.INEXACT, true, false);
            case SearchParser.EEQUAL, SearchParser.MATCHES -> new OperatorFlags(MatchType.EXACT, false, false);
            case SearchParser.CEEQUAL -> new OperatorFlags(MatchType.EXACT, true, false);
            case SearchParser.REQUAL -> new OperatorFlags(MatchType.REGEX, false, false);
            case SearchParser.CREEQUAL -> new OperatorFlags(MatchType.REGEX, true, false);
            case SearchParser.NEQUAL -> new OperatorFlags(MatchType.INEXACT, false, true);
            case SearchParser.NCEQUAL -> new OperatorFlags(MatchType.INEXACT, true, true);
            case SearchParser.NEEQUAL -> new OperatorFlags(MatchType.EXACT, false, true);
            case SearchParser.NCEEQUAL -> new OperatorFlags(MatchType.EXACT, true, true);
            case SearchParser.NREQUAL -> new OperatorFlags(MatchType.REGEX, false, true);
            case SearchParser.NCREEQUAL -> new OperatorFlags(MatchType.REGEX, true, true);
            default -> new OperatorFlags(MatchType.INEXACT, false, false);
        };
    }

}
