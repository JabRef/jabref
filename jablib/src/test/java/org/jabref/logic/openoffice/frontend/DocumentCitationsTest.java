package org.jabref.logic.openoffice.frontend;

import java.util.List;
import java.util.Map;

import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.engine.storage.MarkStore;
import org.jabref.logic.openoffice.rangesort.BetweenSetsRangeOverlapFinder;
import org.jabref.logic.openoffice.rangesort.RangeOverlap;
import org.jabref.logic.openoffice.rangesort.RangeOverlapKind;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationGroups;
import org.jabref.model.openoffice.util.OOVoidResult;

import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class DocumentCitationsTest {

    @ParameterizedTest
    @CsvSource({"EQUAL_RANGE, Found identical ranges", "OVERLAP, Found overlapping ranges", "TOUCH, Found touching ranges"})
    void cursorOverlapMessageStartsWithProtectedAreaExplanationAndRetainsRangeDetails(RangeOverlapKind kind, String description) throws Exception {
        XTextDocument document = mock(XTextDocument.class);
        DocumentCitations citations = mock(DocumentCitations.class, CALLS_REAL_METHODS);
        RangeForOverlapCheck<CitationGroupId> cursor = new RangeForOverlapCheck<>(mock(XTextRange.class), new CitationGroupId("cursor"),
                RangeForOverlapCheck.CURSOR_MARK_KIND, "cursor");
        RangeForOverlapCheck<CitationGroupId> protectedRange = new RangeForOverlapCheck<>(mock(XTextRange.class), new CitationGroupId("protected"),
                RangeForOverlapCheck.REFERENCE_MARK_KIND, "xyz");
        List<RangeForOverlapCheck<CitationGroupId>> userRanges = List.of(cursor);
        List<RangeForOverlapCheck<CitationGroupId>> protectedRanges = List.of(protectedRange);
        List<RangeOverlap<RangeForOverlapCheck<CitationGroupId>>> overlaps = List.of(new RangeOverlap<>(kind, List.of(cursor, protectedRange)));
        doReturn(protectedRanges).when(citations).getCitationRanges(document);
        doReturn(List.of()).when(citations).bibliographyRanges(document);
        doReturn(List.of()).when(citations).footnoteMarkRanges(protectedRanges);
        try (MockedStatic<BetweenSetsRangeOverlapFinder> finder = mockStatic(BetweenSetsRangeOverlapFinder.class)) {
            finder.when(() -> BetweenSetsRangeOverlapFinder.findFirst(document, userRanges, protectedRanges, false)).thenReturn(overlaps);

            OOVoidResult<JabRefException> result = citations.checkRangeOverlapsWithCursor(document, userRanges, false);

            String details = Localization.lang(description) + ": 'cursor', 'xyz'\n";
            assertEquals(Localization.lang("The cursor is in a protected area.") + "\n" + details, result.getError().getLocalizedMessage());
            assertEquals(details, DocumentCitations.rangeOverlapsToMessage(overlaps));
        }
    }

    @Test
    void constructorLoadsCitationGroupsFromItsMarkStore() throws Exception {
        XTextDocument doc = mock(XTextDocument.class);
        CitationGroups groups = new CitationGroups(Map.of());

        try (MockedConstruction<MarkStore> stores = mockConstruction(MarkStore.class, (store, _) ->
                when(store.readAll(doc)).thenReturn(groups))) {
            DocumentCitations citations = new DocumentCitations(doc);

            assertSame(groups, citations.citationGroups);
            assertSame(stores.constructed().getFirst(), citations.markStore);
            verify(citations.markStore).readAll(doc);
        }
    }

    @Test
    void constructorPropagatesStoreRuntimeException() {
        XTextDocument doc = mock(XTextDocument.class);
        com.sun.star.uno.RuntimeException failure = new com.sun.star.uno.RuntimeException("boom");

        try (MockedConstruction<MarkStore> stores = mockConstruction(MarkStore.class, (store, _) ->
                when(store.readAll(doc)).thenThrow(failure))) {
            com.sun.star.uno.RuntimeException exception = assertThrows(com.sun.star.uno.RuntimeException.class,
                    () -> new DocumentCitations(doc));

            assertEquals("boom", exception.getMessage());
            assertSame(failure, exception);
        }
    }
}
