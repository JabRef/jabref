package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Paths are written with forward slashes throughout so that the expectations hold on every OS:
/// `Path#toString` renders them with the platform separator, and the expected command lines are
/// built from the very same `Path` objects.
class ExternalPdfReadersTest {

    private static final Path PDF_FILE = Path.of("C:/library/paper.pdf");
    private static final int PAGE = 7;

    private static final Path SUMATRA = Path.of("C:/Program Files/SumatraPDF/SumatraPDF.exe");

    private static Stream<Arguments> knownReaders() {
        return Stream.of(
                Arguments.of(SUMATRA, List.of("-reuse-instance", "-page", "7")),
                Arguments.of(Path.of("C:/Program Files/Adobe/Acrobat Reader DC/Reader/AcroRd32.exe"),
                        List.of("/A", "page=7")),
                Arguments.of(Path.of("C:/Program Files/Adobe/Acrobat DC/Acrobat/Acrobat.exe"),
                        List.of("/A", "page=7")),
                Arguments.of(Path.of("C:/Program Files/Foxit Software/Foxit PDF Reader/FoxitPDFReader.exe"),
                        List.of("/A", "page=7")),
                Arguments.of(Path.of("C:/Program Files/Tracker Software/PDF Viewer/PDFXCview.exe"),
                        List.of("/A", "page=7")),
                Arguments.of(Path.of("/usr/bin/okular"), List.of("-p", "7")),
                Arguments.of(Path.of("/usr/bin/evince"), List.of("-i", "7")),
                Arguments.of(Path.of("/usr/bin/zathura"), List.of("-P", "7")));
    }

    // [utest->feat~entry-editor.file-annotations.open-at-annotation~1]
    @ParameterizedTest
    @MethodSource("knownReaders")
    void knownReaderBuildsCommandLineWithPage(Path executable, List<String> expectedArguments) {
        List<String> expected = Stream.of(Stream.of(executable.toString()), expectedArguments.stream(), Stream.of(PDF_FILE.toString()))
                                      .flatMap(stream -> stream)
                                      .toList();

        assertEquals(expected, readerFor(executable).getCommandLine(executable, PDF_FILE, PAGE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"okular", "OKULAR", "Okular.exe", "/usr/local/bin/okular", "/opt/My Reader/okular"})
    void executableIsMatchedRegardlessOfCaseDirectoryAndSuffix(String executable) {
        assertEquals("Okular", readerFor(Path.of(executable)).getDisplayName());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "C:/Program Files/Microsoft/Edge/Application/msedge.exe",
            "/usr/bin/xdg-open",
            "notepad.exe",
            "sumatra",
            "sumatrapdf.com"})
    void unknownExecutableGetsNoPageArguments(String executable) {
        assertEquals(Optional.empty(), ExternalPdfReaders.getReader(Path.of(executable)));
    }

    /// The 2017 attempt joined the arguments into one string and split it on spaces again, which
    /// broke every reader installed below `C:\Program Files`.
    @Test
    void pathsContainingSpacesStayOneArgumentEach() {
        Path pdfFile = Path.of("C:/My Library/some paper.pdf");

        List<String> commandLine = readerFor(SUMATRA).getCommandLine(SUMATRA, pdfFile, 3);

        assertEquals(List.of(SUMATRA.toString(), "-reuse-instance", "-page", "3", pdfFile.toString()), commandLine);
    }

    private static ExternalPdfReader readerFor(Path executable) {
        return ExternalPdfReaders.getReader(executable).orElseThrow();
    }
}
