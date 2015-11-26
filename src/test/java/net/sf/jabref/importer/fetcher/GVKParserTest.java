package net.sf.jabref.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import net.sf.jabref.bibtex.BibtexEntryAssert;
import net.sf.jabref.model.entry.BibtexEntry;

public class GVKParserTest {

    private void doTest(String xmlName, int expectedSize, List<String> resourceNames)
            throws ParserConfigurationException, SAXException, IOException {
        try (InputStream is = GVKParser.class.getResourceAsStream(xmlName)) {
            GVKParser parser = new GVKParser();
            List<BibtexEntry> entries = parser.parseEntries(is);
            Assert.assertNotNull(entries);
            Assert.assertEquals(expectedSize, entries.size());
            int i = 0;
            for (String resourceName : resourceNames) {
                BibtexEntryAssert.assertEquals(GVKParser.class, resourceName, entries.get(i));
                i++;
            }
        }
    }

    @Test
    public void emptyResult() throws Exception {
        doTest("gvk_empty_result_becaue_of_bad_query.xml", 0, Collections.EMPTY_LIST);
    }

    @Test
    public void resultFor797485368() throws Exception {
        doTest("gvk_result_for_797485368.xml", 1, Arrays.asList(new String[] {"gvk_result_for_797485368.bib"}));
    }

    @Test
    public void GMP() throws Exception {
        doTest("gvk_gmp.xml", 2, Arrays.asList(new String[] {"gvk_gmp.1.bib", "gvk_gmp.2.bib"}));
    }

    @Test
    public void subTitleTest() throws Exception {
        try (InputStream is = GVKParser.class.getResourceAsStream("gvk_artificial_subtitle_test.xml")) {
            GVKParser parser = new GVKParser();
            List<BibtexEntry> entries = parser.parseEntries(is);
            Assert.assertNotNull(entries);
            Assert.assertEquals(5, entries.size());

            BibtexEntry entry = entries.get(0);
            Assert.assertEquals(null, entry.getField("subtitle"));

            entry = entries.get(1);
            Assert.assertEquals("C", entry.getField("subtitle"));

            entry = entries.get(2);
            Assert.assertEquals("Word", entry.getField("subtitle"));

            entry = entries.get(3);
            Assert.assertEquals("Word1 word2", entry.getField("subtitle"));

            entry = entries.get(4);
            Assert.assertEquals("Word1 word2", entry.getField("subtitle"));
        }
    }
}
