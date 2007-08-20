package tests.net.sf.jabref.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.util.XMPSchemaBibtex;

import org.jempbox.impl.XMLUtil;
import org.jempbox.xmp.XMPMetadata;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import tests.net.sf.jabref.FileBasedTestCase;
import tests.net.sf.jabref.JabRefTestCase;

public class XMPSchemaBibtexTest extends JabRefTestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testXMPSchemaBibtexXMPMetadata() throws IOException {

		XMPMetadata xmp = new XMPMetadata();
		XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);

		assertNotNull(bibtex.getElement());
		assertEquals("rdf:Description", bibtex.getElement().getTagName());

	}

	public void testXMPSchemaBibtexElement()
		throws ParserConfigurationException {
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory
			.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Element e = builder.newDocument().createElement("rdf:Description");

		XMPSchemaBibtex bibtex = new XMPSchemaBibtex(e, "bibtex");

		assertEquals(e, bibtex.getElement());
		assertEquals("rdf:Description", bibtex.getElement().getTagName());
	}

	public void testGetSetPersonList() throws IOException {
		XMPMetadata xmp = new XMPMetadata();
		XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);

		bibtex.setPersonList("author", "Tom DeMarco and Kent Beck");

		Element e = bibtex.getElement();

		NodeList l1 = e.getElementsByTagName("bibtex:author");
		assertEquals(1, l1.getLength());

		NodeList l = e.getElementsByTagName("rdf:li");

		assertEquals(2, l.getLength());

		assertEquals("Tom DeMarco", XMLUtil
			.getStringValue(((Element) l.item(0))));
		assertEquals("Kent Beck", XMLUtil.getStringValue(((Element) l.item(1))));

		List<String> authors = bibtex.getPersonList("author");
		assertEquals(2, authors.size());

		assertEquals("Tom DeMarco", authors.get(0));
		assertEquals("Kent Beck", authors.get(1));
	}

	public void testSetGetTextPropertyString() throws IOException {
		XMPMetadata xmp = new XMPMetadata();
		XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);

		bibtex.setTextProperty("title",
			"The advanced Flux-Compensation for Delawney-Separation");

		Element e = bibtex.getElement();
		assertEquals("The advanced Flux-Compensation for Delawney-Separation",
			e.getAttribute("bibtex:title"));

		assertEquals("The advanced Flux-Compensation for Delawney-Separation",
			bibtex.getTextProperty("title"));

		bibtex.setTextProperty("title",
			"The advanced Flux-Correlation for Delawney-Separation");

		e = bibtex.getElement();
		assertEquals("The advanced Flux-Correlation for Delawney-Separation", e
			.getAttribute("bibtex:title"));

		assertEquals("The advanced Flux-Correlation for Delawney-Separation",
			bibtex.getTextProperty("title"));

		bibtex
			.setTextProperty(
				"abstract",
				"   The abstract\n can go \n \n on several \n lines with \n many \n\n empty ones in \n between.");
		assertEquals(
			"   The abstract\n can go \n \n on several \n lines with \n many \n\n empty ones in \n between.",
			bibtex.getTextProperty("abstract"));
	}

	public void testSetGetBagListString() throws IOException {

		XMPMetadata xmp = new XMPMetadata();
		XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);

		bibtex.addBagValue("author", "Tom DeMarco");
		bibtex.addBagValue("author", "Kent Beck");
		{

			List<String> l = bibtex.getBagList("author");

			assertEquals(2, l.size());

			assertTrue(l.get(0).equals("Tom DeMarco")
				|| l.get(1).equals("Tom DeMarco"));
			assertTrue(l.get(0).equals("Kent Beck")
				|| l.get(1).equals("Kent Beck"));
		}
		{
			bibtex.removeBagValue("author", "Kent Beck");
			List<String> l = bibtex.getBagList("author");
			assertEquals(1, l.size());
			assertTrue(l.get(0).equals("Tom DeMarco"));
		}
		{ // Already removed
			bibtex.removeBagValue("author", "Kent Beck");
			List<String> l = bibtex.getBagList("author");
			assertEquals(1, l.size());
			assertTrue(l.get(0).equals("Tom DeMarco"));
		}
		{ // Duplicates allowed!
			bibtex.addBagValue("author", "Tom DeMarco");
			List<String> l = bibtex.getBagList("author");
			assertEquals(2, l.size());
			assertTrue(l.get(0).equals("Tom DeMarco"));
			assertTrue(l.get(1).equals("Tom DeMarco"));
		}
		{ // Removes both
			bibtex.removeBagValue("author", "Tom DeMarco");
			List<String> l = bibtex.getBagList("author");
			assertEquals(0, l.size());
		}
	}

	public void testGetSequenceListString() throws IOException {

		XMPMetadata xmp = new XMPMetadata();
		XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);

		bibtex.addSequenceValue("author", "Tom DeMarco");
		bibtex.addSequenceValue("author", "Kent Beck");
		{

			List<String> l = bibtex.getSequenceList("author");

			assertEquals(2, l.size());

			assertEquals("Tom DeMarco", l.get(0));
			assertEquals("Kent Beck", l.get(1));
		}
		{
			bibtex.removeSequenceValue("author", "Tom DeMarco");
			List<String> l = bibtex.getSequenceList("author");
			assertEquals(1, l.size());
			assertTrue(l.get(0).equals("Kent Beck"));
		}
		{ // Already removed
			bibtex.removeSequenceValue("author", "Tom DeMarco");
			List<String> l = bibtex.getSequenceList("author");
			assertEquals(1, l.size());
			assertTrue(l.get(0).equals("Kent Beck"));
		}
		{ // Duplicates allowed!
			bibtex.addSequenceValue("author", "Kent Beck");
			List<String> l = bibtex.getSequenceList("author");
			assertEquals(2, l.size());
			assertTrue(l.get(0).equals("Kent Beck"));
			assertTrue(l.get(1).equals("Kent Beck"));
		}
		{ // Remvoes all
			bibtex.removeSequenceValue("author", "Kent Beck");
			List<String> l = bibtex.getSequenceList("author");
			assertEquals(0, l.size());
		}
	}

	public void testSetRemoveGetSequenceDateListString() {
		// We don't use this...
	}

	public void testGetAllProperties() throws IOException {
		XMPMetadata xmp = new XMPMetadata();
		XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);

		bibtex.setTextProperty("title", "BlaBla Ta Ta\nHello World");
		bibtex.setTextProperty("abstract", "BlaBla Ta Ta\nHello World");
		bibtex.setTextProperty("review", "BlaBla Ta Ta\nHello World");
		bibtex.setTextProperty("note", "BlaBla Ta Ta\nHello World");
		bibtex.setPersonList("author", "Mouse, Mickey and Bond, James");

		Map<String, String> s = XMPSchemaBibtex.getAllProperties(bibtex,
			"bibtex");

		assertEquals(5, s.size());
		assertTrue(s.containsKey("title"));
		assertTrue(s.containsKey("author"));

		assertEquals("BlaBla Ta Ta Hello World", s.get("title"));
		assertEquals("BlaBla Ta Ta\nHello World", s.get("abstract"));
		assertEquals("BlaBla Ta Ta\nHello World", s.get("review"));
		assertEquals("BlaBla Ta Ta\nHello World", s.get("note"));
		assertEquals("Mickey Mouse and James Bond", s.get("author"));
	}

	public void testSetBibtexEntry() throws IOException {

		XMPMetadata xmp = new XMPMetadata();
		XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);

		BibtexEntry e = FileBasedTestCase.getBibtexEntry();
		bibtex.setBibtexEntry(e, null);

		BibtexEntry e2 = bibtex.getBibtexEntry();

		assertEquals(e, e2);
	}

	public void testGetTextContent() throws IOException {
		String bibtexString = "<bibtex:year>2003</bibtex:year>\n"
			+ "<bibtex:title>\n   "
			+ "Beach sand convolution by surf-wave optimzation\n"
			+ "</bibtex:title>\n"
			+ "<bibtex:bibtexkey>OezbekC06</bibtex:bibtexkey>\n";

		bibtexString = XMPUtilTest.bibtexXPacket(XMPUtilTest
			.bibtexDescription(bibtexString));

		Document d = XMLUtil.parse(new ByteArrayInputStream(bibtexString
			.getBytes()));

		assertEquals("Beach sand convolution by surf-wave optimzation",
			XMPSchemaBibtex.getTextContent(
				d.getElementsByTagName("bibtex:title").item(0)).trim());

	}

}
