package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoveWordEnclosingAndOuterEnclosingBracesFormatterTest {
    private final RemoveWordEnclosingAndOuterEnclosingBracesFormatter formatter = new RemoveWordEnclosingAndOuterEnclosingBracesFormatter();

    @ParameterizedTest
    @CsvSource({
            "Vall{\\'e}e Poussin, {Vall{\\'e}e} {Poussin}",
            "Vall{\\'e}e Poussin, {Vall{\\'e}e Poussin}",
            "Vall{\\'e}e Poussin, Vall{\\'e}e Poussin"
    })
    public void format(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    @Test
    public void formatExample() {
        assertEquals("In CDMA", formatter.format(formatter.getExampleInput()));
    }
}
