package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.logic.importer.Importer;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Assertions;

public class ImporterTestFiles {

    public static void testIsRecognizedFormat(Importer importer, String fileName, String fileType) throws IOException {
        Assertions.assertTrue(importer.isRecognizedFormat(getPath(fileName, fileType), StandardCharsets.UTF_8));
    }

    public static void testImportEntries(Importer importer, String fileName, String fileType) throws IOException {
        List<BibEntry> entries = importer.importDatabase(getPath(fileName, fileType), StandardCharsets.UTF_8).getDatabase()
                .getEntries();
        BibEntryAssert.assertEquals(ImporterTestFiles.class, fileName + ".bib", entries);
    }

    private static Path getPath(String fileName, String fileType) throws IOException {
        try {
            return Paths.get(ImporterTestFiles.class.getResource(fileName + fileType).toURI());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
