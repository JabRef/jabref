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

    @Test
    void checkNonUtf8WithValidInput() {
        String fullName = "Journal of Physics";
        String abbreviation = "J. Phys.";

        var result = validator.checkNonUtf8(fullName, abbreviation, 1);
        assertTrue(result.isValid());
        assertEquals("", result.getMessage());
        assertEquals("", result.getSuggestion());
    }

    @Test
    void checkNonUtf8WithInvalidInput() {
        // Using a non-UTF8 character
        String fullName = "Journal of Physics\uFFFD";
        String abbreviation = "J. Phys.";

        var result = validator.checkNonUtf8(fullName, abbreviation, 1);
        assertFalse(result.isValid());
        assertEquals("Journal name or abbreviation contains invalid UTF-8 sequences", result.getMessage());
        assertEquals("Ensure all characters are valid UTF-8. Remove or replace any invalid characters.", result.getSuggestion());
    }

    @Test
    void checkStartingLettersWithValidInput() {
        String fullName = "Journal of Physics";
        String abbreviation = "J. Phys.";

        var result = validator.checkStartingLetters(fullName, abbreviation, 1);
        assertTrue(result.isValid());
        assertEquals("", result.getMessage());
        assertEquals("", result.getSuggestion());
    }

    @Test
    void checkStartingLettersWithInvalidInput() {
        String fullName = "Journal of Physics";
        String abbreviation = "Phys. J.";

        var result = validator.checkStartingLetters(fullName, abbreviation, 1);
        assertFalse(result.isValid());
        assertEquals("Abbreviation does not begin with same letter as full journal name", result.getMessage());
        assertEquals("Should start with 'j' (from 'Journal')", result.getSuggestion());
    }

    @Test
    void checkStartingLettersWithThePrefix() {
        String fullName = "The Journal of Physics";
        String abbreviation = "J. Phys.";

        var result = validator.checkStartingLetters(fullName, abbreviation, 1);
        assertTrue(result.isValid());
        assertEquals("", result.getMessage());
        assertEquals("", result.getSuggestion());
    }

    @Test
    void checkStartingLettersWithAPrefix() {
        String fullName = "A Journal of Physics";
        String abbreviation = "J. Phys.";

        var result = validator.checkStartingLetters(fullName, abbreviation, 1);
        assertTrue(result.isValid());
        assertEquals("", result.getMessage());
        assertEquals("", result.getSuggestion());
    }

    @Test
    void checkAbbreviationEqualsFullTextWithValidInput() {
        String fullName = "Journal of Physics";
        String abbreviation = "J. Phys.";

        var result = validator.checkAbbreviationEqualsFullText(fullName, abbreviation, 1);
        assertTrue(result.isValid());
        assertEquals("", result.getMessage());
        assertEquals("", result.getSuggestion());
    }

    @Test
    void checkAbbreviationEqualsFullTextWithInvalidInput() {
        String fullName = "Quantum";
        String abbreviation = "Quantum";

        var result = validator.checkAbbreviationEqualsFullText(fullName, abbreviation, 1);
        assertFalse(result.isValid());
        assertEquals("Abbreviation is the same as the full text", result.getMessage());
        assertEquals("Consider using a shorter abbreviation to distinguish it from the full name", result.getSuggestion());
    }

    @Test
    void checkOutdatedManagementAbbreviationWithValidInput() {
        String fullName = "Management Science";
        String abbreviation = "Manag. Sci.";

        var result = validator.checkOutdatedManagementAbbreviation(fullName, abbreviation, 1);
        assertTrue(result.isValid());
        assertEquals("", result.getMessage());
        assertEquals("", result.getSuggestion());
    }

    @Test
    void checkOutdatedManagementAbbreviationWithInvalidInput() {
        String fullName = "Management Science";
        String abbreviation = "Manage. Sci.";

        var result = validator.checkOutdatedManagementAbbreviation(fullName, abbreviation, 1);
        assertFalse(result.isValid());
        assertEquals("Management is abbreviated with outdated \"Manage.\" instead of \"Manag.\"", result.getMessage());
        assertEquals("Update to use the standard abbreviation \"Manag.\"", result.getSuggestion());
    }

    @Test
    void checkDuplicateFullNames() {
        // Add first entry
        validator.validate("Journal of Physics", "J. Phys.", 1);
        // Add duplicate with different abbreviation
        validator.validate("Journal of Physics", "J. Phys. A", 2);

        var issues = validator.getIssues();
        assertFalse(issues.isEmpty());
        assertTrue(issues.stream()
                         .anyMatch(issue -> issue.getMessage().contains("Duplicate full name")));
        assertTrue(issues.stream()
                         .anyMatch(issue -> issue.getSuggestion().contains("consolidating abbreviations")));
    }

    @Test
    void checkDuplicateAbbreviations() {
        // Add first entry
        validator.validate("Journal of Physics", "J. Phys.", 1);
        // Add different journal with same abbreviation
        validator.validate("Journal of Physiology", "J. Phys.", 2);

        var issues = validator.getIssues();
        assertFalse(issues.isEmpty());
        assertTrue(issues.stream()
                         .anyMatch(issue -> issue.getMessage().contains("Duplicate abbreviation")));
        assertTrue(issues.stream()
                         .anyMatch(issue -> issue.getSuggestion().contains("more specific abbreviations")));
    }

    @Test
    void checkWrongEscapeWithMultipleValidEscapes() {
        String fullName = "Journal with \\n\\t\\r\\b\\f\\\"\\\\";
        String abbreviation = "J. with \\n\\t\\r\\b\\f\\\"\\\\";

        var result = validator.checkWrongEscape(fullName, abbreviation, 1);
        assertTrue(result.isValid());
        assertEquals("", result.getMessage());
        assertEquals("", result.getSuggestion());
    }

    @Test
    void checkWrongEscapeWithUnicodeEscape() {
        String fullName = "Journal with \\u0041";
        String abbreviation = "J. with \\u0042";

        var result = validator.checkWrongEscape(fullName, abbreviation, 1);
        assertTrue(result.isValid());
        assertEquals("", result.getMessage());
        assertEquals("", result.getSuggestion());
    }

    @Test
    void checkWrongEscapeWithInvalidUnicodeEscape() {
        String fullName = "Journal with \\u004";
        String abbreviation = "J. Phys.";

        var result = validator.checkWrongEscape(fullName, abbreviation, 1);
        assertFalse(result.isValid());
        assertEquals("Invalid escape sequence in full name at position 13", result.getMessage());
        assertTrue(result.getSuggestion().contains("valid escape sequences"));
    }

    @Test
    void checkNonUtf8WithMultipleInvalidCharacters() {
        String fullName = "Journal of Physics\uFFFD\uFFFD";
        String abbreviation = "J. Phys.\uFFFD";

        var result = validator.checkNonUtf8(fullName, abbreviation, 1);
        assertFalse(result.isValid());
        assertEquals("Journal name or abbreviation contains invalid UTF-8 sequences", result.getMessage());
        assertTrue(result.getSuggestion().contains("valid UTF-8"));
    }

    @Test
    void checkStartingLettersWithMultiplePrefixes() {
        String fullName = "The A An Journal of Physics";
        String abbreviation = "J. Phys.";

        var result = validator.checkStartingLetters(fullName, abbreviation, 1);
        assertTrue(result.isValid());
        assertEquals("", result.getMessage());
        assertEquals("", result.getSuggestion());
    }

    @Test
    void checkStartingLettersWithSpecialCharacters() {
        String fullName = "The Journal of Physics & Chemistry";
        String abbreviation = "J. Phys. Chem.";

        var result = validator.checkStartingLetters(fullName, abbreviation, 1);
        assertTrue(result.isValid());
        assertEquals("", result.getMessage());
        assertEquals("", result.getSuggestion());
    }

    @Test
    void checkStartingLettersWithNumbers() {
        String fullName = "2D Materials";
        String abbreviation = "2D Mater.";

        var result = validator.checkStartingLetters(fullName, abbreviation, 1);
        assertTrue(result.isValid());
        assertEquals("", result.getMessage());
        assertEquals("", result.getSuggestion());
    }

    @Test
    void validateWithEmptyInputs() {
        var results = validator.validate("", "", 1);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> !r.isValid()));
    }

    @Test
    void validateWithWhitespaceOnly() {
        var results = validator.validate("   ", "   ", 1);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> !r.isValid()));
    }

    @Test
    void checkDuplicateFullNamesWithCaseInsensitivity() {
        validator.validate("Journal of Physics", "J. Phys.", 1);
        validator.validate("JOURNAL OF PHYSICS", "J. Phys.", 2);

        var issues = validator.getIssues();
        assertFalse(issues.isEmpty());
        assertTrue(issues.stream()
                         .anyMatch(issue -> issue.getMessage().contains("Duplicate full name")));
    }

    @Test
    void checkDuplicateAbbreviationsWithCaseInsensitivity() {
        validator.validate("Journal of Physics", "J. Phys.", 1);
        validator.validate("Journal of Physiology", "j. phys.", 2);

        var issues = validator.getIssues();
        assertFalse(issues.isEmpty());
        assertTrue(issues.stream()
                         .anyMatch(issue -> issue.getMessage().contains("Duplicate abbreviation")));
    }

    @Test
    void checkAbbreviationEqualsFullTextWithSpecialCharacters() {
        String fullName = "Physics & Chemistry";
        String abbreviation = "Physics & Chemistry";

        var result = validator.checkAbbreviationEqualsFullText(fullName, abbreviation, 1);
        assertFalse(result.isValid());
        assertEquals("Abbreviation is the same as the full text", result.getMessage());
        assertTrue(result.getSuggestion().contains("shorter abbreviation"));
    }

    @Test
    void checkOutdatedManagementAbbreviationWithVariations() {
        String[] invalidAbbreviations = {
                "Manage. Sci.",
                "Manage Sci.",
                "Manage.Sci.",
                "Manage. Sci"
        };

        for (String abbreviation : invalidAbbreviations) {
            var result = validator.checkOutdatedManagementAbbreviation("Management Science", abbreviation, 1);
            assertFalse(result.isValid());
            assertTrue(result.getMessage().contains("outdated"));
            assertTrue(result.getSuggestion().contains("Manag."));
        }
    }

    @Test
    void validateWithVeryLongInputs() {
        String longName = "A".repeat(1000);
        String longAbbr = "B".repeat(1000);

        var results = validator.validate(longName, longAbbr, 1);
        assertFalse(results.isEmpty());
        // Should still perform all validations without throwing exceptions
        assertEquals(5, results.size());
    }
}
