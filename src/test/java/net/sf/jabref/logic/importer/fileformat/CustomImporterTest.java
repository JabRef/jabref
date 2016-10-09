package net.sf.jabref.logic.importer.fileformat;

import java.nio.file.Paths;
import java.util.Arrays;

import net.sf.jabref.logic.importer.Importer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
    public void testGetId() {
        assertEquals("cpc", importer.getId());
    }

    @Test
    public void testGetClassName() {
        assertEquals("net.sf.jabref.logic.importer.fileformat.CopacImporter", importer.getClassName());
    }

    @Test
    public void testGetBasePath() {
        assertEquals(Paths.get("src/main/java/net/sf/jabref/logic/importer/fileformat/CopacImporter.java"),
                importer.getBasePath());
    }

    @Test
    public void testGetAsStringList() {
        assertEquals(Arrays.asList("src/main/java/net/sf/jabref/logic/importer/fileformat/CopacImporter.java",
                "net.sf.jabref.logic.importer.fileformat.CopacImporter"), importer.getAsStringList());
    }

    @Test
    public void equalsWithSameReference() {
        assertEquals(importer, importer);
    }

    @Test
    public void equalsIsBasedOnName() {
        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals(new CopacImporter(), importer);
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
        CustomImporter customImporter = new CustomImporter(
                "src/main/java/net/sf/jabref/logic/importer/fileformat/CopacImporter.java",
                "net.sf.jabref.logic.importer.fileformat.CopacImporter");
        assertEquals(importer, customImporter);
    }

    private CustomImporter asCustomImporter(Importer importer) throws Exception {
        return new CustomImporter(
                "src/main/java/net/sf/jabref/logic/importer/fileformat/" + importer.getName() + "Importer.java",
                importer.getClass().getName());
    }
}
