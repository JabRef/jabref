package org.jabref.logic.openoffice.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
class CitationNumberingTest {

    @ParameterizedTest
    @MethodSource("firstAppearanceCases")
    void numbersByFirstAppearance(List<String> keys, Map<String, Integer> expected) {
        assertEquals(expected, CitationNumbering.firstAppearance(keys));
    }

    private static Stream<Arguments> firstAppearanceCases() {
        return Stream.of(
                Arguments.of(List.of(), Map.of()),
                Arguments.of(List.of("only"), Map.of("only", 1)),
                Arguments.of(List.of("b", "a", "b", "c", "a"), Map.of("b", 1, "a", 2, "c", 3)),
                Arguments.of(List.of("Smith2020", "Doe1999", "Smith2020"), Map.of("Smith2020", 1, "Doe1999", 2)));
    }

    @Test
    void usesBibliographyOrderForCitedKeys() {
        assertEquals(Map.of("a", 1, "b", 2, "c", 3),
                CitationNumbering.bibliographyOrder(List.of("c", "b", "a", "b"), List.of("a", "b", "c")));
    }

    @Test
    void appendsKeysMissingFromBibliographyInDocumentOrder() {
        assertEquals(Map.of("a", 1, "b", 2, "unlistedFirst", 3, "unlistedSecond", 4),
                CitationNumbering.bibliographyOrder(List.of("unlistedFirst", "b", "unlistedSecond", "a", "unlistedFirst"), List.of("a", "b")));
    }

    @Test
    void ignoresUncitedAndRepeatedBibliographyKeys() {
        assertEquals(Map.of("b", 1, "a", 2),
                CitationNumbering.bibliographyOrder(List.of("a", "b"), List.of("uncited", "b", "b", "a", "otherUncited")));
    }

    @Test
    void emptyCitedSetDoesNotNumberBibliographyKeys() {
        assertEquals(Map.of(), CitationNumbering.bibliographyOrder(List.of(), List.of("uncited")));
    }

    @Test
    void iteratesInAssignedNumberOrder() {
        Map<String, Integer> numbers = CitationNumbering.bibliographyOrder(List.of("c", "b", "a", "d"), List.of("b", "a"));

        assertEquals(List.of("b", "a", "c", "d"), List.copyOf(numbers.keySet()));
    }

    @Test
    void startsFreshForEachRender() {
        CitationNumbering.firstAppearance(List.of("oldFirst", "oldSecond"));

        assertEquals(Map.of("newFirst", 1), CitationNumbering.firstAppearance(List.of("newFirst")));
    }

    @Test
    void exposesImmutableNumbers() {
        Map<String, Integer> numbers = CitationNumbering.firstAppearance(List.of("key"));

        assertThrows(UnsupportedOperationException.class, () -> numbers.put("other", 2));
    }

    @Test
    void doesNotModifyInputOrders() {
        List<String> citedKeys = new ArrayList<>(List.of("b", "a", "b"));
        List<String> bibliographyKeys = new ArrayList<>(List.of("a", "b"));
        CitationNumbering.bibliographyOrder(citedKeys, bibliographyKeys);

        assertEquals(List.of(List.of("b", "a", "b"), List.of("a", "b")), List.of(citedKeys, bibliographyKeys));
    }

    @Test
    void doesNotRetainInputOrders() {
        List<String> keys = new ArrayList<>(List.of("key"));
        Map<String, Integer> numbers = CitationNumbering.firstAppearance(keys);
        keys.set(0, "other");

        assertEquals(Map.of("key", 1), numbers);
    }
}
