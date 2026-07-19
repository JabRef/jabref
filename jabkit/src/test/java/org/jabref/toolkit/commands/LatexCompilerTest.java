package org.jabref.toolkit.commands;

import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatexCompilerTest {

    @Test
    void parsesSinglePage() {
        // pdflatex prints "page" (singular) for a one-page document
        assertEquals(OptionalInt.of(1),
                LatexCompiler.parsePageCount("Output written on paper.pdf (1 page, 54321 bytes)."));
    }

    @Test
    void parsesMultiplePages() {
        assertEquals(OptionalInt.of(7),
                LatexCompiler.parsePageCount("Output written on ieee-paper.pdf (7 pages, 123456 bytes)."));
    }

    @Test
    void takesLastOccurrenceAcrossReruns() {
        String log = """
                Output written on paper.pdf (8 pages, 1000 bytes).
                ... rerun to get cross-references right ...
                Output written on paper.pdf (7 pages, 990 bytes).
                """;
        assertEquals(OptionalInt.of(7), LatexCompiler.parsePageCount(log));
    }

    @Test
    void returnsEmptyWhenCompilationProducedNoPdf() {
        assertEquals(OptionalInt.empty(),
                LatexCompiler.parsePageCount("! LaTeX Error: File `IEEEtran.cls' not found."));
    }
}
