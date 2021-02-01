package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Predicate;

import org.jabref.logic.importer.ParserResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WorldcatImporterTest {

    WorldcatImporter importer;

    private String getFilePath(String filename) throws IOException {
        Predicate<String> filePredicate = name -> name.startsWith(filename) && name.endsWith(".xml");
        Collection<String> paths = ImporterTestEngine.getTestFiles(filePredicate);
        if (paths.size() != 1) {
            throw new IllegalArgumentException("Filename returned 0 or more than 1 result: " + filename);
        }
        return paths.iterator().next();
    }

    private String getFileContent(String filename) throws IOException {
        String path = getFilePath(filename);
        return Files.readString(getPath(path));
    }

    private static Path getPath(String fileName) throws IOException {
        try {
            return Path.of(ImporterTestEngine.class.getResource(fileName).toURI());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    @BeforeEach
    public void setUp() {
        importer = new WorldcatImporter();

    }

    @Test
    public void withResultIsRecognizedFormat() throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(importer, getFilePath("WorldcatImporterTestWithResult"));
    }

    @Test
    public void withoutResultIsRecognizedFormat() throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(importer, getFilePath("WorldcatImporterTestWithoutResult"));
    }

    @Test
    public void badXMLIsNotRecognizedFormat() throws IOException {
        boolean isReq = importer.isRecognizedFormat("Nah bruh");
        assertFalse(isReq);
    }

    @Disabled("Will not work without API key")
    @Test
    public void withResultReturnsNonEmptyResult() throws IOException {
        String withResultXML = getFileContent("WorldcatImporterTestWithResult");
        ParserResult res = importer.importDatabase(withResultXML);
        assertTrue(res.getDatabase().getEntries().size() > 0);
    }

    @Disabled("Will not work without API key")
    @Test
    public void withoutResultReturnsEmptyResult() throws IOException {
        String withoutResultXML = getFileContent("WorldcatImporterTestWithResult");
        ParserResult res = importer.importDatabase(withoutResultXML);
        assertEquals(0, res.getDatabase().getEntries().size());
    }

}
