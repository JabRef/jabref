package org.jabref.gui.importer.fetcher;

import java.io.IOException;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jabref.logic.importer.util.OAI2Handler;
import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.category.FetcherTest;

import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for OAI2-Handler and Fetcher.
 *
 * @author Ulrich St&auml;rk
 * @author Christian Kopf
 * @author Christopher Oezbek
 */

@FetcherTest
public class OAI2HandlerFetcherTest {

    protected OAI2Handler handler;

    protected BibEntry be;

    protected SAXParserFactory parserFactory;

    protected SAXParser saxParser;


    @BeforeEach
    public void setUp() throws ParserConfigurationException, SAXException {
        parserFactory = SAXParserFactory.newInstance();
        saxParser = parserFactory.newSAXParser();
        be = new BibEntry("article");
        handler = new OAI2Handler(be);
    }

    @Test
    public void testCorrectLineBreaks() {
        assertEquals("Test this", OAI2Handler.correctLineBreaks("Test\nthis"));
        assertEquals("Test this", OAI2Handler.correctLineBreaks("Test \n this"));
        assertEquals("Test\nthis", OAI2Handler.correctLineBreaks("Test\n\nthis"));
        assertEquals("Test\nthis", OAI2Handler.correctLineBreaks("Test\n    \nthis"));
        assertEquals("Test\nthis", OAI2Handler.correctLineBreaks("  Test   \n   \n   this  "));
    }

    @Test
    public void testParse() throws Throwable {
        try {
            saxParser.parse(this.getClass().getResourceAsStream("oai2.xml"), handler);
            assertEquals(Optional.of("hep-ph/0408155"), be.getField("eprint"));
            assertEquals(Optional.of("G. F. Giudice and A. Riotto and A. Zaffaroni and J. López-Peña"),
                    be.getField("author"));
            assertEquals(Optional.of("Nucl.Phys. B"), be.getField("journal"));
            assertEquals(Optional.of("710"), be.getField("volume"));
            assertEquals(Optional.of("2005"), be.getField("year"));
            assertEquals(Optional.of("511-525"), be.getField("pages"));

            // Citekey is only generated if the user says so in the import
            // inspection dialog.
            assertEquals(Optional.empty(), be.getCiteKeyOptional());

            assertEquals(Optional.of("Heavy Particles from Inflation"), be.getField("title"));
            assertTrue(be.getField("abstract").isPresent());
            assertEquals(Optional.of("23 pages"), be.getField("comment"));
            assertEquals(Optional.of("CERN-PH-TH/2004-151"), be.getField("reportno"));
        } catch (SAXException e) {
            throw e.getException();
        }
    }

    @Test
    public void testOai22xml() throws SAXException, IOException {

        saxParser.parse(this.getClass().getResourceAsStream("oai22.xml"), handler);
        assertEquals(Optional.of("2005"), be.getField("year"));

    }

    @Test
    public void testOai23xml() throws SAXException, IOException {

        saxParser.parse(this.getClass().getResourceAsStream("oai23.xml"), handler);
        assertEquals(Optional.of("Javier López Peña and Gabriel Navarro"), be.getField("author"));

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

    @Test
    @Ignore
    public void testOnline() throws InterruptedException, IOException, SAXException {

        {
            OAI2Fetcher fetcher = new OAI2Fetcher();
            be = fetcher.importOai2Entry("math.RA/0612188");
            assertNotNull(be);

            assertEquals(Optional.of("math/0612188"), be.getField("eprint"));
            assertEquals(Optional.of("On the classification and properties of noncommutative duplicates"),
                    be.getField("title"));
            assertEquals(Optional.of("Javier López Peña and Gabriel Navarro"), be.getField("author"));
            assertEquals(Optional.of("2007"), be.getField("year"));

            Thread.sleep(20000);
        }

        {
            OAI2Fetcher fetcher = new OAI2Fetcher();
            be = fetcher.importOai2Entry("astro-ph/0702080");
            assertNotNull(be);

            assertEquals(Optional.of("astro-ph/0702080"), be.getField("eprint"));
            assertEquals(
                    Optional.of(
                            "Magnetized Hypermassive Neutron Star Collapse: a candidate central engine for short-hard GRBs"),
                    be.getField("title"));

            Thread.sleep(20000);
        }

        {
            OAI2Fetcher fetcher = new OAI2Fetcher();
            be = fetcher.importOai2Entry("math.QA/0601001");
            assertNotNull(be);

            assertEquals(Optional.of("math/0601001"), be.getField("eprint"));
            Thread.sleep(20000);
        }

        {
            OAI2Fetcher fetcher = new OAI2Fetcher();
            be = fetcher.importOai2Entry("hep-ph/0408155");
            assertNotNull(be);

            assertEquals(Optional.of("hep-ph/0408155"), be.getField("eprint"));
            Thread.sleep(20000);
        }

        OAI2Fetcher fetcher = new OAI2Fetcher();
        be = fetcher.importOai2Entry("0709.3040");
        assertNotNull(be);

        assertEquals(Optional.of("2007"), be.getField("year"));
        assertEquals(Optional.of("#sep#"), be.getField("month"));

    }
}
