package net.sf.jabref.bibtex;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Assert;

import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fetcher.GVKParser;
import net.sf.jabref.importer.fileformat.BibtexParser;
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
    public static void assertEquals(Class<GVKParser> clazz, String resourceName, BibEntry entry)
            throws IOException {
        Assert.assertNotNull(clazz);
        Assert.assertNotNull(resourceName);
        Assert.assertNotNull(entry);
        try (InputStream shouldBeIs = clazz.getResourceAsStream(resourceName)) {
            BibtexEntryAssert.assertEquals(shouldBeIs, entry);
        }

    }

    /**
     * Reads a bibtex database from the given InputStream. The result has to contain a single BibEntry. This entry is
     * compared to the given entry
     *
     * @param shouldBeIs the inputStream reading the entry from
     * @param entry the entry to compare with
     */
    public static void assertEquals(InputStream shouldBeIs, BibEntry entry)
            throws UnsupportedEncodingException, IOException {
        Assert.assertNotNull(shouldBeIs);
        Assert.assertNotNull(entry);
        ParserResult result;
        try (Reader reader = new InputStreamReader(shouldBeIs, StandardCharsets.UTF_8)) {
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
    private static void assertEquals(BibEntry shouldBeEntry, BibEntry entry) {
        // use the canonical string representation to compare the entries
        Assert.assertEquals(CanonicalBibtexEntry.getCanonicalRepresentation(shouldBeEntry),
                CanonicalBibtexEntry.getCanonicalRepresentation(entry));
    }
}
