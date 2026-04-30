package org.jabref.logic.citationstyle;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class CitationStyleLocalTest {

    @Test
    void throwsExceptionWhenFilePathIsNull() {
        assertThrows(NullPointerException.class, () ->
                new CitationStyle(
                        null,               // filePath (should fail)
                        "Title",
                        "ShortTitle",
                        true,
                        true,
                        true,
                        "source",
                        false
                )
        );
    }

    @Test
    void allowsNullTitleWithoutThrowing() {
        assertDoesNotThrow(() ->
                new CitationStyle(
                        "path",
                        null,
                        "ShortTitle",
                        true,
                        true,
                        true,
                        "source",
                        false
                )
        );
    }

    @Test
    void createsCitationStyleWithValidInputs() {
        assertDoesNotThrow(() ->
                new CitationStyle(
                        "path",
                        "Valid Title",
                        "ShortTitle",
                        true,
                        true,
                        true,
                        "source",
                        false
                )
        );
    }
}