package tests.net.sf.jabref.imports;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Util;
import net.sf.jabref.imports.*;

/**
 * Test for OAI2-Handler and Fetcher.
 * 
 * @author Ulrich St&auml;rk
 * @author Christian Kopf
 * 
 * @version $Revision$ ($Date$)
 * 
 */
public class OAI2ImportTest extends TestCase {

	OAI2Handler handler;

	BibtexEntry be;

	File fis;

	File fis2;

	protected SAXParserFactory parserFactory;

	protected SAXParser saxParser;

	protected void setUp() throws Exception {
		parserFactory = SAXParserFactory.newInstance();
		saxParser = parserFactory.newSAXParser();
		be = new BibtexEntry(Util.createNeutralId(), BibtexEntryType.getType("article"));
		handler = new OAI2Handler(be);
		fis = new File("src/java/tests/net/sf/jabref/imports/oai2.xml");
		fis2 = new File("src/java/tests/net/sf/jabref/imports/oai22.xml");
	}

	public void testCorrectLineBreaks(){
		assertEquals("Test this", OAI2Fetcher.correctLineBreaks("Test\nthis"));
		assertEquals("Test this", OAI2Fetcher.correctLineBreaks("Test \n this"));
		assertEquals("Test\nthis", OAI2Fetcher.correctLineBreaks("Test\n\nthis"));
		assertEquals("Test\nthis", OAI2Fetcher.correctLineBreaks("Test\n    \nthis"));
		assertEquals("Test\nthis", OAI2Fetcher.correctLineBreaks("  Test   \n   \n   this  "));
	}
	
	public void testParse() {
		try {
			saxParser.parse(fis, handler);
			assertEquals("hep-ph/0408155", (String) be.getField("eprint"));
			assertEquals("G. F. Giudice and A. Riotto and A. Zaffaroni", (String) be
				.getField("author"));
			assertEquals("Nucl.Phys. B", (String) be.getField("journal"));
			assertEquals("710", (String) be.getField("volume"));
			assertEquals("2005", (String) be.getField("year"));
			assertEquals("511-525", (String) be.getField("pages"));
			assertEquals("GiuRioZaf05", be.getCiteKey());
			assertEquals("Heavy Particles from Inflation", (String) be.getField("title"));
			assertNotNull((String) be.getField("abstract"));
			assertEquals("23 pages", (String) be.getField("comments"));
			assertEquals("CERN-PH-TH/2004-151", (String) be.getField("reportno"));
		} catch (Exception e) {
			fail("Exception");
		}
	}

	public void testOai22xml() {
		try {
			saxParser.parse(fis2, handler);
			assertEquals("GiuRioZaf05", be.getCiteKey());
			assertEquals("2005", (String) be.getField("year"));
		} catch (Exception e) {
			fail("Exception");
		}
	}

	public void testUrlConstructor() {
		OAI2Fetcher fetcher = new OAI2Fetcher();
		assertEquals(
			"http://arxiv.org/oai2?verb=GetRecord&identifier=oai%3AarXiv.org%3Ahep-ph%2F0408155&metadataPrefix=arXiv",
			fetcher.constructUrl("hep-ph/0408155"));
	}

	public void testFixKey() {
		assertEquals("", OAI2Fetcher.fixKey(""));
		assertEquals("test", OAI2Fetcher.fixKey("test"));
		assertEquals("math/0601001", OAI2Fetcher.fixKey("math.RA/0601001"));
		assertEquals("math/0601001", OAI2Fetcher.fixKey("math.QA/0601001"));
		assertEquals("hep-ph/0408155", OAI2Fetcher.fixKey("hep-ph/0408155"));
	}

	public void testOnline() throws InterruptedException {

		try {
			OAI2Fetcher fetcher = new OAI2Fetcher();
			be = fetcher.importOai2Entry("astro-ph/0702080");

			assertEquals("astro-ph/0702080", (String) be.getField("eprint"));
			assertEquals(
				"Magnetized Hypermassive Neutron Star Collapse: a candidate central engine for short-hard GRBs",
				be.getField("title").toString());
		} catch (Exception e) {
			fail();
			e.printStackTrace();
		}

		Thread.sleep(30000);
		
		try {
			OAI2Fetcher fetcher = new OAI2Fetcher();
			be = fetcher.importOai2Entry("math.QA/0601001");
			assertEquals("math/0601001", (String) be.getField("eprint"));
		} catch (Exception e) {
			fail();
			e.printStackTrace();
		}

		Thread.sleep(30000);

		try {
			OAI2Fetcher fetcher = new OAI2Fetcher();
			be = fetcher.importOai2Entry("hep-ph/0408155");

			assertEquals("hep-ph/0408155", (String) be.getField("eprint"));
		} catch (Exception e) {
			fail();
			e.printStackTrace();
		}

	}
}
