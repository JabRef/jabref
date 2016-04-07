package net.sf.jabref.logic.xmp;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.BibtexTestData;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.xml.DomXmpParser;
import org.apache.xmpbox.xml.XmpParsingException;
import org.apache.xmpbox.xml.XmpSerializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class XMPSchemaBibtexTest {

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    public void assertEqualsBibtexEntry(BibEntry e, BibEntry x) {
        Assert.assertNotNull(e);
        Assert.assertNotNull(x);
        Assert.assertEquals(e.getCiteKey(), x.getCiteKey());
        Assert.assertEquals(e.getType(), x.getType());

        Assert.assertEquals(e.getFieldNames().size(), x.getFieldNames().size());

        for (String name : e.getFieldNames()) {
            Assert.assertEquals(e.getField(name), x.getField(name));
        }
    }

    @Test
    public void testXMPSchemaBibtexXMPMetadata() throws IOException, TransformerException {

        XMPMetadata xmp = XMPMetadata.createXMPMetadata();
        XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);
        xmp.addSchema(bibtex);

        XmpSerializer serializer = new XmpSerializer();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        serializer.serialize(xmp, outputStream, true);

        Assert.assertNotNull(outputStream.toString(String.valueOf(StandardCharsets.UTF_8)).contains("rdf:Description"));
    }

    @Test
    public void testGetSetPersonList() throws IOException {
        XMPMetadata xmp = XMPMetadata.createXMPMetadata();
        XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);

        bibtex.setPersonList("author", "Tom DeMarco and Kent Beck");

        List<String> authors = bibtex.getPersonList("author");
        Assert.assertEquals(2, authors.size());

        Assert.assertEquals("Tom DeMarco", authors.get(0));
        Assert.assertEquals("Kent Beck", authors.get(1));
    }

    @Test
    public void testSetGetTextPropertyString() throws IOException {
        XMPMetadata xmp = XMPMetadata.createXMPMetadata();
        XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);

        bibtex.setTextPropertyValueAsSimple("title",
                "The advanced Flux-Compensation for Delawney-Separation");

        String e = bibtex.getUnqualifiedTextPropertyValue("title");
        Assert.assertEquals("The advanced Flux-Compensation for Delawney-Separation",
                e);

        Assert.assertEquals("The advanced Flux-Compensation for Delawney-Separation",
                bibtex.getUnqualifiedTextPropertyValue("title"));

        bibtex.setTextPropertyValueAsSimple("title",
                "The advanced Flux-Correlation for Delawney-Separation");

        e = bibtex.getUnqualifiedTextPropertyValue("title");
        Assert.assertEquals("The advanced Flux-Correlation for Delawney-Separation", e);

        Assert.assertEquals("The advanced Flux-Correlation for Delawney-Separation",
                bibtex.getUnqualifiedTextPropertyValue("title"));

        bibtex
                .setTextPropertyValueAsSimple(
                        "abstract",
                        "   The abstract\n can go \n \n on several \n lines with \n many \n\n empty ones in \n between.");
        Assert.assertEquals(
                "   The abstract\n can go \n \n on several \n lines with \n many \n\n empty ones in \n between.",
                bibtex.getUnqualifiedTextPropertyValue("abstract"));
    }

    @Test
    public void testSetGetBagListString() throws IOException {

        XMPMetadata xmp = XMPMetadata.createXMPMetadata();
        XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);

        bibtex.addBagValueAsSimple("author", "Tom DeMarco");
        bibtex.addBagValueAsSimple("author", "Kent Beck");
        {

            List<String> l = bibtex.getUnqualifiedBagValueList("author");

            Assert.assertEquals(2, l.size());

            Assert.assertTrue(l.get(0).equals("Tom DeMarco")
                    || l.get(1).equals("Tom DeMarco"));
            Assert.assertTrue(l.get(0).equals("Kent Beck")
                    || l.get(1).equals("Kent Beck"));
        }
        {
            bibtex.removeUnqualifiedBagValue("author", "Kent Beck");
            List<String> l = bibtex.getUnqualifiedBagValueList("author");
            Assert.assertEquals(1, l.size());
            Assert.assertTrue(l.get(0).equals("Tom DeMarco"));
        }
        { // Already removed
            bibtex.removeUnqualifiedBagValue("author", "Kent Beck");
            List<String> l = bibtex.getUnqualifiedBagValueList("author");
            Assert.assertEquals(1, l.size());
            Assert.assertTrue(l.get(0).equals("Tom DeMarco"));
        }
        { // Duplicates allowed!
            bibtex.addBagValueAsSimple("author", "Tom DeMarco");
            List<String> l = bibtex.getUnqualifiedBagValueList("author");
            Assert.assertEquals(2, l.size());
            Assert.assertTrue(l.get(0).equals("Tom DeMarco"));
            Assert.assertTrue(l.get(1).equals("Tom DeMarco"));
        }
        // Removes both
        bibtex.removeUnqualifiedBagValue("author", "Tom DeMarco");
        List<String> l = bibtex.getUnqualifiedBagValueList("author");
        Assert.assertEquals(0, l.size());
    }

    @Test
    public void testGetSequenceListString() throws IOException {

        XMPMetadata xmp = XMPMetadata.createXMPMetadata();
        XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);

        bibtex.addUnqualifiedSequenceValue("author", "Tom DeMarco");
        bibtex.addUnqualifiedSequenceValue("author", "Kent Beck");
        {

            List<String> l = bibtex.getUnqualifiedSequenceValueList("author");

            Assert.assertEquals(2, l.size());

            Assert.assertEquals("Tom DeMarco", l.get(0));
            Assert.assertEquals("Kent Beck", l.get(1));
        }
        {
            bibtex.removeUnqualifiedSequenceValue("author", "Tom DeMarco");
            List<String> l = bibtex.getUnqualifiedSequenceValueList("author");
            Assert.assertEquals(1, l.size());
            Assert.assertTrue(l.get(0).equals("Kent Beck"));
        }
        { // Already removed
            bibtex.removeUnqualifiedSequenceValue("author", "Tom DeMarco");
            List<String> l = bibtex.getUnqualifiedSequenceValueList("author");
            Assert.assertEquals(1, l.size());
            Assert.assertTrue(l.get(0).equals("Kent Beck"));
        }
        { // Duplicates allowed!
            bibtex.addUnqualifiedSequenceValue("author", "Kent Beck");
            List<String> l = bibtex.getUnqualifiedSequenceValueList("author");
            Assert.assertEquals(2, l.size());
            Assert.assertTrue(l.get(0).equals("Kent Beck"));
            Assert.assertTrue(l.get(1).equals("Kent Beck"));
        }
        // Remvoes all
        bibtex.removeUnqualifiedSequenceValue("author", "Kent Beck");
        List<String> l = bibtex.getUnqualifiedSequenceValueList("author");
        Assert.assertEquals(0, l.size());
    }

    @Test
    public void testGetAllProperties() throws IOException {
        XMPMetadata xmp = XMPMetadata.createXMPMetadata();
        XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);

        bibtex.setTextPropertyValueAsSimple("title", "BlaBla Ta Ta\nHello World");
        bibtex.setTextPropertyValueAsSimple("abstract", "BlaBla Ta Ta\nHello World");
        bibtex.setTextPropertyValueAsSimple("review", "BlaBla Ta Ta\nHello World");
        bibtex.setTextPropertyValueAsSimple("note", "BlaBla Ta Ta\nHello World");
        bibtex.setPersonList("author", "Mouse, Mickey and Bond, James");

        Map<String, String> s = XMPSchemaBibtex.getAllProperties(bibtex,
                "bibtex");

        Assert.assertEquals(5, s.size());
        Assert.assertTrue(s.containsKey("title"));
        Assert.assertTrue(s.containsKey("author"));

        Assert.assertEquals("BlaBla Ta Ta Hello World", s.get("title"));
        Assert.assertEquals("BlaBla Ta Ta\nHello World", s.get("abstract"));
        Assert.assertEquals("BlaBla Ta Ta\nHello World", s.get("review"));
        Assert.assertEquals("BlaBla Ta Ta\nHello World", s.get("note"));
        Assert.assertEquals("Mickey Mouse and James Bond", s.get("author"));
    }

    @Test
    public void testSetBibtexEntry() throws IOException {

        XMPMetadata xmp = XMPMetadata.createXMPMetadata();
        XMPSchemaBibtex bibtex = new XMPSchemaBibtex(xmp);

        BibEntry e = BibtexTestData.getBibtexEntry();
        bibtex.setBibtexEntry(e, null);

        BibEntry e2 = bibtex.getBibtexEntry();

        assertEqualsBibtexEntry(e, e2);
    }

    @Test
    public void testGetTextContent() throws IOException, XmpParsingException {
        String bibtexString = "<bibtex:year>2003</bibtex:year>\n"
                + "<bibtex:title>\n   "
                + "Beach sand convolution by surf-wave optimzation\n"
                + "</bibtex:title>\n"
                + "<bibtex:bibtexkey>OezbekC06</bibtex:bibtexkey>\n";

        bibtexString = XMPUtilTest.bibtexXPacket(XMPUtilTest
                .bibtexDescription(bibtexString));

        //TODO: currently no idea how to translate
        //Document d = XMLUtil.parse(new ByteArrayInputStream(bibtexString.getBytes()));

        // Assert.assertEquals("Beach sand convolution by surf-wave optimzation",
        //         XMPSchemaBibtex.getTextContent(
        //                 d.getElementsByTagName("bibtex:title").item(0)).trim());

    }

}
