package org.jabref.logic.importer.fetcher;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.logic.importer.fileformat.GvkParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@FetcherTest
public class GvkParserTest {

    private void doTest(String xmlName, int expectedSize, List<String> resourceNames) throws Exception {
        try (InputStream is = GvkParserTest.class.getResourceAsStream(xmlName)) {
            GvkParser parser = new GvkParser();
            List<BibEntry> entries = parser.parseEntries(is);
            assertNotNull(entries);
            assertEquals(expectedSize, entries.size());
            int i = 0;
            for (String resourceName : resourceNames) {
                BibEntryAssert.assertEquals(GvkParserTest.class, resourceName, entries.get(i));
                i++;
            }
        }
    }

    @Test
    public void emptyResult() throws Exception {
        doTest("gvk_empty_result_because_of_bad_query.xml", 0, Collections.emptyList());
    }

    @Test
    public void resultFor797485368() throws Exception {
        doTest("gvk_result_for_797485368.xml", 1, Collections.singletonList("gvk_result_for_797485368.bib"));
    }

    @Test
    public void testGMP() throws Exception {
        doTest("gvk_gmp.xml", 2, Arrays.asList("gvk_gmp.1.bib", "gvk_gmp.2.bib"));
    }

    @Test
    public void subTitleTest() throws Exception {
        try (InputStream is = GvkParserTest.class.getResourceAsStream("gvk_artificial_subtitle_test.xml")) {
            GvkParser parser = new GvkParser();
            List<BibEntry> entries = parser.parseEntries(is);
            assertNotNull(entries);
            assertEquals(5, entries.size());

            assertEquals(Optional.empty(), entries.get(0).getField(StandardField.SUBTITLE));
            assertEquals(Optional.of("C"), entries.get(1).getField(StandardField.SUBTITLE));
            assertEquals(Optional.of("Word"), entries.get(2).getField(StandardField.SUBTITLE));
            assertEquals(Optional.of("Word1 word2"), entries.get(3).getField(StandardField.SUBTITLE));
            assertEquals(Optional.of("Word1 word2"), entries.get(4).getField(StandardField.SUBTITLE));
        }
    }
}
