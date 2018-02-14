package org.jabref.logic.xmp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.BibtexTestData;
import org.jabref.logic.bibtex.FieldContentParserPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;

import org.apache.jempbox.impl.XMLUtil;
import org.apache.jempbox.xmp.XMPMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XMPSchemaBibtexTest {

    private ImportFormatPreferences prefs;

    @BeforeEach
    public void setUp() {
        prefs = mock(ImportFormatPreferences.class);
    }

    public void assertEqualsBibtexEntry(BibEntry e, BibEntry x) {
        assertNotNull(e);
        assertNotNull(x);
        assertEquals(e.getCiteKeyOptional(), x.getCiteKeyOptional());
        assertEquals(e.getType(), x.getType());

        assertEquals(e.getFieldNames().size(), x.getFieldNames().size());

        for (String name : e.getFieldNames()) {
            assertEquals(e.getField(name), x.getField(name));
        }
    }

    @Test
    public void testXMPSchemaBibtexXMPMetadata() throws IOException {

        XMPMetadata xmp = new XMPMetadata();
        XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);

        assertNotNull(bibtex.getElement());
        assertEquals("rdf:Description", bibtex.getElement().getTagName());

    }

    @Test
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

    @Test
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
                .getStringValue((Element) l.item(0)));
        assertEquals("Kent Beck", XMLUtil.getStringValue((Element) l.item(1)));

        List<String> authors = bibtex.getPersonList("author");
        assertEquals(2, authors.size());

        assertEquals("Tom DeMarco", authors.get(0));
        assertEquals("Kent Beck", authors.get(1));
    }

    @Test
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

    @Test
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
        // Removes both
        bibtex.removeBagValue("author", "Tom DeMarco");
        List<String> l = bibtex.getBagList("author");
        assertEquals(0, l.size());
    }

    @Test
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
        // Remvoes all
        bibtex.removeSequenceValue("author", "Kent Beck");
        List<String> l = bibtex.getSequenceList("author");
        assertEquals(0, l.size());
    }

    @Test
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

    @Test
    public void testSetBibtexEntry() throws IOException {
        when(prefs.getFieldContentParserPreferences()).thenReturn(new FieldContentParserPreferences());

        XMPMetadata xmp = new XMPMetadata();
        XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);
        BibEntry e = BibtexTestData.getBibtexEntry(prefs);
        bibtex.setBibtexEntry(e, null);

        BibEntry e2 = bibtex.getBibtexEntry();

        assertEqualsBibtexEntry(e, e2);
    }

    @Test
    public void testGetTextContent() throws IOException {
        String bibtexString = "<bibtex:year>2003</bibtex:year>\n"
                + "<bibtex:title>\n   "
                + "Beach sand convolution by surf-wave optimzation\n"
                + "</bibtex:title>\n"
                + "<bibtex:bibtexkey>OezbekC06</bibtex:bibtexkey>\n";

        bibtexString = XMPUtilTest.bibtexXPacket(XMPUtilTest
                .bibtexDescription(bibtexString));

        Document d = XMLUtil.parse(new ByteArrayInputStream(bibtexString
                .getBytes(StandardCharsets.UTF_8)));

        assertEquals("Beach sand convolution by surf-wave optimzation",
                XMPSchemaBibtex.getTextContent(
                        d.getElementsByTagName("bibtex:title").item(0)).trim());

    }

}
