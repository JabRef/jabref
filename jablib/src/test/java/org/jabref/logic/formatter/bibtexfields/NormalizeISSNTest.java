package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NormalizeISSNTest {

    private final NormalizeIssn formatISSN = new NormalizeIssn();

    @ParameterizedTest
    @CsvSource({
            "0123-4567, 0123-4567",
            "01234567, 0123-4567",
            "Banana, Banana",
            "'',''"
    })
    void issnFormatting(String input, String expected) {
        assertEquals(expected, formatISSN.format(input));
    }
}
