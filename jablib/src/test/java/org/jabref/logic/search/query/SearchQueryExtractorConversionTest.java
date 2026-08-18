package org.jabref.logic.search.query;

import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchQueryNode;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class SearchQueryExtractorConversionTest {
    public static Stream<Arguments> searchConversion() {
        return Stream.of(
                Arguments.of(List.of("term"), "term"),
                Arguments.of(List.of("regex\\.\\*term"), "regex.*term"),
                Arguments.of(List.of("term"), "any = term"),
                Arguments.of(List.of("term"), "any CONTAINS term"),
                Arguments.of(List.of("a", "b"), "a AND b"),
                Arguments.of(List.of("a", "b", "c"), "a OR b AND c"),
                Arguments.of(List.of("a", "b"), "a OR b AND NOT c"),
                Arguments.of(List.of("a", "b"), "author = a AND title = b"),
                Arguments.of(List.of(), "NOT a"),
                Arguments.of(List.of("a", "b", "c"), "(any = a OR any = b) AND NOT (NOT c AND title = d)"),
                Arguments.of(List.of("b", "c"), "title != a OR b OR c"),
                Arguments.of(List.of("a", "b"), "a b"),
                Arguments.of(List.of("term1 term2"), "\"term1 term2\""),
                // regex mode is left untouched, since the user explicitly opted into it
                Arguments.of(List.of("regex.*term"), "any =~ regex.*term")
        );
    }

    @ParameterizedTest
    @MethodSource
    void searchConversion(List<String> expected, String searchExpression) {
        List<String> result = SearchQueryConversion.extractSearchTerms(new SearchQuery(searchExpression)).stream().map(SearchQueryNode::term).toList();
        assertEquals(expected, result);
    }

    public static Stream<Arguments> literalTermsWithRegexMetacharactersProduceValidRegex() {
        return Stream.of(
                Arguments.of("*"),
                Arguments.of("fieldname=*"),
                Arguments.of("anything{"),
                Arguments.of("anything[")
        );
    }

    /// Non-regex search terms containing regex metacharacters (`*`, `{`, `[`, `\`, ...) must always
    /// compile to a valid Java regex, since [org.jabref.gui.search.Highlighter] embeds them
    /// directly into a `Pattern`. See <https://github.com/JabRef/jabref/issues/16539>.
    @ParameterizedTest
    @MethodSource
    void literalTermsWithRegexMetacharactersProduceValidRegex(String searchExpression) {
        List<SearchQueryNode> terms = SearchQueryConversion.extractSearchTerms(new SearchQuery(searchExpression, EnumSet.noneOf(SearchFlags.class)));
        for (SearchQueryNode node : terms) {
            assertDoesNotThrow(() -> Pattern.compile("(?i)(" + node.term() + ")"),
                    () -> "Term produced an invalid regex: " + node.term());
        }
    }

    /// Literal terms must not use Java's `Pattern.quote` (`\Q...\E`), because the same pattern string
    /// is also sent to PostgreSQL's `regexp_mark`/`regexp_positions` functions for highlighting when
    /// the experimental Postgres search backend is enabled, and PostgreSQL's regex engine does not
    /// understand `\Q`/`\E`. Per-character backslash escaping is valid in both engines.
    @ParameterizedTest
    @MethodSource("literalTermsWithRegexMetacharactersProduceValidRegex")
    void literalTermsAreEscapedInAPostgresCompatibleWay(String searchExpression) {
        List<SearchQueryNode> terms = SearchQueryConversion.extractSearchTerms(new SearchQuery(searchExpression, EnumSet.noneOf(SearchFlags.class)));
        for (SearchQueryNode node : terms) {
            assertFalse(node.term().contains("\\Q") || node.term().contains("\\E"),
                    "Term used Java-only \\Q...\\E quoting, which PostgreSQL's regex engine cannot parse: " + node.term());
        }
    }
}
