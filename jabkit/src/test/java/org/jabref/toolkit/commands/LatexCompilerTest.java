package org.jabref.toolkit.commands;

import java.util.OptionalInt;

import org.jabref.toolkit.exception.CliException;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LatexCompilerTest {

    private static final String PAGE_LINE = "Output written on paper.pdf (2 pages, 100 bytes).";

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

    @Test
    void zeroExitWithPageCountReturnsCount() throws CliException {
        assertEquals(2, LatexCompiler.pageCountOrFail(0, PAGE_LINE, "paper.tex"));
    }

    @Test
    void nonZeroExitFailsEvenWhenAPageCountWasWritten() {
        // pdflatex writes a (broken) PDF in nonstopmode, so a page count alone must not count as success.
        CliException exception = assertThrows(CliException.class,
                () -> LatexCompiler.pageCountOrFail(1, PAGE_LINE, "paper.tex"));
        assertEquals(CommandLine.ExitCode.SOFTWARE, exception.getExitCode());
    }

    @Test
    void zeroExitWithoutPageCountFails() {
        assertThrows(CliException.class,
                () -> LatexCompiler.pageCountOrFail(0, "! Undefined control sequence.", "paper.tex"));
    }
}
