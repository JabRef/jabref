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

class ExternalPdfReadersTest {

    private static final Path PDF_FILE = Path.of("C:/library/paper.pdf");
    private static final int PAGE = 7;

    private static final Path SUMATRA = Path.of("C:/Program Files/SumatraPDF/SumatraPDF.exe");
    private static final Path ACROBAT_READER = Path.of("C:/Program Files/Adobe/Acrobat Reader DC/Reader/AcroRd32.exe");
    private static final Path ACROBAT = Path.of("C:/Program Files/Adobe/Acrobat DC/Acrobat/Acrobat.exe");
    private static final Path FOXIT = Path.of("C:/Program Files/Foxit Software/Foxit PDF Reader/FoxitPDFReader.exe");
    private static final Path PDF_XCHANGE = Path.of("C:/Program Files/Tracker Software/PDF Viewer/PDFXCview.exe");
    private static final Path OKULAR = Path.of("/usr/bin/okular");
    private static final Path EVINCE = Path.of("/usr/bin/evince");
    private static final Path ZATHURA = Path.of("/usr/bin/zathura");

    private static Stream<Arguments> knownReaders() {
        return Stream.of(
                Arguments.of(SUMATRA, List.of("-reuse-instance", "-page", "7")),
                Arguments.of(ACROBAT_READER, List.of("/A", "page=7")),
                Arguments.of(ACROBAT, List.of("/A", "page=7")),
                Arguments.of(FOXIT, List.of("/A", "page=7")),
                Arguments.of(PDF_XCHANGE, List.of("/A", "page=7")),
                Arguments.of(OKULAR, List.of("-p", "7")),
                Arguments.of(EVINCE, List.of("-i", "7")),
                Arguments.of(ZATHURA, List.of("-P", "7")));
    }

    // [utest->feat~entry-editor.file-annotations.open-at-annotation~1]
    @ParameterizedTest
    @MethodSource("knownReaders")
    void knownReaderBuildsCommandLineWithPage(Path executable, List<String> expectedArguments) {
        assertEquals(commandLine(executable, expectedArguments),
                readerFor(executable).getCommandLine(executable, PDF_FILE, PAGE));
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

    @Test
    void pathsContainingSpacesStayOneArgumentEach() {
        Path pdfFile = Path.of("C:/My Library/some paper.pdf");

        List<String> commandLine = readerFor(SUMATRA).getCommandLine(SUMATRA, pdfFile, 3);

        assertEquals(List.of(SUMATRA.toString(), "-reuse-instance", "-page", "3", pdfFile.toString()), commandLine);
    }

    private static ExternalPdfReader readerFor(Path executable) {
        return ExternalPdfReaders.getReader(executable).orElseThrow();
    }

    private static List<String> commandLine(Path executable, List<String> arguments) {
        return Stream.of(Stream.of(executable.toString()), arguments.stream(), Stream.of(PDF_FILE.toString()))
                     .flatMap(stream -> stream)
                     .toList();
    }
}
