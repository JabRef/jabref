package org.jabref.logic.openoffice.engine.storage;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationType;

import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@NullMarked
class MarkStoreTest {

    private final XTextDocument document = mock(XTextDocument.class);
    private final XComponentContext context = mock(XComponentContext.class);
    private final XTextCursor position = mock(XTextCursor.class);
    private final MarkStore store = new MarkStore();

    @ParameterizedTest
    @ValueSource(strings = {"", "white space", "comma,key", "non\u00a0breaking", "line\nbreak"})
    void invalidKeysAreRejectedBeforeAnyDocumentMutation(String key) {
        CreationException exception = assertThrows(CreationException.class, () -> store.createCitationGroup(document, context,
                List.of(key), List.of(Optional.empty()), CitationType.AUTHORYEAR_PAR, position, false, false));

        assertEquals("Invalid canonical citation identity: INVALID_KEY", exception.getMessage());
        verifyNoInteractions(document, context, position);
    }

    @Test
    void referenceMarkRejectsUnknownGroupWithoutDocumentAccess() {
        CitationGroup group = new CitationGroup(new CitationGroupId("unknown"), CitationType.AUTHORYEAR_PAR,
                List.of(new Citation("alpha")), Optional.empty());

        NoDocumentException failure = assertThrows(NoDocumentException.class, () -> store.referenceMark(group));

        assertEquals("Unknown citation group: unknown", failure.getMessage());
        verifyNoInteractions(document, context, position);
    }

    @Test
    void emptyGroupsAreRejectedBeforeAnyDocumentMutation() {
        CreationException exception = assertThrows(CreationException.class, () -> store.createCitationGroup(document, context,
                List.of(), List.of(), CitationType.AUTHORYEAR_PAR, position, false, false));

        assertEquals("Invalid canonical citation identity: EMPTY_KEYS", exception.getMessage());
        verifyNoInteractions(document, context, position);
    }

    @Test
    void missingPageInfoSlotsAreRejectedBeforeAnyDocumentMutation() {
        CreationException exception = assertThrows(CreationException.class, () -> store.createCitationGroup(document, context,
                List.of("alpha", "beta"), List.of(Optional.empty()), CitationType.AUTHORYEAR_PAR, position, false, false));

        assertEquals("A page-info slot is required for every citation key", exception.getMessage());
        verifyNoInteractions(document, context, position);
    }
}
