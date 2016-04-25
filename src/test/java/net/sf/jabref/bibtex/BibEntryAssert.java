package net.sf.jabref.bibtex;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.importer.fileformat.ImportFormat;
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
    public static void assertEquals(Class<? extends Object> clazz, String resourceName, BibEntry entry)
            throws IOException {
        Assert.assertNotNull(clazz);
        Assert.assertNotNull(resourceName);
        Assert.assertNotNull(entry);
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
    public static void assertEquals(Class<? extends Object> clazz, String resourceName, List<BibEntry> asIsEntries)
            throws IOException {
        Assert.assertNotNull(clazz);
        Assert.assertNotNull(resourceName);
        Assert.assertNotNull(asIsEntries);
        try (InputStream shouldBeIs = clazz.getResourceAsStream(resourceName)) {
            BibEntryAssert.assertEquals(shouldBeIs, asIsEntries);
        }
    }

    private static List<BibEntry> getListFromInputStream(InputStream is) throws IOException {
        ParserResult result;
        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            BibtexParser parser = new BibtexParser(reader);
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
    public static void assertEquals(InputStream expectedInputStream, List<BibEntry> actualEntries)
            throws UnsupportedEncodingException, IOException {
        Assert.assertNotNull(expectedInputStream);
        Assert.assertNotNull(actualEntries);
        Assert.assertEquals(getListFromInputStream(expectedInputStream), actualEntries);
    }

    public static void assertEquals(List<BibEntry> expectedEntries, InputStream actualInputStream)
            throws UnsupportedEncodingException, IOException {
        Assert.assertNotNull(actualInputStream);
        Assert.assertNotNull(expectedEntries);
        Assert.assertEquals(expectedEntries, getListFromInputStream(actualInputStream));
    }

    /**
     * Reads a bibtex database from the given InputStream. The result has to contain a single BibEntry. This entry is
     * compared to the given entry
     *
     * @param expected the inputStream reading the entry from
     * @param actual the entry to compare with
     */
    public static void assertEquals(InputStream expected, BibEntry actual)
            throws UnsupportedEncodingException, IOException {
        assertEquals(expected, Collections.singletonList(actual));
    }

    /**
     * Compares two InputStreams. For each InputStream a list will be created. expectedIs is read directly, actualIs is filtered through importerForActualIs to convert to a list of BibEntries.
     * @param expectedIs A BibtexImporter InputStream.
     * @param actualIs Your ImportFormat InputStream you want to compare with a BibtexImporter ImportStream.
     * @param importerForActualIs The fileformat you want to use to convert the actualIs to the list of expected BibEntries
     * @throws IOException
     */
    public static void assertEquals(InputStream expectedIs, InputStream actualIs, ImportFormat importerForActualIs)
            throws IOException {
        List<BibEntry> actualEntries = importerForActualIs.importEntries(actualIs, new OutputPrinterToNull());
        Assert.assertEquals(getListFromInputStream(expectedIs), actualEntries);
    }

    /**
     * Compares a list of BibEntries to an InputStream. actualIs is filtered through importerForActualIs to convert to a list of BibEntries.
     * @param expectedIs A BibtexImporter InputStream.
     * @param actualIs Your ImportFormat InputStream you want to compare with a BibtexImporter ImportStream.
     * @param importerForActualIs The fileformat you want to use to convert the actualIs to the list of expected BibEntries
     * @throws IOException
     */
    public static void assertEquals(List<BibEntry> expected, InputStream actualIs, ImportFormat importerForActualIs)
            throws IOException {
        List<BibEntry> actualEntries = importerForActualIs.importEntries(actualIs, new OutputPrinterToNull());
        Assert.assertEquals(expected, actualEntries);
    }

}
