package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.mockito.Mockito.mock;

public class PdfContentImporterTestFiles {

    private static final String FILE_ENDING = ".pdf";

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("LNCS-minimal")
                && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testIsRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(new PdfContentImporter(mock(ImportFormatPreferences.class)), fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testImportEntries(String fileName) throws IOException {
        ImporterTestEngine.testImportEntries(new PdfContentImporter(mock(ImportFormatPreferences.class)), fileName, FILE_ENDING);
    }

}
