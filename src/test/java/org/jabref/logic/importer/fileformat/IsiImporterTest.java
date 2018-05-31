package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.util.FileType;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test cases for the IsiImporter
 */
public class IsiImporterTest {

    private final IsiImporter importer = new IsiImporter();

    private static final String FILE_ENDING = ".isi";

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("IsiImporterTest")
                && !name.contains("Empty")
                && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    private static Stream<String> invalidFileNames() throws IOException {
        Predicate<String> fileName = name -> !name.startsWith("IsiImporterTest");
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

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
        assertEquals(FileType.ISI, importer.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Importer for the ISI Web of Science, INSPEC and Medline format.",
                importer.getDescription());
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testIsRecognizedFormatAccepted(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(importer, fileName);
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    public void testIsRecognizedFormatRejected(String fileName) throws IOException {
        ImporterTestEngine.testIsNotRecognizedFormat(importer, fileName);
    }

    @Test
    public void testProcessSubSup() {
        HashMap<String, String> subs = new HashMap<>();

        subs.put("title", "/sub 3/");
        IsiImporter.processSubSup(subs);
        assertEquals("$_3$", subs.get("title"));

        subs.put("title", "/sub   3   /");
        IsiImporter.processSubSup(subs);
        assertEquals("$_3$", subs.get("title"));

        subs.put("title", "/sub 31/");
        IsiImporter.processSubSup(subs);
        assertEquals("$_{31}$", subs.get("title"));

        subs.put("abstract", "/sub 3/");
        IsiImporter.processSubSup(subs);
        assertEquals("$_3$", subs.get("abstract"));

        subs.put("review", "/sub 31/");
        IsiImporter.processSubSup(subs);
        assertEquals("$_{31}$", subs.get("review"));

        subs.put("title", "/sup 3/");
        IsiImporter.processSubSup(subs);
        assertEquals("$^3$", subs.get("title"));

        subs.put("title", "/sup 31/");
        IsiImporter.processSubSup(subs);
        assertEquals("$^{31}$", subs.get("title"));

        subs.put("abstract", "/sup 3/");
        IsiImporter.processSubSup(subs);
        assertEquals("$^3$", subs.get("abstract"));

        subs.put("review", "/sup 31/");
        IsiImporter.processSubSup(subs);
        assertEquals("$^{31}$", subs.get("review"));

        subs.put("title", "/sub $Hello/");
        IsiImporter.processSubSup(subs);
        assertEquals("$_{\\$Hello}$", subs.get("title"));
    }

    @Test
    public void testImportEntries1() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTest1.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntry entry = entries.get(0);

        assertEquals(1, entries.size());
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
        BibEntry entry = entries.get(0);

        assertEquals(3, entries.size());
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

        BibEntry first = entries.get(0);
        BibEntry second = entries.get(1);

        if (first.getField("title").equals(
                Optional.of("Optical and photoelectric spectroscopy of photorefractive Sn$_2$P$_2$S$_6$ crystals"))) {
            BibEntry tmp = first;
            first = second;
            second = tmp;
        }

        assertEquals(2, entries.size());
        assertEquals(
                Optional.of(
                        "Second harmonic generation of continuous wave ultraviolet light and production of beta -BaB$_2$O$_4$ optical waveguides"),
                first.getField("title"));
        assertEquals("article", first.getType());

        assertEquals(Optional.of("Degl'Innocenti, R. and Guarino, A. and Poberaj, G. and Gunter, P."),
                first.getField("author"));
        assertEquals(Optional.of("Applied Physics Letters"), first.getField("journal"));
        assertEquals(Optional.of("2006"), first.getField("year"));
        assertEquals(Optional.of("#jul#"), first.getField("month"));
        assertEquals(Optional.of("89"), first.getField("volume"));
        assertEquals(Optional.of("4"), first.getField("number"));
        assertEquals(Optional.of("Lorem ipsum abstract"), first.getField("abstract"));
        assertEquals(Optional.of("Aip"), first.getField("publisher"));
        assertEquals(
                Optional.of("Optical and photoelectric spectroscopy of photorefractive Sn$_2$P$_2$S$_6$ crystals"),
                second.getField("title"));
        assertEquals("article", second.getType());
    }

    @Test
    public void testImportEntriesWOS() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTestWOS.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        BibEntry first = entries.get(0);
        BibEntry second = entries.get(1);

        assertEquals(2, entries.size());

        assertEquals(Optional.of("Optical and photoelectric spectroscopy of photorefractive Sn2P2S6 crystals"),
                first.getField("title"));
        assertEquals(Optional.of("Optical waveguides in Sn2P2S6 by low fluence MeV He+ ion implantation"),
                second.getField("title"));

        assertEquals(Optional.of("Journal of Physics-condensed Matter"), first.getField("journal"));
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
        BibEntry entry = entries.get(0);

        assertEquals(1, entries.size());
        assertEquals("article", entry.getType());
        assertEquals(Optional.of("Geoscience and Remote Sensing Letters, IEEE"), entry.getField("journal"));
        assertEquals(Optional.of("Improving Urban Road Extraction in High-Resolution "
                + "Images Exploiting Directional Filtering, Perceptual " + "Grouping, and Simple Topological Concepts"),
                entry.getField("title"));
        assertEquals(Optional.of("4"), entry.getField("volume"));
        assertEquals(Optional.of("3"), entry.getField("number"));
        assertEquals(Optional.of("1545-598X"), entry.getField("SN"));
        assertEquals(Optional.of("387--391"), entry.getField("pages"));
        assertEquals(Optional.of("Gamba, P. and Dell'Acqua, F. and Lisini, G."), entry.getField("author"));
        assertEquals(Optional.of("2006"), entry.getField("year"));
        assertEquals(Optional.of("Perceptual grouping, street extraction, urban remote sensing"),
                entry.getField("keywords"));
        assertEquals(Optional.of("Lorem ipsum abstract"), entry.getField("abstract"));
    }

    @Test
    public void testIEEEImport() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IEEEImport1.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntry entry = entries.get(0);

        assertEquals(1, entries.size());
        assertEquals("article", entry.getType());
        assertEquals(Optional.of("Geoscience and Remote Sensing Letters, IEEE"), entry.getField("journal"));
        assertEquals(
                Optional.of(
                        "Improving Urban Road Extraction in High-Resolution Images Exploiting Directional Filtering, Perceptual Grouping, and Simple Topological Concepts"),
                entry.getField("title"));
        assertEquals(Optional.of("4"), entry.getField("volume"));
        assertEquals(Optional.of("3"), entry.getField("number"));
        assertEquals(Optional.of("1545-598X"), entry.getField("SN"));
        assertEquals(Optional.of("387--391"), entry.getField("pages"));
        assertEquals(Optional.of("Gamba, P. and Dell'Acqua, F. and Lisini, G."), entry.getField("author"));
        assertEquals(Optional.of("2006"), entry.getField("year"));
        assertEquals(Optional.of("Perceptual grouping, street extraction, urban remote sensing"),
                entry.getField("keywords"));
        assertEquals(Optional.of("Lorem ipsum abstract"), entry.getField("abstract"));
    }

    @Test
    public void testImportEntriesMedline() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTestMedline.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        BibEntry first = entries.get(0);
        BibEntry second = entries.get(1);

        assertEquals(2, entries.size());
        assertEquals(
                Optional.of("Effects of modafinil on cognitive performance and alertness during sleep deprivation."),
                first.getField("title"));
        assertEquals(Optional.of("Wesensten, Nancy J."), first.getField("author"));
        assertEquals(Optional.of("Curr Pharm Des"), first.getField("journal"));
        assertEquals(Optional.of("2006"), first.getField("year"));
        assertEquals(Optional.empty(), first.getField("month"));
        assertEquals(Optional.of("12"), first.getField("volume"));
        assertEquals(Optional.of("20"), first.getField("number"));
        assertEquals(Optional.of("2457--71"), first.getField("pages"));
        assertEquals("article", first.getType());
        assertEquals(
                Optional.of(
                        "Estrogen therapy selectively enhances prefrontal cognitive processes: a randomized, double-blind, placebo-controlled study with functional magnetic resonance imaging in perimenopausal and recently postmenopausal women."),
                second.getField("title"));
        assertEquals(
                Optional.of(
                        "Joffe, Hadine and Hall, Janet E. and Gruber, Staci and Sarmiento, Ingrid A. and Cohen, Lee S. and Yurgelun-Todd, Deborah and Martin, Kathryn A."),
                second.getField("author"));
        assertEquals(Optional.of("2006"), second.getField("year"));
        assertEquals(Optional.of("#may#"), second.getField("month"));
        assertEquals(Optional.of("13"), second.getField("volume"));
        assertEquals(Optional.of("3"), second.getField("number"));
        assertEquals(Optional.of("411--22"), second.getField("pages"));
        assertEquals("article", second.getType());
    }

    @Test
    public void testImportEntriesEmpty() throws IOException, URISyntaxException {
        Path file = Paths.get(IsiImporterTest.class.getResource("IsiImporterTestEmpty.isi").toURI());

        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(1, entries.size());
    }
}
