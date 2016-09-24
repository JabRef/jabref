package net.sf.jabref.logic.importer.fileformat;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.logic.importer.Importer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CustomImporterTest {

    private CustomImporter importer;

    @Before
    public void setUp() throws Exception {
        importer = asCustomImporter(new CopacImporter());
    }

    @Test
    public void testGetName() {
        assertEquals("Copac", importer.getName());
    }

    @Test
    public void testGetCliId() {
        assertEquals("cpc", importer.getClidId());
    }

    @Test
    public void testGetClassName() {
        assertEquals("net.sf.jabref.logic.importer.fileformat.CopacImporter", importer.getClassName());
    }

    @Test
    public void testGetBasePath() {
        assertEquals("src/main/java/net/sf/jabref/logic/importer/fileformat/CopacImporter.java",
                importer.getBasePath());
    }

    @Test
    public void testGetInstance() throws Exception {
        assertEquals(new CopacImporter(), importer.getInstance());
    }

    @Test
    public void testGetFileFromBasePath() {
        assertEquals(new File("src/main/java/net/sf/jabref/logic/importer/fileformat/CopacImporter.java"),
                importer.getFileFromBasePath());
    }

    @Test
    public void testGetBasePathUrl() throws Exception {
        assertEquals(
                new File("src/main/java/net/sf/jabref/logic/importer/fileformat/CopacImporter.java").toURI().toURL(),
                importer.getBasePathUrl());
    }

    @Test
    public void testGetAsStringList() {
        assertEquals("Copac", importer.getAsStringList().get(0));
        assertEquals("cpc", importer.getAsStringList().get(1));
        assertEquals("net.sf.jabref.logic.importer.fileformat.CopacImporter", importer.getAsStringList().get(2));
        assertEquals("src/main/java/net/sf/jabref/logic/importer/fileformat/CopacImporter.java",
                importer.getAsStringList().get(3));
    }

    @Test
    public void testEqualsTrue() {
        assertEquals(importer, importer);
    }

    @Test
    public void testEqualsFalse() {
        assertNotEquals(new CopacImporter(), importer);
    }

    @Test
    public void testCompareToSmaller() throws Exception {
        CustomImporter ovidImporter = asCustomImporter(new OvidImporter());

        assertTrue(importer.compareTo(ovidImporter) < 0);
    }

    @Test
    public void testCompareToGreater() throws Exception {
        CustomImporter bibtexmlImporter = asCustomImporter(new BibTeXMLImporter());
        CustomImporter ovidImporter = asCustomImporter(new OvidImporter());

        assertTrue(ovidImporter.compareTo(bibtexmlImporter) > 0);
    }

    @Test
    public void testCompareToEven() throws Exception {
        assertEquals(0, importer.compareTo(asCustomImporter(new CopacImporter())));
    }

    @Test
    public void testToString() {
        assertEquals("Copac", importer.toString());
    }

    @Test
    public void testClassicConstructor() throws Exception {
        CustomImporter customImporter = new CustomImporter("Copac", "cpc",
                "net.sf.jabref.logic.importer.fileformat.CopacImporter",
                "src/main/java/net/sf/jabref/logic/importer/fileformat/CopacImporter.java");
        assertEquals(importer, customImporter);
    }

    @Test
    public void testListConstructor() throws Exception {
        List<String> dataTest = Arrays.asList("Ovid", "ovid", "net.sf.jabref.logic.importer.fileformat.OvidImporter",
                "src/main/java/net/sf/jabref/logic/importer/fileformat/OvidImporter.java");
        CustomImporter customImporter = new CustomImporter(dataTest);
        CustomImporter customOvidImporter = asCustomImporter(new OvidImporter());

        assertEquals(customImporter, customOvidImporter);
    }

    @Test
    public void testEmptyConstructor() throws Exception {
        CustomImporter customImporter = new CustomImporter();
        customImporter.setName("Ovid");
        customImporter.setCliId("ovid");
        customImporter.setClassName("net.sf.jabref.logic.importer.fileformat.OvidImporter");
        customImporter.setBasePath("src/main/java/net/sf/jabref/logic/importer/fileformat/OvidImporter.java");

        CustomImporter customOvidImporter = asCustomImporter(new OvidImporter());

        assertEquals(customImporter, customOvidImporter);
    }

    public CustomImporter asCustomImporter(Importer importer) throws Exception {
        return new CustomImporter(importer.getFormatName(), importer.getId(), importer.getClass().getName(),
                "src/main/java/net/sf/jabref/logic/importer/fileformat/" + importer.getFormatName() + "Importer.java");
    }
}
