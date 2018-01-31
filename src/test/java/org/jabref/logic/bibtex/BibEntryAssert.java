package org.jabref.logic.bibtex;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;

public class BibEntryAssert {

    /**
     * Reads a single entry from the resource using `getResourceAsStream` from the given class. The resource has to
     * contain a single entry
     *
     * @param clazz the class where to call `getResourceAsStream`
     * @param resourceName the resource to read
     * @param entry the entry to compare with
     */
    public static void assertEquals(Class<?> clazz, String resourceName, BibEntry entry)
            throws IOException {
        assertNotNull(clazz);
        assertNotNull(resourceName);
        assertNotNull(entry);
        try (InputStream shouldBeIs = clazz.getResourceAsStream(resourceName)) {
            BibEntryAssert.assertEquals(shouldBeIs, entry);
        }
    }

    /**
     * Reads a single entry from the resource using `getResourceAsStream` from the given class. The resource has to
     * contain a single entry
     *
     * @param clazz the class where to call `getResourceAsStream`
     * @param resourceName the resource to read
     * @param asIsEntries a list containing a single entry to compare with
     */
    public static void assertEquals(Class<?> clazz, String resourceName, List<BibEntry> asIsEntries)
            throws IOException {
        assertNotNull(clazz);
        assertNotNull(resourceName);
        assertNotNull(asIsEntries);
        try (InputStream shouldBeIs = clazz.getResourceAsStream(resourceName)) {
            BibEntryAssert.assertEquals(shouldBeIs, asIsEntries);
        }
    }

    private static List<BibEntry> getListFromInputStream(InputStream is) throws IOException {
        ParserResult result;
        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            BibtexParser parser = new BibtexParser(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
            result = parser.parse(reader);
        }
        assertNotNull(result);
        assertFalse(result.isEmpty());
        return result.getDatabase().getEntries();
    }

    /**
     * Reads a bibtex database from the given InputStream. The list is compared with the given list.
     *
     * @param expectedInputStream the inputStream reading the entry from
     * @param actualEntries a list containing a single entry to compare with
     */
    public static void assertEquals(InputStream expectedInputStream, List<BibEntry> actualEntries)
            throws IOException {
        assertNotNull(expectedInputStream);
        assertNotNull(actualEntries);
        // explicit reference of Assertions is needed here to disambiguate from the methods defined by this class
        Assertions.assertEquals(getListFromInputStream(expectedInputStream), actualEntries);
    }

    public static void assertEquals(List<BibEntry> expectedEntries, InputStream actualInputStream)
            throws IOException {
        assertNotNull(actualInputStream);
        assertNotNull(expectedEntries);
        // explicit reference of Assertions is needed here to disambiguate from the methods defined by this class
        Assertions.assertEquals(expectedEntries, getListFromInputStream(actualInputStream));
    }

    /**
     * Reads a bibtex database from the given InputStream. The result has to contain a single BibEntry. This entry is
     * compared to the given entry
     *
     * @param expected the inputStream reading the entry from
     * @param actual the entry to compare with
     */
    public static void assertEquals(InputStream expected, BibEntry actual)
            throws IOException {
        assertEquals(expected, Collections.singletonList(actual));
    }

    /**
     * Compares two InputStreams. For each InputStream a list will be created. expectedIs is read directly, actualIs is filtered through importer to convert to a list of BibEntries.
     * @param expectedIs A BibtexImporter InputStream.
     * @param fileToImport The path to the file to be imported.
     * @param importer The fileformat you want to use to read the passed file to get the list of expected BibEntries
     * @throws IOException
     */
    public static void assertEquals(InputStream expectedIs, Path fileToImport, Importer importer)
            throws IOException {
        assertEquals(getListFromInputStream(expectedIs), fileToImport, importer);
    }

    public static void assertEquals(InputStream expectedIs, URL fileToImport, Importer importer)
            throws URISyntaxException, IOException {
        assertEquals(expectedIs, Paths.get(fileToImport.toURI()), importer);
    }

    /**
     * Compares a list of BibEntries to an InputStream. actualIs is filtered through importerForActualIs to convert to a list of BibEntries.
     * @param expected A BibtexImporter InputStream.
     * @param fileToImport The path to the file to be imported.
     * @param importer The fileformat you want to use to read the passed file to get the list of expected BibEntries
     * @throws IOException
     */
    public static void assertEquals(List<BibEntry> expected, Path fileToImport, Importer importer)
            throws IOException {
        List<BibEntry> actualEntries = importer.importDatabase(fileToImport, StandardCharsets.UTF_8)
                .getDatabase().getEntries();
        // explicit reference of Assertions is needed here to disambiguate from the methods defined by this class
        Assertions.assertEquals(expected, actualEntries);
    }

    public static void assertEquals(List<BibEntry> expected, URL fileToImport, Importer importer)
            throws URISyntaxException, IOException {
        assertEquals(expected, Paths.get(fileToImport.toURI()), importer);
    }
}
