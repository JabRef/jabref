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
    /**
     * Checks that the tag 037C works correctly.
     * 
     * Specifically, checks that the PICA+ (PICA 3.0)
     * specification is correctly followed for the tag
     * 037C (i.e academic records), by checking that 
     * dissertation information and address can be 
     * correctly extracted from the academic record.
     */
    public void test037C() throws Exception {
        doTest("gvk_037C_diss.xml", 1, Collections.singletonList("gvk_037C_diss.bib"));
        doTest("gvk_037C_book.xml", 1, Collections.singletonList("gvk_037C_book.bib"));
    }

    @Test
    /**
     * Checks that the tag 030F works correctly.
     * 
     * Specifically, checks that the PICA+ (PICA 3.0)
     * specification is correctly followed for the tag
     * 030F (i.e conference information), by checking that 
     * conference information is correctly extracted and 
     * put into the Bib entry.
     * 
     * This is done by sending conference information such 
     * as a subtitle and conference address.
     */
    public void test030F() throws Exception {
        doTest("gvk_030F.xml", 1, Collections.singletonList("gvk_030F.bib"));
    }

    @Test
    /**
     * Checks that the tag 031A works correctly.
     * 
     * Specifically, checks that the PICA+ (PICA 3.0)
     * specification is correctly followed for the tag
     * 031A (i.e differentiating information), by checking 
     * that the year is properly overwritten with the new
     * year (2020).
     */
    public void test031A() throws Exception {
        doTest("gvk_031A.xml", 1, Collections.singletonList("gvk_031A.bib"));
    }

    @Test
    /**
     * Checks that the tag 002@ works correctly.
     * 
     * Specifically, checks that the PICA+ (PICA 3.0)
     * specification is correctly followed for the tag
     * 002@ (i.e document type/status/information), by 
     * checking that the document type is correctly 
     * extracted.
     * 
     * This test checks the type "Asy".
     */
    public void test002AT() throws Exception {
        doTest("gvk_002@.xml", 1, Collections.singletonList("gvk_002@.bib"));
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
