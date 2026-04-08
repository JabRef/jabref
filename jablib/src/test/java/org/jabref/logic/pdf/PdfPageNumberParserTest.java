package org.jabref.logic.pdf;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfPageNumberParserTest {

    @Test
    void parseFirstPageNumberExtractsFirstNumber() {
        assertEquals(Optional.of(73), PdfPageNumberParser.parseFirstPageNumber("73--96"));
        assertEquals(Optional.of(5), PdfPageNumberParser.parseFirstPageNumber("S5-S8"));
        assertEquals(Optional.empty(), PdfPageNumberParser.parseFirstPageNumber("no-pages"));
    }
}
