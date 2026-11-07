package org.jabref.logic.openoffice.frontend;

import java.util.Map;

import org.jabref.logic.openoffice.engine.storage.MarkStore;
import org.jabref.model.openoffice.style.CitationGroups;

import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class DocumentCitationsTest {

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
