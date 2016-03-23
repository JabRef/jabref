package net.sf.jabref.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibtexEntryAssert;
import net.sf.jabref.model.entry.BibEntry;

public class GVKParserTest {

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    private void doTest(String xmlName, int expectedSize, List<String> resourceNames)
            throws ParserConfigurationException, SAXException, IOException {
        try (InputStream is = GVKParser.class.getResourceAsStream(xmlName)) {
            GVKParser parser = new GVKParser();
            List<BibEntry> entries = parser.parseEntries(is);
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
        doTest("gvk_empty_result_becaue_of_bad_query.xml", 0, Collections.emptyList());
    }

    @Test
    public void resultFor797485368() throws Exception {
        doTest("gvk_result_for_797485368.xml", 1, Arrays.asList("gvk_result_for_797485368.bib"));
    }

    @Test
    public void testGMP() throws Exception {
        doTest("gvk_gmp.xml", 2, Arrays.asList("gvk_gmp.1.bib", "gvk_gmp.2.bib"));
    }

    @Test
    public void subTitleTest() throws IOException, ParserConfigurationException, SAXException {
        try (InputStream is = GVKParser.class.getResourceAsStream("gvk_artificial_subtitle_test.xml")) {
            GVKParser parser = new GVKParser();
            List<BibEntry> entries = parser.parseEntries(is);
            Assert.assertNotNull(entries);
            Assert.assertEquals(5, entries.size());

            BibEntry entry = entries.get(0);
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
