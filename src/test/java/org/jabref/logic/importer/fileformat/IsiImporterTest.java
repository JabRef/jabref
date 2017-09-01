package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.util.FileExtensions;
import org.jabref.model.entry.BibEntry;

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


    @Test
    public void testParseMonthException() {
        IsiImporter.parseMonth("20l06 06-07");
    }

    @Test
    public void testGetFormatName() {
        assertEquals("ISI", importer.getName());
    }

    @Test
    public void testGetCLIId() {
        assertEquals("isi", importer.getId());
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
            assertTrue(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testIsRecognizedFormatRejected() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("IsiImporterTestEmpty.isi");

        for (String str : list) {
            Path file = Paths.get(IsiImporterTest.class.getResource(str).toURI());
            assertFalse(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
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
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(1, entries.size());
        BibEntry entry = entries.get(0);
        assertEquals(Optional.of("Optical properties of MgO doped LiNbO$_3$ single crystals"),
                entry.getField("title"));
        assertEquals(
                Optional.of(
                        "James Brown and James Marc Brown and Brown, J. M. and Brown, J. and Brown, J. M. and Brown, J."),
                entry.getField("author"));

        assertEquals("article", entry.getType());
        assertEquals(Optional.of("Optical Materials"), entry.getField("journal"));
        assertEquals(Optional.of("2006"), entry.getField("year"));
        assertEquals(Optional.of("28"), entry.getField("volume"));
        assertEquals(Optional.of("5"), entry.getField("number"));
        assertEquals(Optional.of("467--72"), entry.getField("pages"));
    }

    @Test
    public void testImportEntries2() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTest2.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(3, entries.size());
        BibEntry entry = entries.get(0);
        assertEquals(Optional.of("Optical properties of MgO doped LiNbO$_3$ single crystals"),
                entry.getField("title"));

        assertEquals("misc", entry.getType());
        assertEquals(Optional.of("Optical Materials"), entry.getField("journal"));
        assertEquals(Optional.of("2006"), entry.getField("year"));
        assertEquals(Optional.of("28"), entry.getField("volume"));
        assertEquals(Optional.of("5"), entry.getField("number"));
        assertEquals(Optional.of("467-72"), entry.getField("pages"));
    }

    @Test
    public void testImportEntriesINSPEC() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTestInspec.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(2, entries.size());
        BibEntry a = entries.get(0);
        BibEntry b = entries.get(1);

        if (a.getField("title").equals(
                Optional.of("Optical and photoelectric spectroscopy of photorefractive Sn$_2$P$_2$S$_6$ crystals"))) {
            BibEntry tmp = a;
            a = b;
            b = tmp;
        }

        assertEquals(
                Optional.of(
                        "Second harmonic generation of continuous wave ultraviolet light and production of beta -BaB$_2$O$_4$ optical waveguides"),
                a.getField("title"));
        assertEquals("article", a.getType());

        assertEquals(Optional.of("Degl'Innocenti, R. and Guarino, A. and Poberaj, G. and Gunter, P."),
                a.getField("author"));
        assertEquals(Optional.of("Applied Physics Letters"), a.getField("journal"));
        assertEquals(Optional.of("2006"), a.getField("year"));
        assertEquals(Optional.of("#jul#"), a.getField("month"));
        assertEquals(Optional.of("89"), a.getField("volume"));
        assertEquals(Optional.of("4"), a.getField("number"));
        assertEquals(Optional.of("Lorem ipsum abstract"), a.getField("abstract"));
        assertEquals(Optional.of("Aip"), a.getField("publisher"));

        assertEquals(
                Optional.of("Optical and photoelectric spectroscopy of photorefractive Sn$_2$P$_2$S$_6$ crystals"),
                b.getField("title"));
        assertEquals("article", b.getType());
    }

    @Test
    public void testImportEntriesWOS() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTestWOS.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(2, entries.size());
        BibEntry a = entries.get(0);
        BibEntry b = entries.get(1);

        assertEquals(Optional.of("Optical and photoelectric spectroscopy of photorefractive Sn2P2S6 crystals"),
                a.getField("title"));
        assertEquals(Optional.of("Optical waveguides in Sn2P2S6 by low fluence MeV He+ ion implantation"),
                b.getField("title"));

        assertEquals(Optional.of("Journal of Physics-condensed Matter"), a.getField("journal"));
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
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(1, entries.size());
        BibEntry a = entries.get(0);
        assertEquals("article", a.getType());
        assertEquals(Optional.of("Geoscience and Remote Sensing Letters, IEEE"), a.getField("journal"));
        assertEquals(Optional.of("Improving Urban Road Extraction in High-Resolution "
                + "Images Exploiting Directional Filtering, Perceptual " + "Grouping, and Simple Topological Concepts"),
                a.getField("title"));
        assertEquals(Optional.of("4"), a.getField("volume"));
        assertEquals(Optional.of("3"), a.getField("number"));
        assertEquals(Optional.of("1545-598X"), a.getField("SN"));
        assertEquals(Optional.of("387--391"), a.getField("pages"));
        assertEquals(Optional.of("Gamba, P. and Dell'Acqua, F. and Lisini, G."), a.getField("author"));
        assertEquals(Optional.of("2006"), a.getField("year"));
        assertEquals(Optional.of("Perceptual grouping, street extraction, urban remote sensing"),
                a.getField("keywords"));
        assertEquals(Optional.of("Lorem ipsum abstract"), a.getField("abstract"));
    }

    @Test
    public void testIEEEImport() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IEEEImport1.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(1, entries.size());
        BibEntry a = entries.get(0);

        assertEquals("article", a.getType());
        assertEquals(Optional.of("Geoscience and Remote Sensing Letters, IEEE"), a.getField("journal"));
        assertEquals(
                Optional.of(
                        "Improving Urban Road Extraction in High-Resolution Images Exploiting Directional Filtering, Perceptual Grouping, and Simple Topological Concepts"),
                a.getField("title"));
        assertEquals(Optional.of("4"), a.getField("volume"));
        assertEquals(Optional.of("3"), a.getField("number"));
        assertEquals(Optional.of("1545-598X"), a.getField("SN"));
        assertEquals(Optional.of("387--391"), a.getField("pages"));
        assertEquals(Optional.of("Gamba, P. and Dell'Acqua, F. and Lisini, G."), a.getField("author"));
        assertEquals(Optional.of("2006"), a.getField("year"));
        assertEquals(Optional.of("Perceptual grouping, street extraction, urban remote sensing"),
                a.getField("keywords"));
        assertEquals(Optional.of("Lorem ipsum abstract"), a.getField("abstract"));
    }

    @Test
    public void testImportEntriesMedline() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTestMedline.isi").toURI());

        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(2, entries.size());
        BibEntry a = entries.get(0);
        BibEntry b = entries.get(1);

        assertEquals(
                Optional.of("Effects of modafinil on cognitive performance and alertness during sleep deprivation."),
                a.getField("title"));

        assertEquals(Optional.of("Wesensten, Nancy J."), a.getField("author"));
        assertEquals(Optional.of("Curr Pharm Des"), a.getField("journal"));
        assertEquals(Optional.of("2006"), a.getField("year"));
        assertEquals(Optional.empty(), a.getField("month"));
        assertEquals(Optional.of("12"), a.getField("volume"));
        assertEquals(Optional.of("20"), a.getField("number"));
        assertEquals(Optional.of("2457--71"), a.getField("pages"));
        assertEquals("article", a.getType());

        assertEquals(
                Optional.of(
                        "Estrogen therapy selectively enhances prefrontal cognitive processes: a randomized, double-blind, placebo-controlled study with functional magnetic resonance imaging in perimenopausal and recently postmenopausal women."),
                b.getField("title"));
        assertEquals(
                Optional.of(
                        "Joffe, Hadine and Hall, Janet E. and Gruber, Staci and Sarmiento, Ingrid A. and Cohen, Lee S. and Yurgelun-Todd, Deborah and Martin, Kathryn A."),
                b.getField("author"));
        assertEquals(Optional.of("2006"), b.getField("year"));
        assertEquals(Optional.of("#may#"), b.getField("month"));
        assertEquals(Optional.of("13"), b.getField("volume"));
        assertEquals(Optional.of("3"), b.getField("number"));
        assertEquals(Optional.of("411--22"), b.getField("pages"));
        assertEquals("article", b.getType());
    }

    @Test
    public void testImportEntriesEmpty() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTestEmpty.isi").toURI());

        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(1, entries.size());
    }
}
