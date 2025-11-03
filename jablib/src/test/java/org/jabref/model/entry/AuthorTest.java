package org.jabref.model.entry;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorTest {

    @ParameterizedTest
    @CsvSource({
            "AO, 'A. O.'",
            "AO., 'A. O.'",
            "A.O., 'A. O.'",
            "A-O, 'A.-O.'"
    })
    void addDotIfAbbreviationAddsDot(String input, String expected) {
        assertEquals(expected, Author.addDotIfAbbreviation(input));
    }

    @ParameterizedTest
    @CsvSource({
            "'A O', 'A. O.'",
            "'A-melia', 'A.-melia'",
            "'AmeliA', 'AmeliA'",
            "'Ameli A', 'Ameli A.'",
            "'Ameli ', 'Ameli'",
            "'Ameli AA', 'Ameli A. A.'"
    })
    void addDotIfAbbreviationEdgeCases(String input, String expected) {
        assertEquals(expected, Author.addDotIfAbbreviation(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"O.", "A. O.", "A.-O.",
            "O. Moore", "A. O. Moore", "O. von Moore", "A.-O. Moore",
            "Moore, O.", "Moore, O., Jr.", "Moore, A. O.", "Moore, A.-O.",
            "MEmre", "{\\'{E}}douard", "J{\\\"o}rg", "Moore, O. and O. Moore",
            "Moore, O. and O. Moore and Moore, O. O."})
    void addDotIfAbbreviationDoNotAddDot(String input) {
        assertEquals(input, Author.addDotIfAbbreviation(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            // Lower-case letters
            "asdf", "a",
            // Numbers
            "1", "1 23"
    })
    void addDotIfAbbreviation(String input) {
        assertEquals(input, Author.addDotIfAbbreviation(input));
    }

    @Test
    void bracesKept() {
        assertEquals(Optional.of("{Company Name, LLC}"),
                new Author("", "", null, "{Company Name, LLC}", null).getFamilyName());
    }
}
