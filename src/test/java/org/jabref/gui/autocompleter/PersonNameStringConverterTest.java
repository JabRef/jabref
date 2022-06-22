package org.jabref.gui.autocompleter;

import java.util.Collections;

import org.jabref.model.entry.Author;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PersonNameStringConverterTest {

    /** The author. **/
    private Author author;

    @BeforeEach
    void setUp() {
        // set up auhtor's name
        author = new Author("Joseph M.", "J. M.", "", "Reagle", "Jr.");
    }

    @ParameterizedTest(name = "autoCompFF={0}, autoCompLF={1}, autoCompleteFirstNameMode={2}, expectedResult={3}")
    @CsvSource({
            "TRUE, TRUE, ONLY_FULL, 'Reagle, Jr., Joseph M.'",
            "TRUE, FALSE, ONLY_FULL, 'Joseph M. Reagle, Jr.'",
            "FALSE, TRUE, ONLY_FULL, 'Reagle, Jr., Joseph M.'",
            "FALSE, FALSE, ONLY_FULL, 'Reagle'",

            "TRUE, TRUE, ONLY_ABBREVIATED, 'Reagle, Jr., J. M.'",
            "TRUE, FALSE, ONLY_ABBREVIATED, 'J. M. Reagle, Jr.'",
            "FALSE, TRUE, ONLY_ABBREVIATED, 'Reagle, Jr., J. M.'",
            "FALSE, FALSE, ONLY_ABBREVIATED, 'Reagle'",

            "TRUE, TRUE, BOTH, 'Reagle, Jr., J. M.'",
            "TRUE, FALSE, BOTH, 'J. M. Reagle, Jr.'",
            "FALSE, TRUE, BOTH, 'Reagle, Jr., J. M.'",
            "FALSE, FALSE, BOTH, 'Reagle'"
    })
    void testToStringWithoutAutoCompletePreferences(boolean autoCompFF, boolean autoCompLF, AutoCompleteFirstNameMode autoCompleteFirstNameMode, String expectedResult) {
        PersonNameStringConverter converter = new PersonNameStringConverter(autoCompFF, autoCompLF, autoCompleteFirstNameMode);
        String formattedStr = converter.toString(author);
        assertEquals(expectedResult, formattedStr);
    }

    @ParameterizedTest(name = "shouldAutoComplete={0}, firstNameMode={1}, nameFormat={2}, expectedResult={3}")
    @CsvSource({
            "TRUE, ONLY_FULL, LAST_FIRST, 'Reagle, Jr., Joseph M.'",
            "TRUE, ONLY_ABBREVIATED, LAST_FIRST, 'Reagle, Jr., J. M.'",
            "TRUE, BOTH, LAST_FIRST, 'Reagle, Jr., J. M.'",

            "TRUE, ONLY_FULL, FIRST_LAST, 'Joseph M. Reagle, Jr.'",
            "TRUE, ONLY_ABBREVIATED, FIRST_LAST, 'J. M. Reagle, Jr.'",
            "TRUE, BOTH, FIRST_LAST, 'J. M. Reagle, Jr.'",

            "TRUE, ONLY_FULL, BOTH, 'Reagle, Jr., Joseph M.'",
            "TRUE, ONLY_ABBREVIATED, BOTH, 'Reagle, Jr., J. M.'",
            "TRUE, BOTH, BOTH, 'Reagle, Jr., J. M.'"
    })
    void testToStringWithAutoCompletePreferences(boolean shouldAutoComplete,
                                                 AutoCompleteFirstNameMode firstNameMode,
                                                 AutoCompletePreferences.NameFormat nameFormat,
                                                 String expectedResult) {
        AutoCompletePreferences preferences = new AutoCompletePreferences(
                shouldAutoComplete,
                firstNameMode,
                nameFormat,
                Collections.emptySet());
        PersonNameStringConverter converter = new PersonNameStringConverter(preferences);
        String formattedStr = converter.toString(author);
        assertEquals(expectedResult, formattedStr);
    }
}
