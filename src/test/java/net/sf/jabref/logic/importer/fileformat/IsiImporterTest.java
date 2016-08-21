package net.sf.jabref.logic.importer.fileformat;

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
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        assertEquals(importer.getFormatName(), "ISI");
    }

    @Test
    public void testGetCLIId() {
        assertEquals(importer.getId(), "isi");
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileExtensions.ISI, importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Importer for the ISI Web of Science, INSPEC and Medline format.",
                importer.getDescription());
    }

    @Test
    public void testIsRecognizedFormatAccepted() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("IsiImporterTest1.isi", "IsiImporterTest2.isi", "IsiImporterTestInspec.isi",
                "IsiImporterTestWOS.isi", "IsiImporterTestMedline.isi");

        for (String str : list) {
            Path file = Paths.get(IsiImporterTest.class.getResource(str).toURI());
            assertTrue(importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public void testIsRecognizedFormatRejected() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("IsiImporterTestEmpty.isi");

        for (String str : list) {
            Path file = Paths.get(IsiImporterTest.class.getResource(str).toURI());
            assertFalse(importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public void testProcessSubSup() {

        HashMap<String, String> hm = new HashMap<>();
        hm.put("title", "/sub 3/");
        IsiImporter.processSubSup(hm);
        assertEquals("$_3$", hm.get("title"));

        hm.put("title", "/sub   3   /");
        IsiImporter.processSubSup(hm);
        assertEquals("$_3$", hm.get("title"));

        hm.put("title", "/sub 31/");
        IsiImporter.processSubSup(hm);
        assertEquals("$_{31}$", hm.get("title"));

        hm.put("abstract", "/sub 3/");
        IsiImporter.processSubSup(hm);
        assertEquals("$_3$", hm.get("abstract"));

        hm.put("review", "/sub 31/");
        IsiImporter.processSubSup(hm);
        assertEquals("$_{31}$", hm.get("review"));

        hm.put("title", "/sup 3/");
        IsiImporter.processSubSup(hm);
        assertEquals("$^3$", hm.get("title"));

        hm.put("title", "/sup 31/");
        IsiImporter.processSubSup(hm);
        assertEquals("$^{31}$", hm.get("title"));

        hm.put("abstract", "/sup 3/");
        IsiImporter.processSubSup(hm);
        assertEquals("$^3$", hm.get("abstract"));

        hm.put("review", "/sup 31/");
        IsiImporter.processSubSup(hm);
        assertEquals("$^{31}$", hm.get("review"));

        hm.put("title", "/sub $Hello/");
        IsiImporter.processSubSup(hm);
        assertEquals("$_{\\$Hello}$", hm.get("title"));
    }

    @Test
    public void testImportEntries1() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTest1.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
        assertEquals(1, entries.size());
        BibEntry entry = entries.get(0);
        assertEquals(Optional.of("Optical properties of MgO doped LiNbO$_3$ single crystals"),
                entry.getFieldOptional("title"));
        assertEquals(
                Optional.of(
                        "James Brown and James Marc Brown and Brown, J. M. and Brown, J. and Brown, J. M. and Brown, J."),
                entry.getFieldOptional("author"));

        assertEquals("article", entry.getType());
        assertEquals(Optional.of("Optical Materials"), entry.getFieldOptional("journal"));
        assertEquals(Optional.of("2006"), entry.getFieldOptional("year"));
        assertEquals(Optional.of("28"), entry.getFieldOptional("volume"));
        assertEquals(Optional.of("5"), entry.getFieldOptional("number"));
        assertEquals(Optional.of("467--72"), entry.getFieldOptional("pages"));
    }

    @Test
    public void testImportEntries2() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTest2.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
        assertEquals(3, entries.size());
        BibEntry entry = entries.get(0);
        assertEquals(Optional.of("Optical properties of MgO doped LiNbO$_3$ single crystals"),
                entry.getFieldOptional("title"));

        assertEquals("misc", entry.getType());
        assertEquals(Optional.of("Optical Materials"), entry.getFieldOptional("journal"));
        assertEquals(Optional.of("2006"), entry.getFieldOptional("year"));
        assertEquals(Optional.of("28"), entry.getFieldOptional("volume"));
        assertEquals(Optional.of("5"), entry.getFieldOptional("number"));
        assertEquals(Optional.of("467-72"), entry.getFieldOptional("pages"));
    }

    @Test
    public void testImportEntriesINSPEC() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTestInspec.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

        assertEquals(2, entries.size());
        BibEntry a = entries.get(0);
        BibEntry b = entries.get(1);

        if (a.getFieldOptional("title").equals(
                Optional.of("Optical and photoelectric spectroscopy of photorefractive Sn$_2$P$_2$S$_6$ crystals"))) {
            BibEntry tmp = a;
            a = b;
            b = tmp;
        }

        assertEquals(
                Optional.of(
                        "Second harmonic generation of continuous wave ultraviolet light and production of beta -BaB$_2$O$_4$ optical waveguides"),
                a.getFieldOptional("title"));
        assertEquals("article", a.getType());

        assertEquals(Optional.of("Degl'Innocenti, R. and Guarino, A. and Poberaj, G. and Gunter, P."),
                a.getFieldOptional("author"));
        assertEquals(Optional.of("Applied Physics Letters"), a.getFieldOptional("journal"));
        assertEquals(Optional.of("2006"), a.getFieldOptional("year"));
        assertEquals(Optional.of("#jul#"), a.getFieldOptional("month"));
        assertEquals(Optional.of("89"), a.getFieldOptional("volume"));
        assertEquals(Optional.of("4"), a.getFieldOptional("number"));
        assertEquals(Optional.of("Lorem ipsum abstract"), a.getFieldOptional("abstract"));
        assertEquals(Optional.of("Aip"), a.getFieldOptional("publisher"));

        assertEquals(
                Optional.of("Optical and photoelectric spectroscopy of photorefractive Sn$_2$P$_2$S$_6$ crystals"),
                b.getFieldOptional("title"));
        assertEquals("article", b.getType());
    }

    @Test
    public void testImportEntriesWOS() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTestWOS.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

        assertEquals(2, entries.size());
        BibEntry a = entries.get(0);
        BibEntry b = entries.get(1);

        assertEquals(Optional.of("Optical and photoelectric spectroscopy of photorefractive Sn2P2S6 crystals"),
                a.getFieldOptional("title"));
        assertEquals(Optional.of("Optical waveguides in Sn2P2S6 by low fluence MeV He+ ion implantation"),
                b.getFieldOptional("title"));

        assertEquals(Optional.of("Journal of Physics-condensed Matter"), a.getFieldOptional("journal"));
    }

    @Test
    public void testIsiAuthorsConvert() {
        assertEquals(
                "James Brown and James Marc Brown and Brown, J. M. and Brown, J. and Brown, J. M. and Brown, J.",
                IsiImporter.isiAuthorsConvert(
                        "James Brown and James Marc Brown and Brown, J.M. and Brown, J. and Brown, J.M. and Brown, J."));

        assertEquals(
                "Joffe, Hadine and Hall, Janet E. and Gruber, Staci and Sarmiento, Ingrid A. and Cohen, Lee S. and Yurgelun-Todd, Deborah and Martin, Kathryn A.",
                IsiImporter.isiAuthorsConvert(
                        "Joffe, Hadine; Hall, Janet E; Gruber, Staci; Sarmiento, Ingrid A; Cohen, Lee S; Yurgelun-Todd, Deborah; Martin, Kathryn A"));

    }

    @Test
    public void testMonthConvert() {

        assertEquals("#jun#", IsiImporter.parseMonth("06"));
        assertEquals("#jun#", IsiImporter.parseMonth("JUN"));
        assertEquals("#jun#", IsiImporter.parseMonth("jUn"));
        assertEquals("#may#", IsiImporter.parseMonth("MAY-JUN"));
        assertEquals("#jun#", IsiImporter.parseMonth("2006 06"));
        assertEquals("#jun#", IsiImporter.parseMonth("2006 06-07"));
        assertEquals("#jul#", IsiImporter.parseMonth("2006 07 03"));
        assertEquals("#may#", IsiImporter.parseMonth("2006 May-Jun"));
    }

    @Test
    public void testIsiAuthorConvert() {
        assertEquals("James Brown", IsiImporter.isiAuthorConvert("James Brown"));
        assertEquals("James Marc Brown", IsiImporter.isiAuthorConvert("James Marc Brown"));
        assertEquals("Brown, J. M.", IsiImporter.isiAuthorConvert("Brown, J.M."));
        assertEquals("Brown, J.", IsiImporter.isiAuthorConvert("Brown, J."));
        assertEquals("Brown, J. M.", IsiImporter.isiAuthorConvert("Brown, JM"));
        assertEquals("Brown, J.", IsiImporter.isiAuthorConvert("Brown, J"));
        assertEquals("Brown, James", IsiImporter.isiAuthorConvert("Brown, James"));
        assertEquals("Hall, Janet E.", IsiImporter.isiAuthorConvert("Hall, Janet E"));
        assertEquals("", IsiImporter.isiAuthorConvert(""));
    }

    @Test
    public void testImportIEEEExport() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IEEEImport1.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

        assertEquals(1, entries.size());
        BibEntry a = entries.get(0);
        assertEquals("article", a.getType());
        assertEquals(Optional.of("Geoscience and Remote Sensing Letters, IEEE"), a.getFieldOptional("journal"));
        assertEquals(Optional.of("Improving Urban Road Extraction in High-Resolution "
                + "Images Exploiting Directional Filtering, Perceptual " + "Grouping, and Simple Topological Concepts"),
                a.getFieldOptional("title"));
        assertEquals(Optional.of("4"), a.getFieldOptional("volume"));
        assertEquals(Optional.of("3"), a.getFieldOptional("number"));
        assertEquals(Optional.of("1545-598X"), a.getFieldOptional("SN"));
        assertEquals(Optional.of("387--391"), a.getFieldOptional("pages"));
        assertEquals(Optional.of("Gamba, P. and Dell'Acqua, F. and Lisini, G."), a.getFieldOptional("author"));
        assertEquals(Optional.of("2006"), a.getFieldOptional("year"));
        assertEquals(Optional.of("Perceptual grouping, street extraction, urban remote sensing"),
                a.getFieldOptional("keywords"));
        assertEquals(Optional.of("Lorem ipsum abstract"), a.getFieldOptional("abstract"));
    }

    @Test
    public void testIEEEImport() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IEEEImport1.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

        assertEquals(1, entries.size());
        BibEntry a = entries.get(0);

        assertEquals("article", a.getType());
        assertEquals(Optional.of("Geoscience and Remote Sensing Letters, IEEE"), a.getFieldOptional("journal"));
        assertEquals(
                Optional.of(
                        "Improving Urban Road Extraction in High-Resolution Images Exploiting Directional Filtering, Perceptual Grouping, and Simple Topological Concepts"),
                a.getFieldOptional("title"));
        assertEquals(Optional.of("4"), a.getFieldOptional("volume"));
        assertEquals(Optional.of("3"), a.getFieldOptional("number"));
        assertEquals(Optional.of("1545-598X"), a.getFieldOptional("SN"));
        assertEquals(Optional.of("387--391"), a.getFieldOptional("pages"));
        assertEquals(Optional.of("Gamba, P. and Dell'Acqua, F. and Lisini, G."), a.getFieldOptional("author"));
        assertEquals(Optional.of("2006"), a.getFieldOptional("year"));
        assertEquals(Optional.of("Perceptual grouping, street extraction, urban remote sensing"),
                a.getFieldOptional("keywords"));
        assertEquals(Optional.of("Lorem ipsum abstract"), a.getFieldOptional("abstract"));
    }

    @Test
    public void testImportEntriesMedline() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTestMedline.isi").toURI());

        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

        assertEquals(2, entries.size());
        BibEntry a = entries.get(0);
        BibEntry b = entries.get(1);

        assertEquals(
                Optional.of("Effects of modafinil on cognitive performance and alertness during sleep deprivation."),
                a.getFieldOptional("title"));

        assertEquals(Optional.of("Wesensten, Nancy J."), a.getFieldOptional("author"));
        assertEquals(Optional.of("Curr Pharm Des"), a.getFieldOptional("journal"));
        assertEquals(Optional.of("2006"), a.getFieldOptional("year"));
        assertEquals(Optional.empty(), a.getFieldOptional("month"));
        assertEquals(Optional.of("12"), a.getFieldOptional("volume"));
        assertEquals(Optional.of("20"), a.getFieldOptional("number"));
        assertEquals(Optional.of("2457--71"), a.getFieldOptional("pages"));
        assertEquals("article", a.getType());

        assertEquals(
                Optional.of(
                        "Estrogen therapy selectively enhances prefrontal cognitive processes: a randomized, double-blind, placebo-controlled study with functional magnetic resonance imaging in perimenopausal and recently postmenopausal women."),
                b.getFieldOptional("title"));
        assertEquals(
                Optional.of(
                        "Joffe, Hadine and Hall, Janet E. and Gruber, Staci and Sarmiento, Ingrid A. and Cohen, Lee S. and Yurgelun-Todd, Deborah and Martin, Kathryn A."),
                b.getFieldOptional("author"));
        assertEquals(Optional.of("2006"), b.getFieldOptional("year"));
        assertEquals(Optional.of("#may#"), b.getFieldOptional("month"));
        assertEquals(Optional.of("13"), b.getFieldOptional("volume"));
        assertEquals(Optional.of("3"), b.getFieldOptional("number"));
        assertEquals(Optional.of("411--22"), b.getFieldOptional("pages"));
        assertEquals("article", b.getType());
    }

    @Test
    public void testImportEntriesEmpty() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTestEmpty.isi").toURI());

        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

        assertEquals(1, entries.size());
    }
}
