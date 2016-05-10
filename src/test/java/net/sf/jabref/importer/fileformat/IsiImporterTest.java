package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test cases for the IsiImporter
 */
public class IsiImporterTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testImportEntriesException() throws IOException {
        thrown.expect(IOException.class);

        IsiImporter importer = new IsiImporter();
        importer.importEntries(null, new OutputPrinterToNull());
    }

    @Test
    public void testParseMonthException() {
        IsiImporter.parseMonth("20l06 06-07");
    }

    @Test
    public void testGetFormatName() {
        IsiImporter importer = new IsiImporter();

        Assert.assertEquals(importer.getFormatName(), "ISI");
    }

    @Test
    public void testGetCLIId() {
        IsiImporter importer = new IsiImporter();

        Assert.assertEquals(importer.getCLIId(), "isi");
    }

    @Test
    public void testIsRecognizedFormatAccepted() throws IOException {

        IsiImporter importer = new IsiImporter();

        List<String> list = Arrays.asList("IsiImporterTest1.isi", "IsiImporterTest2.isi", "IsiImporterTestInspec.isi",
                "IsiImporterTestWOS.isi", "IsiImporterTestMedline.isi");

        for (String str : list) {
            try (InputStream is = IsiImporterTest.class.getResourceAsStream(str)) {
                Assert.assertTrue(importer.isRecognizedFormat(is));
            }
        }
    }

    @Test
    public void testIsRecognizedFormatRejected() throws IOException {

        IsiImporter importer = new IsiImporter();

        List<String> list = Arrays.asList("IsiImporterTestEmpty.isi");

        for (String str : list) {
            try (InputStream is = IsiImporterTest.class.getResourceAsStream(str)) {
                Assert.assertFalse(importer.isRecognizedFormat(is));
            }
        }
    }

    @Test
    public void testProcessSubSup() {

        HashMap<String, String> hm = new HashMap<>();
        hm.put("title", "/sub 3/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_3$", hm.get("title"));

        hm.put("title", "/sub   3   /");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_3$", hm.get("title"));

        hm.put("title", "/sub 31/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_{31}$", hm.get("title"));

        hm.put("abstract", "/sub 3/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_3$", hm.get("abstract"));

        hm.put("review", "/sub 31/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_{31}$", hm.get("review"));

        hm.put("title", "/sup 3/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$^3$", hm.get("title"));

        hm.put("title", "/sup 31/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$^{31}$", hm.get("title"));

        hm.put("abstract", "/sup 3/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$^3$", hm.get("abstract"));

        hm.put("review", "/sup 31/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$^{31}$", hm.get("review"));

        hm.put("title", "/sub $Hello/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_{\\$Hello}$", hm.get("title"));
    }

    @Test
    public void testImportEntries1() throws IOException {
        IsiImporter importer = new IsiImporter();

        try (InputStream is = IsiImporterTest.class.getResourceAsStream("IsiImporterTest1.isi")) {

            List<BibEntry> entries = importer.importEntries(is, new OutputPrinterToNull());
            Assert.assertEquals(1, entries.size());
            BibEntry entry = entries.get(0);
            Assert.assertEquals("Optical properties of MgO doped LiNbO$_3$ single crystals", entry.getField("title"));
            Assert.assertEquals(
                    "James Brown and James Marc Brown and Brown, J. M. and Brown, J. and Brown, J. M. and Brown, J.",
                    entry.getField("author"));

            Assert.assertEquals("article", entry.getType());
            Assert.assertEquals("Optical Materials", entry.getField("journal"));
            Assert.assertEquals("2006", entry.getField("year"));
            Assert.assertEquals("28", entry.getField("volume"));
            Assert.assertEquals("5", entry.getField("number"));
            Assert.assertEquals("467--72", entry.getField("pages"));
        }
    }

    @Test
    public void testImportEntries2() throws IOException {
        IsiImporter importer = new IsiImporter();

        try (InputStream is = IsiImporterTest.class.getResourceAsStream("IsiImporterTest2.isi")) {

            List<BibEntry> entries = importer.importEntries(is, new OutputPrinterToNull());
            Assert.assertEquals(3, entries.size());
            BibEntry entry = entries.get(0);
            Assert.assertEquals("Optical properties of MgO doped LiNbO$_3$ single crystals", entry.getField("title"));

            Assert.assertEquals("misc", entry.getType());
            Assert.assertEquals("Optical Materials", entry.getField("journal"));
            Assert.assertEquals("2006", entry.getField("year"));
            Assert.assertEquals("28", entry.getField("volume"));
            Assert.assertEquals("5", entry.getField("number"));
            Assert.assertEquals("467-72", entry.getField("pages"));
        }
    }

    @Test
    public void testImportEntriesINSPEC() throws IOException {
        IsiImporter importer = new IsiImporter();

        try (InputStream is = IsiImporterTest.class.getResourceAsStream("IsiImporterTestInspec.isi")) {

            List<BibEntry> entries = importer.importEntries(is, new OutputPrinterToNull());

            Assert.assertEquals(2, entries.size());
            BibEntry a = entries.get(0);
            BibEntry b = entries.get(1);

            if (a.getField("title")
                    .equals("Optical and photoelectric spectroscopy of photorefractive Sn$_2$P$_2$S$_6$ crystals")) {
                BibEntry tmp = a;
                a = b;
                b = tmp;
            }

            Assert.assertEquals(
                    "Second harmonic generation of continuous wave ultraviolet light and production of beta -BaB$_2$O$_4$ optical waveguides",
                    a.getField("title"));
            Assert.assertEquals("article", a.getType());

            Assert.assertEquals("Degl'Innocenti, R. and Guarino, A. and Poberaj, G. and Gunter, P.",
                    a.getField("author"));
            Assert.assertEquals("Applied Physics Letters", a.getField("journal"));
            Assert.assertEquals("2006", a.getField("year"));
            Assert.assertEquals("#jul#", a.getField("month"));
            Assert.assertEquals("89", a.getField("volume"));
            Assert.assertEquals("4", a.getField("number"));
            Assert.assertEquals(
                    "Lorem ipsum abstract",
                    a.getField("abstract"));
            Assert.assertEquals("Aip", a.getField("publisher"));

            Assert.assertEquals("Optical and photoelectric spectroscopy of photorefractive Sn$_2$P$_2$S$_6$ crystals",
                    b.getField("title"));
            Assert.assertEquals("article", b.getType());
        }
    }

    @Test
    public void testImportEntriesWOS() throws IOException {
        IsiImporter importer = new IsiImporter();

        try (InputStream is = IsiImporterTest.class.getResourceAsStream("IsiImporterTestWOS.isi")) {

            List<BibEntry> entries = importer.importEntries(is, new OutputPrinterToNull());

            Assert.assertEquals(2, entries.size());
            BibEntry a = entries.get(0);
            BibEntry b = entries.get(1);

            Assert.assertEquals("Optical and photoelectric spectroscopy of photorefractive Sn2P2S6 crystals",
                    a.getField("title"));
            Assert.assertEquals("Optical waveguides in Sn2P2S6 by low fluence MeV He+ ion implantation",
                    b.getField("title"));

            Assert.assertEquals("Journal of Physics-condensed Matter", a.getField("journal"));
        }
    }

    @Test
    public void testIsiAuthorsConvert() {
        Assert.assertEquals(
                "James Brown and James Marc Brown and Brown, J. M. and Brown, J. and Brown, J. M. and Brown, J.",
                IsiImporter.isiAuthorsConvert(
                        "James Brown and James Marc Brown and Brown, J.M. and Brown, J. and Brown, J.M. and Brown, J."));

        Assert.assertEquals(
                "Joffe, Hadine and Hall, Janet E. and Gruber, Staci and Sarmiento, Ingrid A. and Cohen, Lee S. and Yurgelun-Todd, Deborah and Martin, Kathryn A.",
                IsiImporter.isiAuthorsConvert(
                        "Joffe, Hadine; Hall, Janet E; Gruber, Staci; Sarmiento, Ingrid A; Cohen, Lee S; Yurgelun-Todd, Deborah; Martin, Kathryn A"));

    }

    @Test
    public void testMonthConvert() {

        Assert.assertEquals("#jun#", IsiImporter.parseMonth("06"));
        Assert.assertEquals("#jun#", IsiImporter.parseMonth("JUN"));
        Assert.assertEquals("#jun#", IsiImporter.parseMonth("jUn"));
        Assert.assertEquals("#may#", IsiImporter.parseMonth("MAY-JUN"));
        Assert.assertEquals("#jun#", IsiImporter.parseMonth("2006 06"));
        Assert.assertEquals("#jun#", IsiImporter.parseMonth("2006 06-07"));
        Assert.assertEquals("#jul#", IsiImporter.parseMonth("2006 07 03"));
        Assert.assertEquals("#may#", IsiImporter.parseMonth("2006 May-Jun"));
    }

    @Test
    public void testIsiAuthorConvert() {
        Assert.assertEquals("James Brown", IsiImporter.isiAuthorConvert("James Brown"));
        Assert.assertEquals("James Marc Brown", IsiImporter.isiAuthorConvert("James Marc Brown"));
        Assert.assertEquals("Brown, J. M.", IsiImporter.isiAuthorConvert("Brown, J.M."));
        Assert.assertEquals("Brown, J.", IsiImporter.isiAuthorConvert("Brown, J."));
        Assert.assertEquals("Brown, J. M.", IsiImporter.isiAuthorConvert("Brown, JM"));
        Assert.assertEquals("Brown, J.", IsiImporter.isiAuthorConvert("Brown, J"));
        Assert.assertEquals("Brown, James", IsiImporter.isiAuthorConvert("Brown, James"));
        Assert.assertEquals("Hall, Janet E.", IsiImporter.isiAuthorConvert("Hall, Janet E"));
        Assert.assertEquals("", IsiImporter.isiAuthorConvert(""));
    }

    @Test
    public void testGetIsCustomImporter() {
        IsiImporter importer = new IsiImporter();
        Assert.assertEquals(false, importer.isCustomImporter());
    }

    @Test
    public void testImportIEEEExport() throws IOException {
        IsiImporter importer = new IsiImporter();

        try (InputStream is = IsiImporterTest.class.getResourceAsStream("IEEEImport1.txt")) {

            List<BibEntry> entries = importer.importEntries(is, new OutputPrinterToNull());

            Assert.assertEquals(1, entries.size());
            BibEntry a = entries.get(0);
            Assert.assertEquals("article", a.getType());
            Assert.assertEquals("Geoscience and Remote Sensing Letters, IEEE", a.getField("journal"));
            Assert.assertEquals("Improving Urban Road Extraction in High-Resolution "
                    + "Images Exploiting Directional Filtering, Perceptual "
                    + "Grouping, and Simple Topological Concepts", a.getField("title"));
            Assert.assertEquals("4", a.getField("volume"));
            Assert.assertEquals("3", a.getField("number"));
            Assert.assertEquals("1545-598X", a.getField("SN"));
            Assert.assertEquals("387--391", a.getField("pages"));
            Assert.assertEquals("Gamba, P. and Dell'Acqua, F. and Lisini, G.", a.getField("author"));
            Assert.assertEquals("2006", a.getField("year"));
            Assert.assertEquals("Perceptual grouping, street extraction, urban remote sensing", a.getField("keywords"));
            Assert.assertEquals("Lorem ipsum abstract",
                    a.getField("abstract"));

        }
    }

    @Test
    public void testIEEEImport() throws IOException {
        IsiImporter importer = new IsiImporter();

        try (InputStream is = IsiImporterTest.class.getResourceAsStream("IEEEImport1.txt")) {

            List<BibEntry> entries = importer.importEntries(is, new OutputPrinterToNull());

            Assert.assertEquals(1, entries.size());
            BibEntry a = entries.get(0);

            Assert.assertEquals("article", a.getType());
            Assert.assertEquals("Geoscience and Remote Sensing Letters, IEEE", a.getField("journal"));
            Assert.assertEquals(
                    "Improving Urban Road Extraction in High-Resolution Images Exploiting Directional Filtering, Perceptual Grouping, and Simple Topological Concepts",
                    a.getField("title"));
            Assert.assertEquals("4", a.getField("volume"));
            Assert.assertEquals("3", a.getField("number"));
            Assert.assertEquals("1545-598X", a.getField("SN"));
            Assert.assertEquals("387--391", a.getField("pages"));
            Assert.assertEquals("Gamba, P. and Dell'Acqua, F. and Lisini, G.", a.getField("author"));
            Assert.assertEquals("2006", a.getField("year"));
            Assert.assertEquals("Perceptual grouping, street extraction, urban remote sensing", a.getField("keywords"));
            Assert.assertEquals("Lorem ipsum abstract",
                    a.getField("abstract"));

        }
    }

    @Test
    public void testImportEntriesMedline() throws IOException {
        IsiImporter importer = new IsiImporter();
        try (InputStream is = IsiImporterTest.class.getResourceAsStream("IsiImporterTestMedline.isi")) {

            List<BibEntry> entries = importer.importEntries(is, new OutputPrinterToNull());

            Assert.assertEquals(2, entries.size());
            BibEntry a = entries.get(0);
            BibEntry b = entries.get(1);

            Assert.assertEquals("Effects of modafinil on cognitive performance and alertness during sleep deprivation.",
                    a.getField("title"));

            Assert.assertEquals("Wesensten, Nancy J.", a.getField("author"));
            Assert.assertEquals("Curr Pharm Des", a.getField("journal"));
            Assert.assertEquals("2006", a.getField("year"));
            Assert.assertEquals(null, a.getField("month"));
            Assert.assertEquals("12", a.getField("volume"));
            Assert.assertEquals("20", a.getField("number"));
            Assert.assertEquals("2457--71", a.getField("pages"));
            Assert.assertEquals("article", a.getType());

            Assert.assertEquals(
                    "Estrogen therapy selectively enhances prefrontal cognitive processes: a randomized, double-blind, placebo-controlled study with functional magnetic resonance imaging in perimenopausal and recently postmenopausal women.",
                    b.getField("title"));
            Assert.assertEquals(
                    "Joffe, Hadine and Hall, Janet E. and Gruber, Staci and Sarmiento, Ingrid A. and Cohen, Lee S. and Yurgelun-Todd, Deborah and Martin, Kathryn A.",
                    b.getField("author"));
            Assert.assertEquals("2006", b.getField("year"));
            Assert.assertEquals("#may#", b.getField("month"));
            Assert.assertEquals("13", b.getField("volume"));
            Assert.assertEquals("3", b.getField("number"));
            Assert.assertEquals("411--22", b.getField("pages"));
            Assert.assertEquals("article", b.getType());
        }
    }

    @Test
    public void testImportEntriesEmpty() throws IOException {
        IsiImporter importer = new IsiImporter();

        try (InputStream is = IsiImporterTest.class.getResourceAsStream("IsiImporterTestEmpty.isi")) {

            List<BibEntry> entries = importer.importEntries(is, new OutputPrinterToNull());

            Assert.assertEquals(1, entries.size());
        }
    }
}
