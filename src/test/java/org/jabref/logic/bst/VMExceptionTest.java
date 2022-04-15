package org.jabref.logic.bst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VMExceptionTest {

    @Test
    void ShouldGetSameErrorMessage() {
        VMException error = new VMException("Something Went Wrong");
        assertEquals("Something Went Wrong", error.getMessage());
    }

    @Test
    void shouldAssertEqualsMessageUsingNewConstructor() {
        VMException error = new VMException("Something Went Wrong", "JAB-768");
        assertEquals("Something Went Wrong", error.getMessage());
    }

    @Test
    void shouldAssertEqualsInternalCodeUsingNewConstructor() {
        VMException error = new VMException("Something Went Wrong", "JAB-768");
        assertEquals("JAB-768", error.getStatusCode());
    }
}
