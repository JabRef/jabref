package net.sf.jabref.imports;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Util;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test for OAI2-Handler and Fetcher.
 *
 * @author Ulrich St&auml;rk
 * @author Christian Kopf
 * @author Christopher Oezbek
 * @version $Revision$ ($Date$)
 */
public class OAI2ImportTest {

    OAI2Handler handler;

    BibtexEntry be;

    protected SAXParserFactory parserFactory;

    protected SAXParser saxParser;

    @Before
    public void setUp() throws Exception {
        parserFactory = SAXParserFactory.newInstance();
        saxParser = parserFactory.newSAXParser();
        be = new BibtexEntry(Util.createNeutralId(), BibtexEntryType.getType("article"));
        handler = new OAI2Handler(be);
    }

    @Test
    public void testCorrectLineBreaks() {
        assertEquals("Test this", OAI2Fetcher.correctLineBreaks("Test\nthis"));
        assertEquals("Test this", OAI2Fetcher.correctLineBreaks("Test \n this"));
        assertEquals("Test\nthis", OAI2Fetcher.correctLineBreaks("Test\n\nthis"));
        assertEquals("Test\nthis", OAI2Fetcher.correctLineBreaks("Test\n    \nthis"));
        assertEquals("Test\nthis", OAI2Fetcher.correctLineBreaks("  Test   \n   \n   this  "));
    }

    @Test
    public void testParse() throws Throwable {
        try {
            saxParser.parse(this.getClass().getResourceAsStream("oai2.xml"), handler);
            assertEquals("hep-ph/0408155", be.getField("eprint"));
            assertEquals("G. F. Giudice and A. Riotto and A. Zaffaroni and J. López-Peña",
                    be.getField("author"));
            assertEquals("Nucl.Phys. B", be.getField("journal"));
            assertEquals("710", be.getField("volume"));
            assertEquals("2005", be.getField("year"));
            assertEquals("511-525", be.getField("pages"));

            // Citekey is only generated if the user says so in the import
            // inspection dialog.
            assertEquals(null, be.getCiteKey());

            assertEquals("Heavy Particles from Inflation", be.getField("title"));
            assertNotNull(be.getField("abstract"));
            assertEquals("23 pages", be.getField("comments"));
            assertEquals("CERN-PH-TH/2004-151", be.getField("reportno"));
        } catch (SAXException e) {
            throw e.getException();
        }
    }

    @Test
    public void testOai22xml() throws Exception {
        try {
            saxParser.parse(this.getClass().getResourceAsStream("oai22.xml"), handler);
            assertEquals("2005", be.getField("year"));
        } catch (SAXException e) {
            throw e.getException();
        }
    }

    @Test
    public void testOai23xml() throws Throwable {
        try {
            saxParser.parse(this.getClass().getResourceAsStream("oai23.xml"), handler);
            assertEquals("Javier López Peña and Gabriel Navarro", be.getField("author"));
        } catch (SAXException e) {
            throw e.getException();
        }

    }

    @Test
    public void testUrlConstructor() {
        OAI2Fetcher fetcher = new OAI2Fetcher();
        assertEquals(
                "http://export.arxiv.org/oai2?verb=GetRecord&identifier=oai%3AarXiv.org%3Ahep-ph%2F0408155&metadataPrefix=arXiv",
                fetcher.constructUrl("hep-ph/0408155"));

        assertEquals(
                "http://export.arxiv.org/oai2?verb=GetRecord&identifier=oai%3AarXiv.org%3Amath%2F0612188&metadataPrefix=arXiv",
                fetcher.constructUrl("math/0612188"));

    }

    @Test
    public void testFixKey() {
        assertEquals("", OAI2Fetcher.fixKey(""));
        assertEquals("test", OAI2Fetcher.fixKey("test"));
        assertEquals("math/0601001", OAI2Fetcher.fixKey("math.RA/0601001"));
        assertEquals("math/0601001", OAI2Fetcher.fixKey("math.QA/0601001"));
        assertEquals("hep-ph/0408155", OAI2Fetcher.fixKey("hep-ph/0408155"));
        assertEquals("0709.3040v1", OAI2Fetcher.fixKey("arXiv:0709.3040v1"));
        assertEquals("", OAI2Fetcher.fixKey("arXiv:"));
    }

    @Test @Ignore
    public void testOnline() throws InterruptedException {

        {
            OAI2Fetcher fetcher = new OAI2Fetcher();
            be = fetcher.importOai2Entry("math.RA/0612188");
            assertNotNull(be);

            assertEquals("math/0612188", be.getField("eprint"));
            assertEquals("On the classification and properties of noncommutative duplicates", be
                    .getField("title"));
            assertEquals("Javier López Peña and Gabriel Navarro", be.getField("author"));
            assertEquals("2007", be.getField("year"));

            Thread.sleep(20000);
        }

        {
            OAI2Fetcher fetcher = new OAI2Fetcher();
            be = fetcher.importOai2Entry("astro-ph/0702080");
            assertNotNull(be);

            assertEquals("astro-ph/0702080", be.getField("eprint"));
            assertEquals(
                    "Magnetized Hypermassive Neutron Star Collapse: a candidate central engine for short-hard GRBs",
                    be.getField("title"));

            Thread.sleep(20000);
        }

        {
            OAI2Fetcher fetcher = new OAI2Fetcher();
            be = fetcher.importOai2Entry("math.QA/0601001");
            assertNotNull(be);

            assertEquals("math/0601001", be.getField("eprint"));
            Thread.sleep(20000);
        }

        {
            OAI2Fetcher fetcher = new OAI2Fetcher();
            be = fetcher.importOai2Entry("hep-ph/0408155");
            assertNotNull(be);

            assertEquals("hep-ph/0408155", be.getField("eprint"));
            Thread.sleep(20000);
        }

        {
            OAI2Fetcher fetcher = new OAI2Fetcher();
            be = fetcher.importOai2Entry("0709.3040");
            assertNotNull(be);

            assertEquals("2007", be.getField("year"));
            assertEquals("#sep#", be.getField("month"));
        }

    }
}
