package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.gui.entryeditor.citationrelationtab.CitationEntryReviewDialog.Decision;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class CitationEntryReviewTest {

    private List<CitationRelationItem> fetchedItems;
    private CitationEntryReview review;

    @BeforeEach
    void setUp() {
        fetchedItems = List.of(
                new CitationRelationItem(new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "Gut microbiota and cognition"), false),
                new CitationRelationItem(new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "Dietary flavanols and memory"), false),
                new CitationRelationItem(new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "Sleep and memory consolidation"), false));
        review = new CitationEntryReview(fetchedItems);
    }

    private static Stream<Arguments> decisionsWithAcceptedEntries() {
        return Stream.of(
                // every entry accepted
                Arguments.of(List.of(Decision.NEXT, Decision.NEXT, Decision.NEXT), List.of(0, 1, 2)),
                // every entry skipped
                Arguments.of(List.of(Decision.SKIP, Decision.SKIP, Decision.SKIP), List.of()),
                // the second entry skipped
                Arguments.of(List.of(Decision.NEXT, Decision.SKIP, Decision.NEXT), List.of(0, 2)),
                // the first entry accepted, then skipped after going back to it
                Arguments.of(List.of(Decision.NEXT, Decision.PREVIOUS, Decision.SKIP, Decision.NEXT, Decision.NEXT), List.of(1, 2)),
                // the review ended without imports after the first entry was accepted
                Arguments.of(List.of(Decision.NEXT, Decision.CANCEL), List.of())
        );
    }

    @ParameterizedTest
    @MethodSource("decisionsWithAcceptedEntries")
    void getAcceptedItemsContainsChosenEntries(List<Decision> decisions, List<Integer> expectedIndices) {
        decisions.forEach(review::applyDecision);

        assertEquals(expectedIndices.stream().map(fetchedItems::get).toList(), review.getAcceptedItems());
    }

    @Test
    void applyDecisionPreviousStaysOnFirstEntry() {
        review.applyDecision(Decision.PREVIOUS);

        assertEquals(1, review.getCurrentNumber());
    }

    @Test
    void isFinishedReturnsTrueAfterDecisionOnLastEntry() {
        List.of(Decision.NEXT, Decision.SKIP, Decision.NEXT).forEach(review::applyDecision);

        assertTrue(review.isFinished());
    }

    @Test
    void getCurrentEntryKeepsEditsAfterGoingBack() {
        review.getCurrentEntry().setField(StandardField.YEAR, "2024");
        review.applyDecision(Decision.NEXT);
        review.applyDecision(Decision.PREVIOUS);

        assertEquals(Optional.of("2024"), review.getCurrentEntry().getField(StandardField.YEAR));
    }

    @Test
    void getCurrentEntryEditsDoNotChangeFetchedEntry() {
        review.getCurrentEntry().setField(StandardField.YEAR, "2024");
        review.applyDecision(Decision.SKIP);

        assertEquals(Optional.empty(), fetchedItems.getFirst().entry().getField(StandardField.YEAR));
    }

    @Test
    void getAcceptedItemsContainsFileAttachedDuringReview() {
        review.getCurrentEntry().setField(StandardField.FILE, ":paper.pdf:PDF");
        List.of(Decision.NEXT, Decision.SKIP, Decision.SKIP).forEach(review::applyDecision);

        assertEquals(Optional.of(":paper.pdf:PDF"), review.getAcceptedItems().getFirst().entry().getField(StandardField.FILE));
    }

    @Test
    void getAcceptedItemsKeepsLocalEntry() {
        BibEntry localEntry = new BibEntry(StandardEntryType.Article).withCitationKey("Local2020");
        CitationRelationItem localItem = new CitationRelationItem(fetchedItems.getFirst().entry(), localEntry, true);
        CitationEntryReview localReview = new CitationEntryReview(List.of(localItem));

        localReview.applyDecision(Decision.NEXT);

        assertEquals(List.of(localItem), localReview.getAcceptedItems());
    }
}
