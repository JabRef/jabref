package net.sf.jabref.bibtex;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Assert;

import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexImporter;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.importer.fileformat.ImportFormat;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.CanonicalBibtexEntry;

public class BibtexEntryAssert {

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
            BibtexEntryAssert.assertEquals(shouldBeIs, entry);
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
            BibtexEntryAssert.assertEquals(shouldBeIs, asIsEntries);
        }
    }

    /**
     * Reads a bibtex database from the given InputStream. The result has to contain a single BibEntry.
     * This entry is compared to the given entry. The given entry is contained in a list to ease using of the compare method
     *
     * @param shouldBeInputStream the inputStream reading the entry from
     * @param asIsEntries a list containing a single entry to compare with
     */
    public static void assertEquals(InputStream shouldBeInputStream, List<BibEntry> asIsEntries)
            throws UnsupportedEncodingException, IOException {
        Assert.assertNotNull(shouldBeInputStream);
        Assert.assertNotNull(asIsEntries);
        Assert.assertEquals(1, asIsEntries.size());
        assertEquals(shouldBeInputStream, asIsEntries.get(0));
    }

    /**
     * Reads a bibtex database from the given InputStream. The result has to contain a single BibEntry. This entry is
     * compared to the given entry
     *
     * @param shouldBeInputStream the inputStream reading the entry from
     * @param entry the entry to compare with
     */
    public static void assertEquals(InputStream shouldBeInputStream, BibEntry entry)
            throws UnsupportedEncodingException, IOException {
        Assert.assertNotNull(shouldBeInputStream);
        Assert.assertNotNull(entry);
        ParserResult result;
        try (Reader reader = new InputStreamReader(shouldBeInputStream, StandardCharsets.UTF_8)) {
            BibtexParser parser = new BibtexParser(reader);
            result = parser.parse();
        }
        Assert.assertNotNull(result);
        Assert.assertNotEquals(ParserResult.INVALID_FORMAT, result);
        Assert.assertEquals(1, result.getDatabase().getEntryCount());
        BibEntry shouldBeEntry = result.getDatabase().getEntries().iterator().next();
        assertEquals(shouldBeEntry, entry);
    }

    /**
     * Compares two InputStreams. For each InputStream a list will be created. Afterwards the list will be compared.
     * @param importer The fileformat you want to compare with Bibtex.
     * @param shouldBeIs A BibtexImporter InputStream.
     * @param actualEntries Your ImportFormat InputStream you want to compare with a BibtexImporter ImportStream.
     * @throws IOException
     */
    public static void assertEquals(ImportFormat importer, InputStream shouldBeIs, InputStream actualEntries)
            throws IOException {
        BibtexImporter bibImporter = new BibtexImporter();

        List<BibEntry> importEntries = importer.importEntries(actualEntries, new OutputPrinterToNull());
        List<BibEntry> bibEntries = bibImporter.importEntries(shouldBeIs, new OutputPrinterToNull());
        Assert.assertFalse(importEntries.isEmpty());
        Assert.assertFalse(bibEntries.isEmpty());

        BibtexEntryAssert.assertEquals(importEntries, bibEntries);
    }

    /**
     * Compares to lists of bibtex entries
     *
     * @param shouldBeIs the list with the expected entries
     * @param actualEntries the list with the actual entries
     */
    public static void assertEquals(List<BibEntry> shouldBeIs, List<BibEntry> actualEntries) {
        Assert.assertNotNull(shouldBeIs);
        Assert.assertNotNull(actualEntries);
        Assert.assertEquals(shouldBeIs.size(), actualEntries.size());
        for (int i = 0; i < actualEntries.size(); i++) {
            assertEquals(shouldBeIs.get(i), actualEntries.get(i));
        }
    }

    /**
     * Compares to BibTeX entries using their canonical representation
     */
    public static void assertEquals(BibEntry shouldBeEntry, BibEntry entry) {
        // use the canonical string representation to compare the entries
        String shouldBeEntryRepresentation = CanonicalBibtexEntry.getCanonicalRepresentation(shouldBeEntry);
        String entryRepresentation = CanonicalBibtexEntry.getCanonicalRepresentation(entry);
        Assert.assertEquals(shouldBeEntryRepresentation, entryRepresentation);
    }
}
