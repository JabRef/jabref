package net.sf.jabref.importer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.fileformat.BibTeXMLImporter;
import net.sf.jabref.importer.fileformat.CopacImporter;
import net.sf.jabref.importer.fileformat.OvidImporter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CustomImporterTest {

    private CustomImporter importer1;
    private CustomImporter importer2;
    private CustomImporter importer3;

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();

        BibTeXMLImporter bibtexml = new BibTeXMLImporter();
        importer1 = new CustomImporter();
        importer1.setName(bibtexml.getFormatName());
        importer1.setCliId(bibtexml.getId());
        importer1.setClassName(bibtexml.getClass().getName());
        importer1.setBasePath("src/main/java/net/sf/jabref/importer/fileformat/BibTeXMLImporter.java");

        OvidImporter ovid = new OvidImporter();

        List<String> dataTest = Arrays.asList(ovid.getFormatName(), ovid.getId(), ovid.getClass().getName(), "src/main/java/net/sf/jabref/importer/fileformat/OvidImporter.java");
        importer2 = new CustomImporter(dataTest);

        CopacImporter copac = new CopacImporter();
        importer3 = new CustomImporter(copac.getFormatName(), copac.getId(), copac.getClass().getName(), "src/main/java/net/sf/jabref/importer/fileformat/CopacImporter.java");
    }

    @Test
    public void testGetName() {
        assertEquals("BibTeXML", importer1.getName());
        assertEquals("Ovid", importer2.getName());
        assertEquals("Copac", importer3.getName());
    }

    @Test
    public void testGetCliId() {
        assertEquals("bibtexml", importer1.getClidId());
        assertEquals("ovid", importer2.getClidId());
        assertEquals("cpc", importer3.getClidId());
    }

    @Test
    public void testGetClassName() {
        assertEquals("net.sf.jabref.importer.fileformat.BibTeXMLImporter", importer1.getClassName());
        assertEquals("net.sf.jabref.importer.fileformat.OvidImporter", importer2.getClassName());
        assertEquals("net.sf.jabref.importer.fileformat.CopacImporter", importer3.getClassName());
    }

    @Test
    public void testGetBasePath() {
        assertEquals("src/main/java/net/sf/jabref/importer/fileformat/BibTeXMLImporter.java", importer1.getBasePath());
        assertEquals("src/main/java/net/sf/jabref/importer/fileformat/OvidImporter.java", importer2.getBasePath());
        assertEquals("src/main/java/net/sf/jabref/importer/fileformat/CopacImporter.java", importer3.getBasePath());
    }

    @Test
    public void testGetInstance() throws IOException, ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        assertEquals(new BibTeXMLImporter(), importer1.getInstance());
        assertEquals(new OvidImporter(), importer2.getInstance());
        assertEquals(new CopacImporter(), importer3.getInstance());
    }

    @Test
    public void testGetFileFromBasePath() {
        assertEquals(new File(importer1.getBasePath()), importer1.getFileFromBasePath());
        assertEquals(new File(importer2.getBasePath()), importer2.getFileFromBasePath());
        assertEquals(new File(importer3.getBasePath()), importer3.getFileFromBasePath());
    }

    @Test
    public void testGetBasePathUrl() throws MalformedURLException {
        assertEquals(new File(importer1.getBasePath()).toURI().toURL(), importer1.getBasePathUrl());
        assertEquals(new File(importer2.getBasePath()).toURI().toURL(), importer2.getBasePathUrl());
        assertEquals(new File(importer3.getBasePath()).toURI().toURL(), importer3.getBasePathUrl());
    }

    @Test
    public void testGetAsStringList() {
        assertEquals(importer1.getName(), importer1.getAsStringList().get(0));
        assertEquals(importer1.getClidId(), importer1.getAsStringList().get(1));
        assertEquals(importer1.getClassName(), importer1.getAsStringList().get(2));
        assertEquals(importer1.getBasePath(), importer1.getAsStringList().get(3));

        assertEquals(importer2.getName(), importer2.getAsStringList().get(0));
        assertEquals(importer2.getClidId(), importer2.getAsStringList().get(1));
        assertEquals(importer2.getClassName(), importer2.getAsStringList().get(2));
        assertEquals(importer2.getBasePath(), importer2.getAsStringList().get(3));

        assertEquals(importer3.getName(), importer3.getAsStringList().get(0));
        assertEquals(importer3.getClidId(), importer3.getAsStringList().get(1));
        assertEquals(importer3.getClassName(), importer3.getAsStringList().get(2));
        assertEquals(importer3.getBasePath(), importer3.getAsStringList().get(3));
    }

    @Test
    public void testEquals() {
        boolean test1 = importer1.equals(importer1);
        boolean test2 = importer1.equals(importer2);
        boolean test3 = importer3.equals(importer1);
        
        assertTrue(test1);
        assertFalse(test2);
        assertFalse(test3);
    }

    @Test
    public void testHashCode() {
        assertEquals(646299595, importer1.hashCode());
        assertEquals(2470242, importer2.hashCode());
        assertEquals(65293446, importer3.hashCode());
    }

    @Test
    public void testCompareTo() {
        assertEquals(0, importer1.compareTo(importer1));
        assertTrue(importer1.compareTo(importer2) < 0);
        assertTrue(importer3.compareTo(importer1) > 0);
    }

    @Test
    public void testToString() {
        assertEquals("BibTeXML", importer1.toString());
        assertEquals("Ovid", importer2.toString());
        assertEquals("Copac", importer3.toString());
    }
}
