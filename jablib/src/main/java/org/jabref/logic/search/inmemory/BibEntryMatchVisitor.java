package org.jabref.logic.search.inmemory;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jabref.logic.search.query.SearchFieldConstants;
import org.jabref.logic.search.query.SearchQueryConversion;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.DateGroup;
import org.jabref.model.search.SearchFlags;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Walks a Search.g4 parse tree against a single [BibEntry] and returns whether the entry matches.
///
/// Operator semantics mirror [org.jabref.logic.search.query.SearchToSqlVisitor].
/// Pseudo-fields supported: `any` / `anyfield`, `key`, `entrytype`, `anykeyword`.
class BibEntryMatchVisitor extends SearchBaseVisitor<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibEntryMatchVisitor.class);
    private static final String GROUPS_FIELD_NAME = StandardField.GROUPS.getName();

    /// Compiled regexes shared across all visitor instances (one instance is created per [BibEntry],
    /// so an instance-level cache would not help when scanning a library). Keyed by pattern + case mode.
    private static final Cache<RegexKey, Pattern> COMPILED_PATTERNS =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(15))
                    .expireAfterAccess(Duration.ofMinutes(15))
                    .build();

    private record RegexKey(String pattern, boolean caseSensitive) {
    }

    private final BibEntry entry;
    private final EnumSet<SearchFlags> searchBarFlags;
    private final Character keywordSeparator;

    BibEntryMatchVisitor(BibEntry entry, EnumSet<SearchFlags> searchBarFlags, Character keywordSeparator) {
        this.entry = entry;
        this.searchBarFlags = searchBarFlags;
        this.keywordSeparator = keywordSeparator;
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
            SearchFlags matchKind = searchBarFlags.contains(SearchFlags.REGULAR_EXPRESSION)
                                    ? SearchFlags.REGULAR_EXPRESSION
                                    : SearchFlags.INEXACT_MATCH;
            return matchAnyField(term, matchKind, caseSensitive);
        }

        String fieldName = ctx.FIELD().getText().toLowerCase(Locale.ROOT);
        int operator = ctx.operator().getStart().getType();

        if (operator == SearchParser.GT || operator == SearchParser.LT ||
            operator == SearchParser.GTE || operator == SearchParser.LTE) {
            return compareFieldValue(fieldName, term, operator);
        }

        OperatorFlags flags = mapOperator(operator);

        // field = "" / field != "" — presence/absence
        if (term.isEmpty()) {
            boolean present = isFieldPresent(fieldName);
            return flags.negation ? present : !present;
        }

        boolean matched = matchField(fieldName, term, flags.matchKind, flags.caseSensitive);
        return flags.negation != matched;
    }

    private boolean isFieldPresent(String fieldName) {
        return switch (fieldName) {
            case SearchFieldConstants.KEY,
                 SearchFieldConstants.CITATION_KEY ->
                    entry.getCitationKey().isPresent();
            case SearchFieldConstants.ENTRY_TYPE ->
                    true; // every entry has a type
            case SearchFieldConstants.ANY_FIELD,
                 SearchFieldConstants.ANY_FIELD_ALIAS ->
                    entry.getFields().stream()
                         .anyMatch(f -> !GROUPS_FIELD_NAME.equals(f.getName()));
            case SearchFieldConstants.ANY_KEYWORD ->
                    entry.getField(StandardField.KEYWORDS).isPresent();
            default ->
                    entry.getFields().stream()
                         .anyMatch(f -> f.getName().equalsIgnoreCase(fieldName));
        };
    }

    private boolean matchField(String fieldName, String term, SearchFlags matchKind, boolean caseSensitive) {
        return switch (fieldName) {
            case SearchFieldConstants.KEY,
                 SearchFieldConstants.CITATION_KEY ->
                    entry.getCitationKey()
                         .map(v -> matchValue(v, term, matchKind, caseSensitive))
                         .orElse(false);
            case SearchFieldConstants.ENTRY_TYPE ->
                    matchValue(entry.getType().getName(), term, matchKind, caseSensitive);
            case SearchFieldConstants.ANY_FIELD,
                 SearchFieldConstants.ANY_FIELD_ALIAS ->
                    matchAnyField(term, matchKind, caseSensitive);
            case SearchFieldConstants.ANY_KEYWORD ->
                    matchAnyKeyword(term, matchKind, caseSensitive);
            default ->
                    matchNamedField(fieldName, term, matchKind, caseSensitive);
        };
    }

    private boolean matchNamedField(String fieldName, String term, SearchFlags matchKind, boolean caseSensitive) {
        for (Field field : entry.getFields()) {
            if (!field.getName().equalsIgnoreCase(fieldName)) {
                continue;
            }
            if (entry.getField(field)
                     .filter(value -> matchValue(value, term, matchKind, caseSensitive))
                     .isPresent()) {
                return true;
            }
        }
        return false;
    }

    private boolean matchAnyField(String term, SearchFlags matchKind, boolean caseSensitive) {
        for (Field field : entry.getFields()) {
            if (GROUPS_FIELD_NAME.equals(field.getName())) {
                continue;
            }
            if (entry.getField(field)
                     .filter(value -> matchValue(value, term, matchKind, caseSensitive))
                     .isPresent()) {
                return true;
            }
        }
        return false;
    }

    private boolean matchAnyKeyword(String term, SearchFlags matchKind, boolean caseSensitive) {
        List<String> keywords = entry.getKeywords(keywordSeparator).stream().map(Object::toString).toList();
        return keywords.stream().anyMatch(k -> matchValue(k, term, matchKind, caseSensitive));
    }

    /// @param matchKind one of [SearchFlags#INEXACT_MATCH], [SearchFlags#EXACT_MATCH], [SearchFlags#REGULAR_EXPRESSION]
    private static boolean matchValue(String value, String term, SearchFlags matchKind, boolean caseSensitive) {
        return switch (matchKind) {
            case INEXACT_MATCH ->
                    caseSensitive
                    ? value.contains(term)
                    : value.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT));
            case EXACT_MATCH ->
                    caseSensitive ? value.equals(term) : value.equalsIgnoreCase(term);
            case REGULAR_EXPRESSION ->
                    matchRegex(value, term, caseSensitive);
            default ->
                    throw new IllegalArgumentException("matchKind must be INEXACT_MATCH, EXACT_MATCH, or REGULAR_EXPRESSION, got " + matchKind);
        };
    }

    private static boolean matchRegex(String value, String pattern, boolean caseSensitive) {
        try {
            Pattern compiled = COMPILED_PATTERNS.asMap().computeIfAbsent(
                    new RegexKey(pattern, caseSensitive),
                    key -> Pattern.compile(key.pattern(), key.caseSensitive() ? 0 : Pattern.CASE_INSENSITIVE));
            return compiled.matcher(value).find();
        } catch (PatternSyntaxException e) {
            LOGGER.debug("Invalid regex pattern '{}': {}", pattern, e.getMessage());
            return false;
        }
    }

    private boolean compareFieldValue(String fieldName, String term, int operator) {
        Field field = FieldFactory.parseField(fieldName);

        if (field.getProperties().contains(FieldProperty.YEAR)) {
            try {
                int termYear = Integer.parseInt(term.strip());
                // extractDate handles the date→year alias: an entry with only "date = 2022-11-05"
                // is matched by "year >= 2020" because getFieldOrAlias resolves date to year
                return DateGroup.extractDate(field, entry)
                                .flatMap(d -> d.getYear())
                                .map(entryYear -> compareInts(entryYear, termYear, operator))
                                .orElse(false);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        if (field.getProperties().contains(FieldProperty.DATE)) {
            int dashes = (int) term.chars().filter(c -> c == '-').count();
            return DateGroup.extractDate(field, entry)
                            .map(d -> normalizeDateKey(d, dashes))
                            .map(entryKey -> compareStrings(entryKey, term, operator))
                            .orElse(false);
        }

        return entry.getField(field)
                    .map(v -> compareStrings(v.strip(), term.strip(), operator))
                    .orElse(false);
    }

    private static String normalizeDateKey(org.jabref.model.entry.Date d, int termDashes) {
        int year = d.getYear().orElse(0);
        int month = d.getMonth().map(org.jabref.model.entry.Month::getNumber).orElse(1);
        int day = d.getDay().orElse(1);
        return switch (termDashes) {
            case 0  -> "%04d".formatted(year);
            case 1  -> "%04d-%02d".formatted(year, month);
            default -> "%04d-%02d-%02d".formatted(year, month, day);
        };
    }

    private static boolean compareInts(int a, int b, int operator) {
        return switch (operator) {
            case SearchParser.GT  -> a > b;
            case SearchParser.LT  -> a < b;
            case SearchParser.GTE -> a >= b;
            case SearchParser.LTE -> a <= b;
            default -> false;
        };
    }

    private static boolean compareStrings(String a, String b, int operator) {
        int cmp = a.compareTo(b);
        return switch (operator) {
            case SearchParser.GT  -> cmp > 0;
            case SearchParser.LT  -> cmp < 0;
            case SearchParser.GTE -> cmp >= 0;
            case SearchParser.LTE -> cmp <= 0;
            default -> false;
        };
    }

    /// @param matchKind one of [SearchFlags#INEXACT_MATCH], [SearchFlags#EXACT_MATCH], [SearchFlags#REGULAR_EXPRESSION]
    private record OperatorFlags(SearchFlags matchKind, boolean caseSensitive, boolean negation) {
    }

    private static OperatorFlags mapOperator(int tokenType) {
        return switch (tokenType) {
            case SearchParser.EQUAL,
                 SearchParser.CONTAINS ->
                    new OperatorFlags(SearchFlags.INEXACT_MATCH, false, false);
            case SearchParser.CEQUAL ->
                    new OperatorFlags(SearchFlags.INEXACT_MATCH, true, false);
            case SearchParser.EEQUAL,
                 SearchParser.MATCHES ->
                    new OperatorFlags(SearchFlags.EXACT_MATCH, false, false);
            case SearchParser.CEEQUAL ->
                    new OperatorFlags(SearchFlags.EXACT_MATCH, true, false);
            case SearchParser.REQUAL ->
                    new OperatorFlags(SearchFlags.REGULAR_EXPRESSION, false, false);
            case SearchParser.CREEQUAL ->
                    new OperatorFlags(SearchFlags.REGULAR_EXPRESSION, true, false);
            case SearchParser.NEQUAL ->
                    new OperatorFlags(SearchFlags.INEXACT_MATCH, false, true);
            case SearchParser.NCEQUAL ->
                    new OperatorFlags(SearchFlags.INEXACT_MATCH, true, true);
            case SearchParser.NEEQUAL ->
                    new OperatorFlags(SearchFlags.EXACT_MATCH, false, true);
            case SearchParser.NCEEQUAL ->
                    new OperatorFlags(SearchFlags.EXACT_MATCH, true, true);
            case SearchParser.NREQUAL ->
                    new OperatorFlags(SearchFlags.REGULAR_EXPRESSION, false, true);
            case SearchParser.NCREEQUAL ->
                    new OperatorFlags(SearchFlags.REGULAR_EXPRESSION, true, true);
            default ->
                    new OperatorFlags(SearchFlags.INEXACT_MATCH, false, false);
        };
    }
}
