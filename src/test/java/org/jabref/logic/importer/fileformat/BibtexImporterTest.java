package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * This class tests the BibtexImporter.
 * <p>
 * The tests for writing can be found at {@link org.jabref.logic.exporter.BibtexDatabaseWriterTest}
 */
public class BibtexImporterTest {

    private BibtexImporter importer;

    @BeforeEach
    public void setUp() {
        importer = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
    }

    @Test
    public void testIsRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Path.of(BibtexImporterTest.class.getResource("BibtexImporter.examples.bib").toURI());
        assertTrue(importer.isRecognizedFormat(file));
    }

    @Test
    public void testImportEntries() throws IOException, URISyntaxException {
        Path file = Path.of(BibtexImporterTest.class.getResource("BibtexImporter.examples.bib").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();

        assertEquals(4, bibEntries.size());

        for (BibEntry entry : bibEntries) {
            if (entry.getCitationKey().get().equals("aksin")) {
                assertEquals(
                        Optional.of(
                                "Aks{\\i}n, {\\\"O}zge and T{\\\"u}rkmen, Hayati and Artok, Levent and {\\c{C}}etinkaya, "
                                        + "Bekir and Ni, Chaoying and B{\\\"u}y{\\\"u}kg{\\\"u}ng{\\\"o}r, Orhan and {\\\"O}zkal, Erhan"),
                        entry.getField(StandardField.AUTHOR));
                assertEquals(Optional.of("aksin"), entry.getCitationKey());
                assertEquals(Optional.of("2006"), entry.getField(StandardField.DATE));
                assertEquals(Optional.of("Effect of immobilization on catalytic characteristics"), entry.getField(new UnknownField("indextitle")));
                assertEquals(Optional.of("#jomch#"), entry.getField(StandardField.JOURNAL));
                assertEquals(Optional.of("13"), entry.getField(StandardField.NUMBER));
                assertEquals(Optional.of("3027-3036"), entry.getField(StandardField.PAGES));
                assertEquals(Optional
                                .of("Effect of immobilization on catalytic characteristics of saturated {Pd-N}-heterocyclic "
                                        + "carbenes in {Mizoroki-Heck} reactions"),
                        entry.getField(StandardField.TITLE));
                assertEquals(Optional.of("691"), entry.getField(StandardField.VOLUME));
            } else if (entry.getCitationKey().get().equals("stdmodel")) {
                assertEquals(Optional
                                .of("A \\texttt{set} with three members discussing the standard model of particle physics. "
                                        + "The \\texttt{crossref} field in the \\texttt{@set} entry and the \\texttt{entryset} field in "
                                        + "each set member entry is needed only when using BibTeX as the backend"),
                        entry.getField(StandardField.ANNOTATION));
                assertEquals(Optional.of("stdmodel"), entry.getCitationKey());
                assertEquals(Optional.of("glashow,weinberg,salam"), entry.getField(StandardField.ENTRYSET));
            } else if (entry.getCitationKey().get().equals("set")) {
                assertEquals(Optional
                                .of("A \\texttt{set} with three members. The \\texttt{crossref} field in the \\texttt{@set} "
                                        + "entry and the \\texttt{entryset} field in each set member entry is needed only when using "
                                        + "BibTeX as the backend"),
                        entry.getField(StandardField.ANNOTATION));
                assertEquals(Optional.of("set"), entry.getCitationKey());
                assertEquals(Optional.of("herrmann,aksin,yoon"), entry.getField(StandardField.ENTRYSET));
            } else if (entry.getCitationKey().get().equals("Preissel2016")) {
                assertEquals(Optional.of("Heidelberg"), entry.getField(StandardField.ADDRESS));
                assertEquals(Optional.of("Preißel, René"), entry.getField(StandardField.AUTHOR));
                assertEquals(Optional.of("Preissel2016"), entry.getCitationKey());
                assertEquals(Optional.of("3., aktualisierte und erweiterte Auflage"),
                        entry.getField(StandardField.EDITION));
                assertEquals(Optional.of("978-3-86490-311-3"), entry.getField(StandardField.ISBN));
                assertEquals(Optional.of("Versionsverwaltung"), entry.getField(StandardField.KEYWORDS));
                assertEquals(Optional.of("XX, 327 Seiten"), entry.getField(StandardField.PAGES));
                assertEquals(Optional.of("dpunkt.verlag"), entry.getField(StandardField.PUBLISHER));
                assertEquals(Optional.of("Git: dezentrale Versionsverwaltung im Team : Grundlagen und Workflows"),
                        entry.getField(StandardField.TITLE));
                assertEquals(Optional.of("http://d-nb.info/107601965X"), entry.getField(StandardField.URL));
                assertEquals(Optional.of("2016"), entry.getField(StandardField.YEAR));
            }
        }
    }

    @Test
    public void testGetFormatName() {
        assertEquals("BibTeX", importer.getName());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(StandardFileType.BIBTEX_DB, importer.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals(
                "This importer enables `--importToOpen someEntry.bib`",
                importer.getDescription());
    }

    @Test
    public void testRecognizesDatabaseID() throws Exception {
        Path file = Path.of(BibtexImporterTest.class.getResource("AutosavedSharedDatabase.bib").toURI());
        String sharedDatabaseID = importer.importDatabase(file).getDatabase().getSharedDatabaseID().get();
        assertEquals("13ceoc8dm42f5g1iitao3dj2ap", sharedDatabaseID);
    }

    static Stream<Arguments> testParsingOfEncodedFileWithHeader() {
        return Stream.of(
                Arguments.of(StandardCharsets.US_ASCII, "encoding-us-ascii-with-header.bib"),
                Arguments.of(StandardCharsets.UTF_8, "encoding-utf-8-with-header.bib"),
                Arguments.of(Charset.forName("Windows-1252"), "encoding-windows-1252-with-header.bib")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testParsingOfEncodedFileWithHeader(Charset charset, String fileName) throws Exception {
        ParserResult parserResult = importer.importDatabase(
                Path.of(BibtexImporterTest.class.getResource(fileName).toURI()));
        assertEquals(Optional.of(charset), parserResult.getMetaData().getEncoding());
    }

    @ParameterizedTest
    @CsvSource({"encoding-windows-1252-with-header.bib", "encoding-windows-1252-without-header.bib"})
    public void testParsingOfWindows1252EncodedFileReadsDegreeCharacterCorrectly(String filename) throws Exception {
        ParserResult parserResult = importer.importDatabase(
                Path.of(BibtexImporterTest.class.getResource(filename).toURI()));
        assertEquals(
                List.of(new BibEntry(StandardEntryType.Article).withField(StandardField.ABSTRACT, "25° C")),
                parserResult.getDatabase().getEntries());
    }

    @ParameterizedTest
    @CsvSource({"encoding-utf-8-with-header.bib", "encoding-utf-8-without-header.bib"})
    public void testParsingOfUtf8EncodedFileReadsUmlautCharacterCorrectly(String filename) throws Exception {
        ParserResult parserResult = importer.importDatabase(
                Path.of(BibtexImporterTest.class.getResource(filename).toURI()));
        assertEquals(
                List.of(new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "Ü ist ein Umlaut")),
                parserResult.getDatabase().getEntries());
    }


    @ParameterizedTest
    @CsvSource({"encoding-utf-16BE-with-header.bib", "encoding-utf-16BE-without-header.bib"})
    public void testParsingOfUtf16EncodedFileReadsUmlautCharacterCorrectly(String filename) throws Exception {
        ParserResult parserResult = importer.importDatabase(
                Path.of(BibtexImporterTest.class.getResource(filename).toURI()));

        BibDatabaseContext parsedContext = new BibDatabaseContext(parserResult.getDatabase(), parserResult.getMetaData());

        MetaData metaData = new MetaData();
        metaData.setMode(BibDatabaseMode.BIBTEX);
        metaData.setEncoding(StandardCharsets.UTF_16BE);
        BibDatabase database = new BibDatabase(List.of(new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "Ü ist ein Umlaut")));;
        database.setNewLineSeparator("\n");
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(database, metaData);

        assertEquals(
                     bibDatabaseContext.getDatabase().getEntries(),
                     parsedContext.getDatabase().getEntries());
    }
	
    @Test
    public void encodingSupplied() throws Exception {
        ParserResult parserResult = importer.importDatabase(
                Path.of(BibtexImporterTest.class.getResource("encoding-utf-8-with-header.bib").toURI()));
        assertTrue(parserResult.getMetaData().getEncodingExplicitlySupplied());
    }

    @Test
    public void encodingNotSupplied() throws Exception {
        ParserResult parserResult = importer.importDatabase(
                Path.of(BibtexImporterTest.class.getResource("encoding-utf-8-without-header.bib").toURI()));
        assertFalse(parserResult.getMetaData().getEncodingExplicitlySupplied());
    }
}
