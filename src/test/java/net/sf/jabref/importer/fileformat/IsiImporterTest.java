package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test cases for the IsiImporter
 */
public class IsiImporterTest {

    private final IsiImporter importer = new IsiImporter();

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testParseMonthException() {
        IsiImporter.parseMonth("20l06 06-07");
    }

    @Test
    public void testGetFormatName() {
        Assert.assertEquals(importer.getFormatName(), "ISI");
    }

    @Test
    public void testGetCLIId() {
        Assert.assertEquals(importer.getId(), "isi");
    }

    @Test
    public void testsGetExtensions() {
        Assert.assertEquals(Arrays.asList(".isi",".txt"), importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        Assert.assertEquals("Importer for the ISI Web of Science, INSPEC and Medline format.",
                importer.getDescription());
    }

    @Test
    public void testIsRecognizedFormatAccepted() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("IsiImporterTest1.isi", "IsiImporterTest2.isi", "IsiImporterTestInspec.isi",
                "IsiImporterTestWOS.isi", "IsiImporterTestMedline.isi");

        for (String str : list) {
            Path file = Paths.get(IsiImporterTest.class.getResource(str).toURI());
            Assert.assertTrue(importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public void testIsRecognizedFormatRejected() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("IsiImporterTestEmpty.isi");

        for (String str : list) {
            Path file = Paths.get(IsiImporterTest.class.getResource(str).toURI());
            Assert.assertFalse(importer.isRecognizedFormat(file, Charset.defaultCharset()));
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
    public void testImportEntries1() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTest1.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
        Assert.assertEquals(1, entries.size());
        BibEntry entry = entries.get(0);
        Assert.assertEquals(Optional.of("Optical properties of MgO doped LiNbO$_3$ single crystals"), entry.getFieldOptional("title"));
        Assert.assertEquals(
                Optional.of(
                        "James Brown and James Marc Brown and Brown, J. M. and Brown, J. and Brown, J. M. and Brown, J."),
                entry.getFieldOptional("author"));

        Assert.assertEquals("article", entry.getType());
        Assert.assertEquals(Optional.of("Optical Materials"), entry.getFieldOptional("journal"));
        Assert.assertEquals(Optional.of("2006"), entry.getFieldOptional("year"));
        Assert.assertEquals(Optional.of("28"), entry.getFieldOptional("volume"));
        Assert.assertEquals(Optional.of("5"), entry.getFieldOptional("number"));
        Assert.assertEquals(Optional.of("467--72"), entry.getFieldOptional("pages"));
    }

    @Test
    public void testImportEntries2() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTest2.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
        Assert.assertEquals(3, entries.size());
        BibEntry entry = entries.get(0);
        Assert.assertEquals(Optional.of("Optical properties of MgO doped LiNbO$_3$ single crystals"), entry.getFieldOptional("title"));

        Assert.assertEquals("misc", entry.getType());
        Assert.assertEquals(Optional.of("Optical Materials"), entry.getFieldOptional("journal"));
        Assert.assertEquals(Optional.of("2006"), entry.getFieldOptional("year"));
        Assert.assertEquals(Optional.of("28"), entry.getFieldOptional("volume"));
        Assert.assertEquals(Optional.of("5"), entry.getFieldOptional("number"));
        Assert.assertEquals(Optional.of("467-72"), entry.getFieldOptional("pages"));
    }

    @Test
    public void testImportEntriesINSPEC() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTestInspec.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

        Assert.assertEquals(2, entries.size());
        BibEntry a = entries.get(0);
        BibEntry b = entries.get(1);

        if (a.getFieldOptional("title").equals(
                Optional.of("Optical and photoelectric spectroscopy of photorefractive Sn$_2$P$_2$S$_6$ crystals"))) {
            BibEntry tmp = a;
            a = b;
            b = tmp;
        }

        Assert.assertEquals(
                Optional.of(
                        "Second harmonic generation of continuous wave ultraviolet light and production of beta -BaB$_2$O$_4$ optical waveguides"),
                a.getFieldOptional("title"));
        Assert.assertEquals("article", a.getType());

        Assert.assertEquals(Optional.of("Degl'Innocenti, R. and Guarino, A. and Poberaj, G. and Gunter, P."), a.getFieldOptional("author"));
        Assert.assertEquals(Optional.of("Applied Physics Letters"), a.getFieldOptional("journal"));
        Assert.assertEquals(Optional.of("2006"), a.getFieldOptional("year"));
        Assert.assertEquals(Optional.of("#jul#"), a.getFieldOptional("month"));
        Assert.assertEquals(Optional.of("89"), a.getFieldOptional("volume"));
        Assert.assertEquals(Optional.of("4"), a.getFieldOptional("number"));
        Assert.assertEquals(Optional.of("Lorem ipsum abstract"), a.getFieldOptional("abstract"));
        Assert.assertEquals(Optional.of("Aip"), a.getFieldOptional("publisher"));

        Assert.assertEquals(
                Optional.of("Optical and photoelectric spectroscopy of photorefractive Sn$_2$P$_2$S$_6$ crystals"),
                b.getFieldOptional("title"));
        Assert.assertEquals("article", b.getType());
    }

    @Test
    public void testImportEntriesWOS() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTestWOS.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

        Assert.assertEquals(2, entries.size());
        BibEntry a = entries.get(0);
        BibEntry b = entries.get(1);

        Assert.assertEquals(Optional.of("Optical and photoelectric spectroscopy of photorefractive Sn2P2S6 crystals"),
                a.getFieldOptional("title"));
        Assert.assertEquals(Optional.of("Optical waveguides in Sn2P2S6 by low fluence MeV He+ ion implantation"),
                b.getFieldOptional("title"));

        Assert.assertEquals(Optional.of("Journal of Physics-condensed Matter"), a.getFieldOptional("journal"));
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
    public void testImportIEEEExport() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IEEEImport1.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

        Assert.assertEquals(1, entries.size());
        BibEntry a = entries.get(0);
        Assert.assertEquals("article", a.getType());
        Assert.assertEquals(Optional.of("Geoscience and Remote Sensing Letters, IEEE"), a.getFieldOptional("journal"));
        Assert.assertEquals(Optional.of("Improving Urban Road Extraction in High-Resolution "
                + "Images Exploiting Directional Filtering, Perceptual " + "Grouping, and Simple Topological Concepts"),
                a.getFieldOptional("title"));
        Assert.assertEquals(Optional.of("4"), a.getFieldOptional("volume"));
        Assert.assertEquals(Optional.of("3"), a.getFieldOptional("number"));
        Assert.assertEquals(Optional.of("1545-598X"), a.getFieldOptional("SN"));
        Assert.assertEquals(Optional.of("387--391"), a.getFieldOptional("pages"));
        Assert.assertEquals(Optional.of("Gamba, P. and Dell'Acqua, F. and Lisini, G."), a.getFieldOptional("author"));
        Assert.assertEquals(Optional.of("2006"), a.getFieldOptional("year"));
        Assert.assertEquals(Optional.of("Perceptual grouping, street extraction, urban remote sensing"), a.getFieldOptional("keywords"));
        Assert.assertEquals(Optional.of("Lorem ipsum abstract"), a.getFieldOptional("abstract"));
    }

    @Test
    public void testIEEEImport() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IEEEImport1.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

        Assert.assertEquals(1, entries.size());
        BibEntry a = entries.get(0);

        Assert.assertEquals("article", a.getType());
        Assert.assertEquals(Optional.of("Geoscience and Remote Sensing Letters, IEEE"), a.getFieldOptional("journal"));
        Assert.assertEquals(
                Optional.of(
                        "Improving Urban Road Extraction in High-Resolution Images Exploiting Directional Filtering, Perceptual Grouping, and Simple Topological Concepts"),
                a.getFieldOptional("title"));
        Assert.assertEquals(Optional.of("4"), a.getFieldOptional("volume"));
        Assert.assertEquals(Optional.of("3"), a.getFieldOptional("number"));
        Assert.assertEquals(Optional.of("1545-598X"), a.getFieldOptional("SN"));
        Assert.assertEquals(Optional.of("387--391"), a.getFieldOptional("pages"));
        Assert.assertEquals(Optional.of("Gamba, P. and Dell'Acqua, F. and Lisini, G."), a.getFieldOptional("author"));
        Assert.assertEquals(Optional.of("2006"), a.getFieldOptional("year"));
        Assert.assertEquals(Optional.of("Perceptual grouping, street extraction, urban remote sensing"), a.getFieldOptional("keywords"));
        Assert.assertEquals(Optional.of("Lorem ipsum abstract"), a.getFieldOptional("abstract"));
    }

    @Test
    public void testImportEntriesMedline() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTestMedline.isi").toURI());

        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

        Assert.assertEquals(2, entries.size());
        BibEntry a = entries.get(0);
        BibEntry b = entries.get(1);

        Assert.assertEquals(
                Optional.of("Effects of modafinil on cognitive performance and alertness during sleep deprivation."),
                a.getFieldOptional("title"));

        Assert.assertEquals(Optional.of("Wesensten, Nancy J."), a.getFieldOptional("author"));
        Assert.assertEquals(Optional.of("Curr Pharm Des"), a.getFieldOptional("journal"));
        Assert.assertEquals(Optional.of("2006"), a.getFieldOptional("year"));
        Assert.assertEquals(Optional.empty(), a.getFieldOptional("month"));
        Assert.assertEquals(Optional.of("12"), a.getFieldOptional("volume"));
        Assert.assertEquals(Optional.of("20"), a.getFieldOptional("number"));
        Assert.assertEquals(Optional.of("2457--71"), a.getFieldOptional("pages"));
        Assert.assertEquals("article", a.getType());

        Assert.assertEquals(
                Optional.of(
                        "Estrogen therapy selectively enhances prefrontal cognitive processes: a randomized, double-blind, placebo-controlled study with functional magnetic resonance imaging in perimenopausal and recently postmenopausal women."),
                b.getFieldOptional("title"));
        Assert.assertEquals(
                Optional.of(
                        "Joffe, Hadine and Hall, Janet E. and Gruber, Staci and Sarmiento, Ingrid A. and Cohen, Lee S. and Yurgelun-Todd, Deborah and Martin, Kathryn A."),
                b.getFieldOptional("author"));
        Assert.assertEquals(Optional.of("2006"), b.getFieldOptional("year"));
        Assert.assertEquals(Optional.of("#may#"), b.getFieldOptional("month"));
        Assert.assertEquals(Optional.of("13"), b.getFieldOptional("volume"));
        Assert.assertEquals(Optional.of("3"), b.getFieldOptional("number"));
        Assert.assertEquals(Optional.of("411--22"), b.getFieldOptional("pages"));
        Assert.assertEquals("article", b.getType());
    }

    @Test
    public void testImportEntriesEmpty() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTestEmpty.isi").toURI());

        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

        Assert.assertEquals(1, entries.size());
    }
}
