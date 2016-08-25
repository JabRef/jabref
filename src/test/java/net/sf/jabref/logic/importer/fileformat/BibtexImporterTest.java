package net.sf.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Before
    public void setUp() {
        importer = new BibtexImporter(ImportFormatPreferences.fromPreferences(JabRefPreferences.getInstance()));
    }

    @Test
    public void testIsRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Paths.get(BibtexImporterTest.class.getResource("BibtexImporter.examples.bib").toURI());
        assertTrue(importer.isRecognizedFormat(file, Charset.defaultCharset()));
    }

    @Test
    public void testImportEntries() throws IOException, URISyntaxException {
        Path file = Paths.get(BibtexImporterTest.class.getResource("BibtexImporter.examples.bib").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

        assertEquals(4, bibEntries.size());

        for (BibEntry entry : bibEntries) {

            if (entry.getCiteKeyOptional().get().equals("aksin")) {
                assertEquals(
                        Optional.of(
                                "Aks{\\i}n, {\\\"O}zge and T{\\\"u}rkmen, Hayati and Artok, Levent and {\\c{C}}etinkaya, "
                                        + "Bekir and Ni, Chaoying and B{\\\"u}y{\\\"u}kg{\\\"u}ng{\\\"o}r, Orhan and {\\\"O}zkal, Erhan"),
                        entry.getFieldOptional("author"));
                assertEquals(Optional.of("aksin"), entry.getFieldOptional("bibtexkey"));
                assertEquals(Optional.of("2006"), entry.getFieldOptional("date"));
                assertEquals(Optional.of("Effect of immobilization on catalytic characteristics"), entry.getFieldOptional("indextitle"));
                assertEquals(Optional.of("#jomch#"), entry.getFieldOptional("journaltitle"));
                assertEquals(Optional.of("13"), entry.getFieldOptional("number"));
                assertEquals(Optional.of("3027-3036"), entry.getFieldOptional("pages"));
                assertEquals(Optional
                        .of("Effect of immobilization on catalytic characteristics of saturated {Pd-N}-heterocyclic "
                                + "carbenes in {Mizoroki-Heck} reactions"),
                        entry.getFieldOptional("title"));
                assertEquals(Optional.of("691"), entry.getFieldOptional("volume"));
            } else if (entry.getCiteKeyOptional().get().equals("stdmodel")) {
                assertEquals(Optional
                        .of("A \\texttt{set} with three members discussing the standard model of particle physics. " +
                                "The \\texttt{crossref} field in the \\texttt{@set} entry and the \\texttt{entryset} field in " +
                                "each set member entry is needed only when using BibTeX as the backend"),
                        entry.getFieldOptional("annotation"));
                assertEquals(Optional.of("stdmodel"), entry.getFieldOptional("bibtexkey"));
                assertEquals(Optional.of("glashow,weinberg,salam"), entry.getFieldOptional("entryset"));
            } else if (entry.getCiteKeyOptional().get().equals("set")) {
                assertEquals(Optional
                        .of("A \\texttt{set} with three members. The \\texttt{crossref} field in the \\texttt{@set} " +
                        "entry and the \\texttt{entryset} field in each set member entry is needed only when using " +
                                "BibTeX as the backend"),
                        entry.getFieldOptional("annotation"));
                assertEquals(Optional.of("set"), entry.getFieldOptional("bibtexkey"));
                assertEquals(Optional.of("herrmann,aksin,yoon"), entry.getFieldOptional("entryset"));
            } else if (entry.getCiteKeyOptional().get().equals("Preissel2016")) {
                assertEquals(Optional.of("Heidelberg"), entry.getFieldOptional("address"));
                assertEquals(Optional.of("Preißel, René"), entry.getFieldOptional("author"));
                assertEquals(Optional.of("Preissel2016"), entry.getFieldOptional("bibtexkey"));
                assertEquals(Optional.of("3., aktualisierte und erweiterte Auflage"), entry.getFieldOptional("edition"));
                assertEquals(Optional.of("978-3-86490-311-3"), entry.getFieldOptional("isbn"));
                assertEquals(Optional.of("Versionsverwaltung"), entry.getFieldOptional("keywords"));
                assertEquals(Optional.of("XX, 327 Seiten"), entry.getFieldOptional("pages"));
                assertEquals(Optional.of("dpunkt.verlag"), entry.getFieldOptional("publisher"));
                assertEquals(Optional.of("Git: dezentrale Versionsverwaltung im Team : Grundlagen und Workflows"),
                        entry.getFieldOptional("title"));
                assertEquals(Optional.of("http://d-nb.info/107601965X"), entry.getFieldOptional("url"));
                assertEquals(Optional.of("2016"), entry.getFieldOptional("year"));
            }
        }
    }

    @Test
    public void testGetFormatName() {
        assertEquals("BibTeX", importer.getFormatName());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileExtensions.BIBTEX_DB, importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        assertEquals("This importer exists only to enable `--importToOpen someEntry.bib`\n" +
                            "It is NOT intended to import a BIB file. This is done via the option action, which treats the metadata fields.\n" +
                            "The metadata is not required to be read here, as this class is NOT called at --import.", importer.getDescription());
    }
}
