package org.jabref.logic.icore;

import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.jabref.logic.icore.ConferenceAcronymExtractor.extract;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConferenceAcronymExtractorTest {
    @ParameterizedTest(name = "Extract from \"{0}\" should return \"{1}\"")
    @CsvSource({
        "(SERA), SERA",                                                     // Simple acronym extraction
        "Extract conference from full title (SERA), SERA",                  // Acronym in a conference title
        "This (SERA) has multiple (CONF) acronyms, SERA",                   // Extract only the first acronym encountered
        "This (C++SER@-20_26) has special characters, C++SER@-20_26",       // Special characters in acronym
        "Input with whitespace (     ACR     )', ACR",                      // Extract strips whitespace around acronym
        "(Nested (parentheses (SERA))), SERA",                              // Extract the acronym in the deepest parentheses
        "This open paren ((SERA) is never closed, SERA",                    // Extract acronym from incomplete parentheses
        "Input with empty () parentheses, ",                                // Empty acronym inside parentheses
        "Input with empty (        ) whitespace in parens, ",               // Only whitespace inside parentheses
        "'',"                                                               // Empty string
    })
    void acronymExtraction(String input, String expectedResult) {
        assertEquals(Optional.ofNullable(expectedResult), extract(input));
    }
}
