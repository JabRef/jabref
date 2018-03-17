package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImporterTestEngine {

    private static final String TEST_RESOURCES = "src/test/resources/org/jabref/logic/importer/fileformat";

    /**
     * @param fileNamePredicate A predicate that describes the files which contain tests
     * @return A collection with the names of files in the test folder
     * @throws IOException if there is a problem when trying to read the files in the file system
     */
    public static Collection<String> getTestFiles(Predicate<String> fileNamePredicate) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(TEST_RESOURCES))) {
            return stream
                    .map(path -> path.getFileName().toString())
                    .filter(fileNamePredicate)
                    .collect(Collectors.toList());
        }
    }

    public static void testIsRecognizedFormat(Importer importer, String fileName) throws IOException {
        Assertions.assertTrue(importer.isRecognizedFormat(getPath(fileName), StandardCharsets.UTF_8));
    }

    public static void testIsNotRecognizedFormat(Importer importer, String fileName) throws IOException {
        Assertions.assertFalse(importer.isRecognizedFormat(getPath(fileName), StandardCharsets.UTF_8));
    }

    public static void testImportEntries(Importer importer, String fileName, String fileType) throws IOException, ImportException {
        ParserResult parserResult = importer.importDatabase(getPath(fileName), StandardCharsets.UTF_8);
        if (parserResult.isInvalid()) {
            throw new ImportException(parserResult.getErrorMessage());
        }
        List<BibEntry> entries = parserResult.getDatabase()
                                             .getEntries();
        BibEntryAssert.assertEquals(ImporterTestEngine.class, fileName.replaceAll(fileType, ".bib"), entries);
    }

    private static Path getPath(String fileName) throws IOException {
        try {
            return Paths.get(ImporterTestEngine.class.getResource(fileName).toURI());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    public static void testImportMalformedFiles(Importer importer, String fileName) throws IOException {
        List<BibEntry> entries = importer.importDatabase(getPath(fileName), StandardCharsets.UTF_8).getDatabase()
                .getEntries();
        assertEquals(entries, new ArrayList<BibEntry>());
    }
}
