package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IsiImporterTest {

    private static final String FILE_ENDING = ".isi";
    private final IsiImporter importer = new IsiImporter();

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
        assertEquals(StandardFileType.ISI, importer.getFileType());
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
        HashMap<Field, String> subs = new HashMap<>();

        subs.put(StandardField.TITLE, "/sub 3/");
        IsiImporter.processSubSup(subs);
        assertEquals("$_3$", subs.get(StandardField.TITLE));

        subs.put(StandardField.TITLE, "/sub   3   /");
        IsiImporter.processSubSup(subs);
        assertEquals("$_3$", subs.get(StandardField.TITLE));

        subs.put(StandardField.TITLE, "/sub 31/");
        IsiImporter.processSubSup(subs);
        assertEquals("$_{31}$", subs.get(StandardField.TITLE));

        subs.put(StandardField.ABSTRACT, "/sub 3/");
        IsiImporter.processSubSup(subs);
        assertEquals("$_3$", subs.get(StandardField.ABSTRACT));

        subs.put(StandardField.REVIEW, "/sub 31/");
        IsiImporter.processSubSup(subs);
        assertEquals("$_{31}$", subs.get(StandardField.REVIEW));

        subs.put(StandardField.TITLE, "/sup 3/");
        IsiImporter.processSubSup(subs);
        assertEquals("$^3$", subs.get(StandardField.TITLE));

        subs.put(StandardField.TITLE, "/sup 31/");
        IsiImporter.processSubSup(subs);
        assertEquals("$^{31}$", subs.get(StandardField.TITLE));

        subs.put(StandardField.ABSTRACT, "/sup 3/");
        IsiImporter.processSubSup(subs);
        assertEquals("$^3$", subs.get(StandardField.ABSTRACT));

        subs.put(StandardField.REVIEW, "/sup 31/");
        IsiImporter.processSubSup(subs);
        assertEquals("$^{31}$", subs.get(StandardField.REVIEW));

        subs.put(StandardField.TITLE, "/sub $Hello/");
        IsiImporter.processSubSup(subs);
        assertEquals("$_{\\$Hello}$", subs.get(StandardField.TITLE));
    }

    @Test
    public void testImportEntries1() throws IOException, URISyntaxException {
        Path file = Path.of(IsiImporterTest.class.getResource("IsiImporterTest1.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntry entry = entries.get(0);

        assertEquals(1, entries.size());
        assertEquals(Optional.of("Optical properties of MgO doped LiNbO$_3$ single crystals"),
                entry.getField(StandardField.TITLE));
        assertEquals(
                Optional.of(
                        "James Brown and James Marc Brown and Brown, J. M. and Brown, J. and Brown, J. M. and Brown, J."),
                entry.getField(StandardField.AUTHOR));
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("Optical Materials"), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("2006"), entry.getField(StandardField.YEAR));
        assertEquals(Optional.of("28"), entry.getField(StandardField.VOLUME));
        assertEquals(Optional.of("5"), entry.getField(StandardField.NUMBER));
        assertEquals(Optional.of("467--72"), entry.getField(StandardField.PAGES));
    }

    @Test
    public void testImportEntries2() throws IOException, URISyntaxException {
        Path file = Path.of(IsiImporterTest.class.getResource("IsiImporterTest2.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntry entry = entries.get(0);

        assertEquals(3, entries.size());
        assertEquals(Optional.of("Optical properties of MgO doped LiNbO$_3$ single crystals"),
                entry.getField(StandardField.TITLE));
        assertEquals(StandardEntryType.Misc, entry.getType());
        assertEquals(Optional.of("Optical Materials"), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("2006"), entry.getField(StandardField.YEAR));
        assertEquals(Optional.of("28"), entry.getField(StandardField.VOLUME));
        assertEquals(Optional.of("5"), entry.getField(StandardField.NUMBER));
        assertEquals(Optional.of("467-72"), entry.getField(StandardField.PAGES));
    }

    @Test
    public void testImportEntriesINSPEC() throws IOException, URISyntaxException {
        Path file = Path.of(IsiImporterTest.class.getResource("IsiImporterTestInspec.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        BibEntry first = entries.get(0);
        BibEntry second = entries.get(1);

        if (first.getField(StandardField.TITLE).equals(
                Optional.of("Optical and photoelectric spectroscopy of photorefractive Sn$_2$P$_2$S$_6$ crystals"))) {
            BibEntry tmp = first;
            first = second;
            second = tmp;
        }

        assertEquals(2, entries.size());
        assertEquals(
                Optional.of(
                        "Second harmonic generation of continuous wave ultraviolet light and production of beta -BaB$_2$O$_4$ optical waveguides"),
                first.getField(StandardField.TITLE));
        assertEquals(StandardEntryType.Article, first.getType());

        assertEquals(Optional.of("Degl'Innocenti, R. and Guarino, A. and Poberaj, G. and Gunter, P."),
                first.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Applied Physics Letters"), first.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("2006"), first.getField(StandardField.YEAR));
        assertEquals(Optional.of(Month.JULY), first.getMonth());
        assertEquals(Optional.of("89"), first.getField(StandardField.VOLUME));
        assertEquals(Optional.of("4"), first.getField(StandardField.NUMBER));
        assertEquals(Optional.of("Lorem ipsum abstract"), first.getField(StandardField.ABSTRACT));
        assertEquals(Optional.of("Aip"), first.getField(StandardField.PUBLISHER));
        assertEquals(
                Optional.of("Optical and photoelectric spectroscopy of photorefractive Sn$_2$P$_2$S$_6$ crystals"),
                second.getField(StandardField.TITLE));
        assertEquals(StandardEntryType.Article, second.getType());
    }

    @Test
    public void testImportEntriesWOS() throws IOException, URISyntaxException {
        Path file = Path.of(IsiImporterTest.class.getResource("IsiImporterTestWOS.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        BibEntry first = entries.get(0);
        BibEntry second = entries.get(1);

        assertEquals(2, entries.size());

        assertEquals(Optional.of("Optical and photoelectric spectroscopy of photorefractive Sn2P2S6 crystals"),
                first.getField(StandardField.TITLE));
        assertEquals(Optional.of("Optical waveguides in Sn2P2S6 by low fluence MeV He+ ion implantation"),
                second.getField(StandardField.TITLE));

        assertEquals(Optional.of("Journal of Physics-condensed Matter"), first.getField(StandardField.JOURNAL));
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
        Path file = Path.of(IsiImporterTest.class.getResource("IEEEImport1.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntry entry = entries.get(0);

        assertEquals(1, entries.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("Geoscience and Remote Sensing Letters, IEEE"), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("Improving Urban Road Extraction in High-Resolution "
                        + "Images Exploiting Directional Filtering, Perceptual " + "Grouping, and Simple Topological Concepts"),
                entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("4"), entry.getField(StandardField.VOLUME));
        assertEquals(Optional.of("3"), entry.getField(StandardField.NUMBER));
        assertEquals(Optional.of("1545-598X"), entry.getField(new UnknownField("SN")));
        assertEquals(Optional.of("387--391"), entry.getField(StandardField.PAGES));
        assertEquals(Optional.of("Gamba, P. and Dell'Acqua, F. and Lisini, G."), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("2006"), entry.getField(StandardField.YEAR));
        assertEquals(Optional.of("Perceptual grouping, street extraction, urban remote sensing"),
                entry.getField(StandardField.KEYWORDS));
        assertEquals(Optional.of("Lorem ipsum abstract"), entry.getField(StandardField.ABSTRACT));
    }

    @Test
    public void testIEEEImport() throws IOException, URISyntaxException {
        Path file = Path.of(IsiImporterTest.class.getResource("IEEEImport1.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntry entry = entries.get(0);

        assertEquals(1, entries.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("Geoscience and Remote Sensing Letters, IEEE"), entry.getField(StandardField.JOURNAL));
        assertEquals(
                Optional.of(
                        "Improving Urban Road Extraction in High-Resolution Images Exploiting Directional Filtering, Perceptual Grouping, and Simple Topological Concepts"),
                entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("4"), entry.getField(StandardField.VOLUME));
        assertEquals(Optional.of("3"), entry.getField(StandardField.NUMBER));
        assertEquals(Optional.of("1545-598X"), entry.getField(new UnknownField("SN")));
        assertEquals(Optional.of("387--391"), entry.getField(StandardField.PAGES));
        assertEquals(Optional.of("Gamba, P. and Dell'Acqua, F. and Lisini, G."), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("2006"), entry.getField(StandardField.YEAR));
        assertEquals(Optional.of("Perceptual grouping, street extraction, urban remote sensing"),
                entry.getField(StandardField.KEYWORDS));
        assertEquals(Optional.of("Lorem ipsum abstract"), entry.getField(StandardField.ABSTRACT));
    }

    @Test
    public void testImportEntriesMedline() throws IOException, URISyntaxException {
        Path file = Path.of(IsiImporterTest.class.getResource("IsiImporterTestMedline.isi").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        BibEntry first = entries.get(0);
        BibEntry second = entries.get(1);

        assertEquals(2, entries.size());
        assertEquals(
                Optional.of("Effects of modafinil on cognitive performance and alertness during sleep deprivation."),
                first.getField(StandardField.TITLE));
        assertEquals(Optional.of("Wesensten, Nancy J."), first.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Curr Pharm Des"), first.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("2006"), first.getField(StandardField.YEAR));
        assertEquals(Optional.empty(), first.getField(StandardField.MONTH));
        assertEquals(Optional.of("12"), first.getField(StandardField.VOLUME));
        assertEquals(Optional.of("20"), first.getField(StandardField.NUMBER));
        assertEquals(Optional.of("2457--71"), first.getField(StandardField.PAGES));
        assertEquals(StandardEntryType.Article, first.getType());
        assertEquals(
                Optional.of(
                        "Estrogen therapy selectively enhances prefrontal cognitive processes: a randomized, double-blind, placebo-controlled study with functional magnetic resonance imaging in perimenopausal and recently postmenopausal women."),
                second.getField(StandardField.TITLE));
        assertEquals(
                Optional.of(
                        "Joffe, Hadine and Hall, Janet E. and Gruber, Staci and Sarmiento, Ingrid A. and Cohen, Lee S. and Yurgelun-Todd, Deborah and Martin, Kathryn A."),
                second.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("2006"), second.getField(StandardField.YEAR));
        assertEquals(Optional.of(Month.MAY), second.getMonth());
        assertEquals(Optional.of("13"), second.getField(StandardField.VOLUME));
        assertEquals(Optional.of("3"), second.getField(StandardField.NUMBER));
        assertEquals(Optional.of("411--22"), second.getField(StandardField.PAGES));
        assertEquals(StandardEntryType.Article, second.getType());
    }

    @Test
    public void testImportEntriesEmpty() throws IOException, URISyntaxException {
        Path file = Path.of(IsiImporterTest.class.getResource("IsiImporterTestEmpty.isi").toURI());

        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(1, entries.size());
    }
}
