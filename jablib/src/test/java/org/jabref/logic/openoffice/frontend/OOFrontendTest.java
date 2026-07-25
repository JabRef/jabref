package org.jabref.logic.openoffice.frontend;

import org.jabref.logic.JabRefException;
import org.jabref.logic.openoffice.backend.Backend52;
import org.jabref.model.openoffice.util.OOResult;

import com.sun.star.text.XTextDocument;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class OOFrontendTest {

    @Test
    void createReturnsErrorWhenBackendThrowsRuntimeException() {
        XTextDocument doc = mock(XTextDocument.class);

        try (MockedConstruction<Backend52> mockedBackend = mockConstruction(Backend52.class, (backend, _) ->
                when(backend.getJabRefReferenceMarkNames(doc)).thenThrow(new com.sun.star.uno.RuntimeException("boom")))) {
            OOResult<OOFrontend, JabRefException> result = OOFrontend.create(doc);

            assertTrue(result.isError());
            assertEquals("boom", result.getError().getMessage());
            assertInstanceOf(com.sun.star.uno.RuntimeException.class, result.getError().getCause());
        }
    }
}
