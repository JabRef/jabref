package org.jabref.gui.search;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HighlighterTest {

    @Test
    void buildSearchPatternEscapesLiteralSpecialCharacters() {
        SearchQuery searchQuery = new SearchQuery("title = *");

        Map<Optional<Field>, List<String>> groupedTerms = Highlighter.groupTermsByField(searchQuery);

        assertEquals(
                Optional.of("\\*"),
                Highlighter.buildSearchPattern(groupedTerms.get(Optional.of(StandardField.TITLE)))
        );
    }

    @Test
    void invalidRegularExpressionQueryDoesNotProduceHighlightTerms() {
        SearchQuery searchQuery = new SearchQuery("*", EnumSet.of(SearchFlags.REGULAR_EXPRESSION));

        assertEquals(Map.of(), Highlighter.groupTermsByField(searchQuery));
    }

    @Test
    void buildSearchPatternKeepsRegularExpressionTerms() {
        SearchQuery searchQuery = new SearchQuery("title =~ term.*");

        Map<Optional<Field>, List<String>> groupedTerms = Highlighter.groupTermsByField(searchQuery);

        assertEquals(
                Optional.of("term.*"),
                Highlighter.buildSearchPattern(groupedTerms.get(Optional.of(StandardField.TITLE)))
        );
    }
}
