package org.jabref.logic.openoffice.engine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.openoffice.engine.storage.MarkStore;
import org.jabref.logic.openoffice.engine.storage.MarkWriter;
import org.jabref.logic.openoffice.frontend.DocumentCitations;
import org.jabref.logic.openoffice.uno.UnoTextRange;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationGroups;
import org.jabref.model.openoffice.style.CitationType;

import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@NullMarked
class CitationGroupEditorTest {

    private final XTextDocument document = mock(XTextDocument.class);
    private final XComponentContext context = mock(XComponentContext.class);
    private final MarkWriter writer = mock(MarkWriter.class);
    private final CitationGroupEditor editor = new CitationGroupEditor(document, context, writer);

    @Test
    void structuralNoOpsDoNotLockControllersOrInspectChangeRecording() throws Exception {
        CitationGroups groups = new CitationGroups(new LinkedHashMap<>());
        try (MockedConstruction<MarkStore> stores = storesFor(groups)) {
            DocumentCitations documentCitations = new DocumentCitations(document);

            assertEquals(false, editor.merge(documentCitations).get());
            assertEquals(false, editor.separate(documentCitations).get());
            verifyNoInteractions(document, context, writer);
        }
    }

    @Test
    void insertionPreservesPreparedIdentityAndPageInfosWithoutWritingAMarker() throws Exception {
        List<String> keys = List.of("beta", "alpha", "beta");
        List<Optional<OOText>> pageInfos = List.of(Optional.of(OOText.fromString("p. 5")), Optional.empty(), Optional.of(OOText.fromString("p. 9")));
        CitationType type = CitationType.AUTHOR_ONLY;
        String uid = "prepared-uid";
        List<Citation> citations = keys.stream().map(Citation::new).toList();
        for (int index = 0; index < citations.size(); index++) {
            citations.get(index).setPageInfo(pageInfos.get(index));
        }
        CitationGroup created = new CitationGroup(new CitationGroupId(uid), type, citations,
                Optional.of(MarkCodec.encode(new JabRefMarkData(keys, uid, type))));
        XTextCursor position = mock(XTextCursor.class);
        try (MockedConstruction<MarkStore> stores = storesFor(new CitationGroups(new LinkedHashMap<>()))) {
            DocumentCitations documentCitations = new DocumentCitations(document);
            MarkStore store = stores.constructed().getFirst();
            when(store.createCitationGroup(document, context, keys, pageInfos, type, position, true, false, uid)).thenReturn(created);

            CitationGroup inserted = editor.insert(documentCitations, keys, pageInfos, type, position, uid, true, false);

            assertEquals(uid, inserted.groupId.citationGroupIdAsString());
            assertEquals(keys, inserted.citationsInStorageOrder.stream().map(Citation::getCitationKey).toList());
            assertEquals(pageInfos, inserted.citationsInStorageOrder.stream().map(Citation::getPageInfo).toList());
            assertEquals(type, inserted.citationType);
            assertEquals(List.of(created), documentCitations.citationGroups.getCitationGroupsUnordered());
            verify(store).createCitationGroup(document, context, keys, pageInfos, type, position, true, false, uid);
            verifyNoInteractions(document, context, writer, position);
        }
    }

    @ParameterizedTest
    @EnumSource(value = CitationType.class, mode = EnumSource.Mode.EXCLUDE, names = "AUTHORYEAR_PAR")
    void separationLeavesNonNormalGroupsUnchanged(CitationType type) throws Exception {
        CitationGroup group = new CitationGroup(new CitationGroupId("untouched"), type,
                List.of(new Citation("alpha"), new Citation("beta")), Optional.empty());
        CitationGroups groups = new CitationGroups(Map.of(group.groupId, group));
        try (MockedConstruction<MarkStore> stores = storesFor(groups)) {
            DocumentCitations documentCitations = new DocumentCitations(document);

            assertEquals(false, editor.separate(documentCitations).get());
            assertEquals(List.of(group), documentCitations.citationGroups.getCitationGroupsUnordered());
            verify(stores.constructed().getFirst(), never()).getMarkRange(any(), any());
            verifyNoInteractions(document, context, writer);
        }
    }

    @Test
    void separationPreparationIsReadOnlyAndPreservesGlobalOrderAndStableIdentities() throws Exception {
        CitationGroup before = group("before", CitationType.AUTHORYEAR_PAR, List.of("before"), List.of(Optional.empty()));
        CitationGroup original = group("original", CitationType.AUTHORYEAR_PAR, List.of("beta", "alpha"),
                List.of(info("p. 5"), info("p. 7")));
        CitationGroup after = group("after", CitationType.AUTHOR_ONLY, List.of("after"), List.of(Optional.empty()));
        CitationGroups groups = groups(before, original, after);
        try (MockedConstruction<MarkStore> stores = storesFor(groups)) {
            DocumentCitations documentCitations = new DocumentCitations(document);
            MarkStore store = stores.constructed().getFirst();
            XText text = mock(XText.class);
            XTextCursor range = mock(XTextCursor.class);
            when(text.createTextCursorByRange(any())).thenReturn(range);
            markRange(store, original, text);
            List<CitationGroup> order = List.of(after, original, before);

            CitationGroupEditor.PreparedEdit prepared = editor.prepareSeparate(documentCitations, order);
            List<Optional<String>> preparedNames = prepared.groupsInDocumentOrder().stream().map(CitationGroup::getReferenceMarkNameForLinking).toList();
            CitationGroupEditor.PreparedEdit repeated = editor.prepareSeparate(documentCitations, order);

            assertEquals(List.of(List.of("after"), List.of("beta"), List.of("alpha"), List.of("before")),
                    prepared.groupsInDocumentOrder().stream().map(CitationGroupEditorTest::keys).toList());
            assertEquals(List.of(info("p. 5"), info("p. 7")), prepared.groupsInDocumentOrder().subList(1, 3).stream()
                                                                      .map(group -> group.citationsInStorageOrder.getFirst().getPageInfo()).toList());
            assertEquals(preparedNames, prepared.groupsInDocumentOrder().stream().map(CitationGroup::getReferenceMarkNameForLinking).toList());
            assertEquals(4L, Stream.concat(prepared.groupsInDocumentOrder().subList(1, 3).stream(), repeated.groupsInDocumentOrder().subList(1, 3).stream())
                                   .map(group -> group.groupId.citationGroupIdAsString()).distinct().count());
            assertEquals(List.of(before, original, after), groups.getCitationGroupsUnordered());
            assertEquals(List.of(info("p. 5"), info("p. 7")), original.citationsInStorageOrder.stream().map(Citation::getPageInfo).toList());
            verify(range, never()).setString(anyString());
            verify(store, never()).removeCitationGroup(any(), any());
            verify(store, never()).createCitationGroup(any(), any(), anyList(), anyList(), any(), any(), anyBoolean(), anyBoolean(), anyString());
            verifyNoInteractions(document, context, writer);
        }
    }

    @Test
    void mergePreparationUsesUnmappedKeyOrderAtTheOriginalGlobalPosition() throws Exception {
        CitationGroup first = group("first", CitationType.AUTHORYEAR_PAR, List.of("beta"), List.of(info("p. 5")));
        CitationGroup second = group("second", CitationType.AUTHORYEAR_PAR, List.of("alpha"), List.of(info("p. 7")));
        CitationGroup other = group("other", CitationType.AUTHOR_ONLY, List.of("other"), List.of(Optional.empty()));
        try (MockedConstruction<MarkStore> stores = storesFor(groups(first, second, other));
             MockedStatic<UnoTextRange> comparisons = mockStatic(UnoTextRange.class)) {
            DocumentCitations documentCitations = spy(new DocumentCitations(document));
            doReturn(List.of(first, second, other)).when(documentCitations).getCitationGroupsSortedWithinPartitions(document, false);
            MarkStore store = stores.constructed().getFirst();
            XText text = mock(XText.class);
            XTextCursor range = mock(XTextCursor.class);
            when(text.createTextCursorByRange(any())).thenReturn(range);
            when(range.getString()).thenReturn(" ");
            XTextRange firstRange = markRange(store, first, text);
            XTextRange secondRange = markRange(store, second, text);
            markRange(store, other, text);
            comparisons.when(() -> UnoTextRange.comparables(firstRange, secondRange)).thenReturn(true);
            comparisons.when(() -> UnoTextRange.compareStarts(firstRange.getEnd(), secondRange.getStart())).thenReturn(-1);

            CitationGroupEditor.PreparedEdit prepared = editor.prepareMerge(documentCitations, List.of(other, first, second));
            CitationGroupEditor.PreparedEdit repeated = editor.prepareMerge(documentCitations, List.of(other, first, second));

            assertEquals(List.of(List.of("other"), List.of("beta", "alpha")), prepared.groupsInDocumentOrder().stream().map(CitationGroupEditorTest::keys).toList());
            CitationGroup merged = prepared.groupsInDocumentOrder().getLast();
            assertNotEquals(merged.groupId.citationGroupIdAsString(), repeated.groupsInDocumentOrder().getLast().groupId.citationGroupIdAsString());
            assertEquals(List.of(info("p. 5"), info("p. 7")), merged.citationsInStorageOrder.stream().map(Citation::getPageInfo).toList());
            assertEquals(merged.groupId.citationGroupIdAsString(), MarkCodec.decode(merged.getReferenceMarkNameForLinking().orElseThrow()).uniqueId());
            verify(range, never()).setString(anyString());
            verify(store, never()).removeCitationGroup(any(), any());
            verifyNoInteractions(document, context, writer);
        }
    }

    @Test
    void structuralApplyPreservesPreparedNamesAndReturnsStoredIdsWithoutPlaceholders() throws Exception {
        CitationGroup original = group("original", CitationType.AUTHORYEAR_PAR, List.of("beta", "alpha"),
                List.of(info("p. 5"), info("p. 7")));
        try (MockedConstruction<MarkStore> stores = storesFor(groups(original))) {
            DocumentCitations documentCitations = new DocumentCitations(document);
            MarkStore store = stores.constructed().getFirst();
            XText text = mock(XText.class);
            XTextCursor range = mock(XTextCursor.class);
            when(text.createTextCursorByRange(any())).thenReturn(range);
            markRange(store, original, text);
            CitationGroupEditor.PreparedEdit prepared = editor.prepareSeparate(documentCitations, List.of(original));
            when(store.createCitationGroup(eq(document), eq(context), anyList(), anyList(), eq(CitationType.AUTHORYEAR_PAR),
                    eq(range), eq(false), anyBoolean(), anyString())).thenAnswer(invocation ->
                    group(invocation.getArgument(8), invocation.getArgument(4), invocation.getArgument(2), invocation.getArgument(3)));

            List<CitationGroup> stored = editor.apply(documentCitations, prepared);

            assertEquals(prepared.groupsInDocumentOrder().stream().map(CitationGroup::getReferenceMarkNameForLinking).toList(),
                    stored.stream().map(CitationGroup::getReferenceMarkNameForLinking).toList());
            for (int index = 0; index < stored.size(); index++) {
                assertNotSame(prepared.groupsInDocumentOrder().get(index).groupId, stored.get(index).groupId);
                assertEquals(prepared.groupsInDocumentOrder().get(index).groupId.citationGroupIdAsString(), stored.get(index).groupId.citationGroupIdAsString());
                verify(writer).invalidate(stored.get(index).groupId.citationGroupIdAsString());
            }
            assertEquals(stored, documentCitations.citationGroups.getCitationGroupsInGlobalOrder());
            verify(writer, never()).write(any(), any(), any());
            verify(range).setString("");
        }
    }

    private XTextRange markRange(MarkStore store, CitationGroup group, XText text) throws Exception {
        XTextRange range = mock(XTextRange.class);
        when(range.getText()).thenReturn(text);
        when(range.getStart()).thenReturn(mock(XTextRange.class));
        when(range.getEnd()).thenReturn(mock(XTextRange.class));
        when(store.getMarkRange(group, document)).thenReturn(Optional.of(range));
        return range;
    }

    private static CitationGroup group(String uid, CitationType type, List<String> keys, List<Optional<OOText>> pageInfos) throws MarkCodecException {
        List<Citation> citations = keys.stream().map(Citation::new).toList();
        for (int index = 0; index < citations.size(); index++) {
            citations.get(index).setPageInfo(pageInfos.get(index));
        }
        return new CitationGroup(new CitationGroupId(uid), type, citations, Optional.of(MarkCodec.encode(new JabRefMarkData(keys, uid, type))));
    }

    private static CitationGroups groups(CitationGroup... groups) {
        Map<CitationGroupId, CitationGroup> byId = new LinkedHashMap<>();
        for (CitationGroup group : groups) {
            byId.put(group.groupId, group);
        }
        return new CitationGroups(byId);
    }

    private static List<String> keys(CitationGroup group) {
        return group.citationsInStorageOrder.stream().map(Citation::getCitationKey).toList();
    }

    private static Optional<OOText> info(String text) {
        return Optional.of(OOText.fromString(text));
    }

    private MockedConstruction<MarkStore> storesFor(CitationGroups groups) {
        return mockConstruction(MarkStore.class, (store, _) -> when(store.readAll(document)).thenReturn(groups));
    }
}
