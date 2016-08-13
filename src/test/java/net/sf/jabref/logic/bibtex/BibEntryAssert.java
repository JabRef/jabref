package net.sf.jabref.logic.bibtex;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.importer.fileformat.ImportFormat;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;

public class BibEntryAssert {

    /**
     * Reads a single entry from the resource using `getResourceAsStream` from the given class. The resource has to
     * contain a single entry
     *
     * @param clazz the class where to call `getResourceAsStream`
     * @param resourceName the resource to read
     * @param entry the entry to compare with
     */
    public static void assertEquals(Class<?> clazz, String resourceName, BibEntry entry,
            ImportFormatPreferences importFormatPreferences)
            throws IOException {
        Assert.assertNotNull(clazz);
        Assert.assertNotNull(resourceName);
        Assert.assertNotNull(entry);
        try (InputStream shouldBeIs = clazz.getResourceAsStream(resourceName)) {
            BibEntryAssert.assertEquals(shouldBeIs, entry, importFormatPreferences);
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
    public static void assertEquals(Class<?> clazz, String resourceName, List<BibEntry> asIsEntries,
            ImportFormatPreferences importFormatPreferences)
            throws IOException {
        Assert.assertNotNull(clazz);
        Assert.assertNotNull(resourceName);
        Assert.assertNotNull(asIsEntries);
        try (InputStream shouldBeIs = clazz.getResourceAsStream(resourceName)) {
            BibEntryAssert.assertEquals(shouldBeIs, asIsEntries, importFormatPreferences);
        }
    }

    private static List<BibEntry> getListFromInputStream(InputStream is,
            ImportFormatPreferences importFormatPreferences) throws IOException {
        ParserResult result;
        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            BibtexParser parser = new BibtexParser(reader, importFormatPreferences);
            result = parser.parse();
        }
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isNullResult());
        return result.getDatabase().getEntries();
    }

    /**
     * Reads a bibtex database from the given InputStream. The list is compared with the given list.
     *
     * @param expectedInputStream the inputStream reading the entry from
     * @param actualEntries a list containing a single entry to compare with
     */
    public static void assertEquals(InputStream expectedInputStream, List<BibEntry> actualEntries,
            ImportFormatPreferences importFormatPreferences)
            throws IOException {
        Assert.assertNotNull(expectedInputStream);
        Assert.assertNotNull(actualEntries);
        Assert.assertEquals(getListFromInputStream(expectedInputStream, importFormatPreferences), actualEntries);
    }

    public static void assertEquals(List<BibEntry> expectedEntries, InputStream actualInputStream,
            ImportFormatPreferences importFormatPreferences)
            throws IOException {
        Assert.assertNotNull(actualInputStream);
        Assert.assertNotNull(expectedEntries);
        Assert.assertEquals(expectedEntries, getListFromInputStream(actualInputStream, importFormatPreferences));
    }

    /**
     * Reads a bibtex database from the given InputStream. The result has to contain a single BibEntry. This entry is
     * compared to the given entry
     *
     * @param expected the inputStream reading the entry from
     * @param actual the entry to compare with
     */
    public static void assertEquals(InputStream expected, BibEntry actual,
            ImportFormatPreferences importFormatPreferences)
            throws IOException {
        assertEquals(expected, Collections.singletonList(actual), importFormatPreferences);
    }

    /**
     * Compares two InputStreams. For each InputStream a list will be created. expectedIs is read directly, actualIs is filtered through importFormat to convert to a list of BibEntries.
     * @param expectedIs A BibtexImporter InputStream.
     * @param fileToImport The path to the file to be imported.
     * @param importFormat The fileformat you want to use to read the passed file to get the list of expected BibEntries
     * @throws IOException
     */
    public static void assertEquals(InputStream expectedIs, Path fileToImport, ImportFormat importFormat,
            ImportFormatPreferences importFormatPreferences)
            throws IOException {
        assertEquals(getListFromInputStream(expectedIs, importFormatPreferences), fileToImport, importFormat);
    }

    public static void assertEquals(InputStream expectedIs, URL fileToImport, ImportFormat importFormat,
            ImportFormatPreferences importFormatPreferences)
            throws URISyntaxException, IOException {
        assertEquals(expectedIs, Paths.get(fileToImport.toURI()), importFormat, importFormatPreferences);
    }

    /**
     * Compares a list of BibEntries to an InputStream. actualIs is filtered through importerForActualIs to convert to a list of BibEntries.
     * @param expected A BibtexImporter InputStream.
     * @param fileToImport The path to the file to be imported.
     * @param importFormat The fileformat you want to use to read the passed file to get the list of expected BibEntries
     * @throws IOException
     */
    public static void assertEquals(List<BibEntry> expected, Path fileToImport, ImportFormat importFormat)
            throws IOException {
        List<BibEntry> actualEntries = importFormat.importDatabase(fileToImport, Charset.defaultCharset())
                .getDatabase().getEntries();
        Assert.assertEquals(expected, actualEntries);
    }

    public static void assertEquals(List<BibEntry> expected, URL fileToImport, ImportFormat importFormat)
            throws URISyntaxException, IOException {
        assertEquals(expected, Paths.get(fileToImport.toURI()), importFormat);
    }
}
