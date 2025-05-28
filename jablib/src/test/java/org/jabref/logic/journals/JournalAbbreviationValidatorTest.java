package org.jabref.logic.journals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalAbbreviationValidatorTest {

    private JournalAbbreviationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new JournalAbbreviationValidator();
    }

    @Test
    void checkWrongEscapeWithInvalidFullName() {
        String fullName = "Zeszyty Naukowe Wy\\";
        String abbreviation = "Problemy Mat.";

        var result = validator.checkWrongEscape(fullName, abbreviation, 1);
        assertFalse(result.isValid());
        assertEquals("Invalid escape sequence in full name at position 15", result.getMessage());
        assertEquals("Use valid escape sequences: \\\\, \\\", \\n, \\t, \\r, \\b, \\f, \\uXXXX", result.getSuggestion());
        assertEquals(1, result.getLineNumber());
    }

    @Test
    void checkWrongEscapeWithInvalidAbbreviation() {
        String fullName = "Journal of Evolutionary Biochemistry and Physiology";
        String abbreviation = "J. Evol. Biochem. Physiol.\\";

        var result = validator.checkWrongEscape(fullName, abbreviation, 2);
        assertFalse(result.isValid());
        assertEquals("Invalid escape sequence in abbreviation at position 22", result.getMessage());
        assertEquals("Use valid escape sequences: \\\\, \\\", \\n, \\t, \\r, \\b, \\f, \\uXXXX", result.getSuggestion());
        assertEquals(2, result.getLineNumber());
    }

    @Test
    void checkWrongEscapeWithValidEscapes() {
        String fullName = "Journal with \\n newline and \\t tab";
        String abbreviation = "J. with \\r return and \\b backspace";

        var result = validator.checkWrongEscape(fullName, abbreviation, 1);
        assertTrue(result.isValid());
        assertEquals("", result.getMessage());
        assertEquals("", result.getSuggestion());
    }

    @Test
    void checkWrongEscapeWithMultipleIssues() {
        String fullName = "Journal with \\x invalid";
        String abbreviation = "J. with \\y invalid";

        var result = validator.checkWrongEscape(fullName, abbreviation, 1);
        assertFalse(result.isValid());
        assertTrue(result.getMessage().startsWith("Invalid escape sequence"));
        assertTrue(result.getSuggestion().contains("valid escape sequences"));
    }
}
