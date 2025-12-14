package org.jabref.logic.citation.contextextractor;

import java.util.List;
import java.util.Optional;

import org.jabref.model.citation.ReferenceEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitationMatcherTest {

    private CitationMatcher matcher;
    private List<ReferenceEntry> references;

    @BeforeEach
    void setUp() {
        matcher = new CitationMatcher();

        references = List.of(
                ReferenceEntry.builder("[1] Smith reference text", "[1]")
                        .authors("Smith, John")
                        .title("Machine Learning Basics")
                        .year("2020")
                        .build(),
                ReferenceEntry.builder("[2] Jones reference text", "[2]")
                        .authors("Jones, Alice")
                        .title("Deep Learning Applications")
                        .year("2019")
                        .build(),
                ReferenceEntry.builder("[3] Williams reference text", "[3]")
                        .authors("Williams, Bob")
                        .title("Neural Networks")
                        .year("2021")
                        .build()
        );
    }

    @Test
    void testMatchExactNumericMarker() {
        Optional<ReferenceEntry> result = matcher.matchMarkerToReference("[1]", references);

        assertTrue(result.isPresent());
        assertEquals("[1]", result.get().marker());
    }

    @Test
    void testMatchNumericWithoutBrackets() {
        Optional<ReferenceEntry> result = matcher.matchMarkerToReference("1", references);

        assertTrue(result.isPresent());
        assertEquals("[1]", result.get().marker());
    }

    @Test
    void testMatchAuthorYearMarker() {
        Optional<ReferenceEntry> result = matcher.matchMarkerToReference("(Smith 2020)", references);

        assertTrue(result.isPresent());
        assertEquals(Optional.of("Smith, John"), result.get().authors());
    }

    @Test
    void testMatchInlineAuthorYear() {
        Optional<ReferenceEntry> result = matcher.matchMarkerToReference("Smith (2020)", references);

        assertTrue(result.isPresent());
        assertEquals(Optional.of("2020"), result.get().year());
    }

    @Test
    void testMatchAuthorKeyMarker() {
        List<ReferenceEntry> authorKeyRefs = List.of(
                ReferenceEntry.builder("[Smith20] reference", "[Smith20]")
                        .authors("Smith, John")
                        .year("2020")
                        .build()
        );

        Optional<ReferenceEntry> result = matcher.matchMarkerToReference("[Smith20]", authorKeyRefs);

        assertTrue(result.isPresent());
    }

    @Test
    void testNoMatchReturnsEmpty() {
        Optional<ReferenceEntry> result = matcher.matchMarkerToReference("[99]", references);

        assertFalse(result.isPresent());
    }

    @Test
    void testMatchMultipleNumericMarkers() {
        List<ReferenceEntry> results = matcher.matchMultipleMarkers("[1,2]", references);

        assertEquals(2, results.size());
    }

    @Test
    void testMatchNumericRange() {
        List<ReferenceEntry> results = matcher.matchMultipleMarkers("[1-3]", references);

        assertEquals(3, results.size());
    }

    @Test
    void testMatchWithDetails() {
        Optional<CitationMatcher.MatchResult> result = matcher.matchWithDetails("[1]", references);

        assertTrue(result.isPresent());
        assertEquals(CitationMatcher.MatchType.EXACT_MARKER, result.get().matchType());
        assertTrue(result.get().confidence() >= 0.9);
    }

    @Test
    void testMatchWithDetailsAuthorYear() {
        Optional<CitationMatcher.MatchResult> result = matcher.matchWithDetails("(Jones 2019)", references);

        assertTrue(result.isPresent());
        assertEquals(CitationMatcher.MatchType.AUTHOR_YEAR, result.get().matchType());
    }

    @Test
    void testCalculateMatchScore() {
        double exactScore = matcher.calculateMatchScore("[1]", references.get(0));
        double wrongScore = matcher.calculateMatchScore("[99]", references.get(0));

        assertTrue(exactScore > wrongScore);
        assertEquals(1.0, exactScore, 0.01);
    }

    @Test
    void testNoMatchForUnrelatedMarker() {
        Optional<ReferenceEntry> result = matcher.matchMarkerToReference("UnknownAuthor", references);

        assertFalse(result.isPresent());
    }

    @Test
    void testNullMarkerReturnsEmpty() {
        Optional<ReferenceEntry> result = matcher.matchMarkerToReference(null, references);
        assertFalse(result.isPresent());
    }

    @Test
    void testEmptyMarkerReturnsEmpty() {
        Optional<ReferenceEntry> result = matcher.matchMarkerToReference("", references);
        assertFalse(result.isPresent());
    }

    @Test
    void testNullReferencesReturnsEmpty() {
        Optional<ReferenceEntry> result = matcher.matchMarkerToReference("[1]", null);
        assertFalse(result.isPresent());
    }

    @Test
    void testEmptyReferencesReturnsEmpty() {
        Optional<ReferenceEntry> result = matcher.matchMarkerToReference("[1]", List.of());
        assertFalse(result.isPresent());
    }

    @Test
    void testMatchResultConfidenceLevels() {
        CitationMatcher.MatchResult highConfidence = new CitationMatcher.MatchResult(
                references.get(0), 0.9, CitationMatcher.MatchType.EXACT_MARKER);
        assertTrue(highConfidence.isHighConfidence());
        assertFalse(highConfidence.isMediumConfidence());
        assertFalse(highConfidence.isLowConfidence());

        CitationMatcher.MatchResult mediumConfidence = new CitationMatcher.MatchResult(
                references.get(0), 0.6, CitationMatcher.MatchType.FUZZY);
        assertFalse(mediumConfidence.isHighConfidence());
        assertTrue(mediumConfidence.isMediumConfidence());
        assertFalse(mediumConfidence.isLowConfidence());

        CitationMatcher.MatchResult lowConfidence = new CitationMatcher.MatchResult(
                references.get(0), 0.3, CitationMatcher.MatchType.FUZZY);
        assertFalse(lowConfidence.isHighConfidence());
        assertFalse(lowConfidence.isMediumConfidence());
        assertTrue(lowConfidence.isLowConfidence());
    }

    @Test
    void testMatchAuthorWithYear() {
        List<ReferenceEntry> refs = List.of(
                ReferenceEntry.builder("[1] text", "[1]")
                        .authors("Smith, John")
                        .year("2020")
                        .build()
        );

        Optional<ReferenceEntry> result = matcher.matchMarkerToReference("(Smith 2020)", refs);

        assertTrue(result.isPresent());
    }

    @Test
    void testMatchAuthorYearWithDifferentFormats() {
        List<ReferenceEntry> refs = List.of(
                ReferenceEntry.builder("[1] text", "[1]")
                        .authors("Johnson, Alice")
                        .year("2019")
                        .build()
        );

        Optional<ReferenceEntry> result1 = matcher.matchMarkerToReference("(Johnson 2019)", refs);
        assertTrue(result1.isPresent());

        Optional<ReferenceEntry> result2 = matcher.matchMarkerToReference("Johnson (2019)", refs);
        assertTrue(result2.isPresent());
    }
}
