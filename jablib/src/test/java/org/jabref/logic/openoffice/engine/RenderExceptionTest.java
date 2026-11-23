package org.jabref.logic.openoffice.engine;

import java.io.IOException;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@NullMarked
class RenderExceptionTest {

    @Test
    void rawDiagnosticDoesNotBecomeLocalizedUserGuidance() {
        RenderException error = new RenderException("Internal renderer diagnostic");

        assertEquals("Internal renderer diagnostic", error.getMessage());
        assertEquals(Optional.empty(), error.localizedMessage());
    }

    @Test
    void rawFailureRetainsCauseWithoutExposingItsDiagnostic() {
        IOException cause = new IOException("Internal converter output");
        RenderException error = new RenderException("Internal renderer diagnostic", cause);

        assertSame(cause, error.getCause());
        assertEquals(Optional.empty(), error.localizedMessage());
    }
}
