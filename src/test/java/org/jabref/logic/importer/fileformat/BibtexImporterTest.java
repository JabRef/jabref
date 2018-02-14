package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.FileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * This class tests the BibtexImporter.
 * That importer is only used for --importToOpen, which is currently untested
 * <p>
 * TODO:
 * 1. Add test for --importToOpen
 * 2. Move these tests to the code opening a bibtex file
 */
public class BibtexImporterTest {

    private BibtexImporter importer;


    @BeforeEach
    public void setUp() {
        importer = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
    }

    @Test
    public void testIsRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Paths.get(BibtexImporterTest.class.getResource("BibtexImporter.examples.bib").toURI());
        assertTrue(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
    }

    @Test
    public void testImportEntries() throws IOException, URISyntaxException {
        Path file = Paths.get(BibtexImporterTest.class.getResource("BibtexImporter.examples.bib").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(4, bibEntries.size());

        for (BibEntry entry : bibEntries) {

            if (entry.getCiteKeyOptional().get().equals("aksin")) {
                assertEquals(
                        Optional.of(
                                "Aks{\\i}n, {\\\"O}zge and T{\\\"u}rkmen, Hayati and Artok, Levent and {\\c{C}}etinkaya, "
                                        + "Bekir and Ni, Chaoying and B{\\\"u}y{\\\"u}kg{\\\"u}ng{\\\"o}r, Orhan and {\\\"O}zkal, Erhan"),
                        entry.getField("author"));
                assertEquals(Optional.of("aksin"), entry.getField("bibtexkey"));
                assertEquals(Optional.of("2006"), entry.getField("date"));
                assertEquals(Optional.of("Effect of immobilization on catalytic characteristics"),
                        entry.getField("indextitle"));
                assertEquals(Optional.of("#jomch#"), entry.getField("journal"));
                assertEquals(Optional.of("13"), entry.getField("number"));
                assertEquals(Optional.of("3027-3036"), entry.getField("pages"));
                assertEquals(Optional
                        .of("Effect of immobilization on catalytic characteristics of saturated {Pd-N}-heterocyclic "
                                + "carbenes in {Mizoroki-Heck} reactions"),
                        entry.getField("title"));
                assertEquals(Optional.of("691"), entry.getField("volume"));
            } else if (entry.getCiteKeyOptional().get().equals("stdmodel")) {
                assertEquals(Optional
                        .of("A \\texttt{set} with three members discussing the standard model of particle physics. "
                                + "The \\texttt{crossref} field in the \\texttt{@set} entry and the \\texttt{entryset} field in "
                                + "each set member entry is needed only when using BibTeX as the backend"),
                        entry.getField("annotation"));
                assertEquals(Optional.of("stdmodel"), entry.getField("bibtexkey"));
                assertEquals(Optional.of("glashow,weinberg,salam"), entry.getField("entryset"));
            } else if (entry.getCiteKeyOptional().get().equals("set")) {
                assertEquals(Optional
                        .of("A \\texttt{set} with three members. The \\texttt{crossref} field in the \\texttt{@set} "
                                + "entry and the \\texttt{entryset} field in each set member entry is needed only when using "
                                + "BibTeX as the backend"),
                        entry.getField("annotation"));
                assertEquals(Optional.of("set"), entry.getField("bibtexkey"));
                assertEquals(Optional.of("herrmann,aksin,yoon"), entry.getField("entryset"));
            } else if (entry.getCiteKeyOptional().get().equals("Preissel2016")) {
                assertEquals(Optional.of("Heidelberg"), entry.getField("address"));
                assertEquals(Optional.of("Preißel, René"), entry.getField("author"));
                assertEquals(Optional.of("Preissel2016"), entry.getField("bibtexkey"));
                assertEquals(Optional.of("3., aktualisierte und erweiterte Auflage"),
                        entry.getField("edition"));
                assertEquals(Optional.of("978-3-86490-311-3"), entry.getField("isbn"));
                assertEquals(Optional.of("Versionsverwaltung"), entry.getField("keywords"));
                assertEquals(Optional.of("XX, 327 Seiten"), entry.getField("pages"));
                assertEquals(Optional.of("dpunkt.verlag"), entry.getField("publisher"));
                assertEquals(Optional.of("Git: dezentrale Versionsverwaltung im Team : Grundlagen und Workflows"),
                        entry.getField("title"));
                assertEquals(Optional.of("http://d-nb.info/107601965X"), entry.getField("url"));
                assertEquals(Optional.of("2016"), entry.getField("year"));
            }
        }
    }

    @Test
    public void testGetFormatName() {
        assertEquals("BibTeX", importer.getName());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileType.BIBTEX_DB, importer.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals(
                "This importer exists only to enable `--importToOpen someEntry.bib`\n"
                        + "It is NOT intended to import a BIB file. This is done via the option action, which treats the metadata fields.\n"
                        + "The metadata is not required to be read here, as this class is NOT called at --import.",
                importer.getDescription());
    }

    @Test
    public void testRecognizesDatabaseID() throws Exception {
        Path file = Paths.get(BibtexImporterTest.class.getResource("AutosavedSharedDatabase.bib").toURI());
        String sharedDatabaseID = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getSharedDatabaseID().get();
        assertEquals("13ceoc8dm42f5g1iitao3dj2ap", sharedDatabaseID);
    }
}
