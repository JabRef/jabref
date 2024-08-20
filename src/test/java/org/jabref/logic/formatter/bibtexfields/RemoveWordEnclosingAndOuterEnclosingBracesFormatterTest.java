package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoveWordEnclosingAndOuterEnclosingBracesFormatterTest {
    private final RemoveWordEnclosingAndOuterEnclosingBracesFormatter formatter = new RemoveWordEnclosingAndOuterEnclosingBracesFormatter();

    @ParameterizedTest
    @CsvSource({
            "A test B, {A} test {B}",
            "A and B, {{A} and {B}}",
            "{w}ord word wor{d}, {w}ord word wor{d}",
            "{w}ord word word, {{w}ord word word}",
            "{w}ord word wor{d}, {w}ord word {wor{d}}",
            "Vall{\\'e}e Poussin, {Vall{\\'e}e} {Poussin}",
            "Vall{\\'e}e Poussin, {Vall{\\'e}e Poussin}",
            "Vall{\\'e}e Poussin, Vall{\\'e}e Poussin"
    })
    void format(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    @Test
    void formatExample() {
        assertEquals("In CDMA", formatter.format(formatter.getExampleInput()));
    }
}
