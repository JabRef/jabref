package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;

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
        Globals.prefs = JabRefPreferences.getInstance();
        importer = new BibtexImporter();
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        try (InputStream stream = BibtexImporterTest.class.getResourceAsStream("BibtexImporter.examples.bib")) {
            assertTrue(importer.isRecognizedFormat(stream));
        }
    }

    @Test
    public void testImportEntries() throws IOException {
        try (InputStream stream = BibtexImporterTest.class.getResourceAsStream("BibtexImporter.examples.bib")) {
            List<BibEntry> bibEntries = importer.importEntries(stream, new OutputPrinterToNull());

            assertEquals(4, bibEntries.size());

            for (BibEntry entry : bibEntries) {

                if (entry.getCiteKey().equals("aksin")) {
                    assertEquals("Aks{\\i}n, {\\\"O}zge and T{\\\"u}rkmen, Hayati and Artok, Levent and {\\c{C}}etinkaya, " +
                                    "Bekir and Ni, Chaoying and B{\\\"u}y{\\\"u}kg{\\\"u}ng{\\\"o}r, Orhan and {\\\"O}zkal, Erhan",
                            entry.getField("author"));
                    assertEquals("aksin", entry.getField("bibtexkey"));
                    assertEquals("2006", entry.getField("date"));
                    assertEquals("Effect of immobilization on catalytic characteristics", entry.getField("indextitle"));
                    assertEquals("#jomch#", entry.getField("journaltitle"));
                    assertEquals("13", entry.getField("number"));
                    assertEquals("3027-3036", entry.getField("pages"));
                    assertEquals("Effect of immobilization on catalytic characteristics of saturated {Pd-N}-heterocyclic " +
                            "carbenes in {Mizoroki-Heck} reactions", entry.getField("title"));
                    assertEquals("691", entry.getField("volume"));
                } else if (entry.getCiteKey().equals("stdmodel")) {
                    assertEquals("A \\texttt{set} with three members discussing the standard model of particle physics. " +
                                    "The \\texttt{crossref} field in the \\texttt{@set} entry and the \\texttt{entryset} field in " +
                                    "each set member entry is needed only when using BibTeX as the backend",
                            entry.getField("annotation"));
                    assertEquals("stdmodel", entry.getField("bibtexkey"));
                    assertEquals("glashow,weinberg,salam", entry.getField("entryset"));
                } else if (entry.getCiteKey().equals("set")) {
                    assertEquals("A \\texttt{set} with three members. The \\texttt{crossref} field in the \\texttt{@set} " +
                            "entry and the \\texttt{entryset} field in each set member entry is needed only when using " +
                            "BibTeX as the backend", entry.getField("annotation"));
                    assertEquals("set", entry.getField("bibtexkey"));
                    assertEquals("herrmann,aksin,yoon", entry.getField("entryset"));
                } else if (entry.getCiteKey().equals("Preissel2016")) {
                    assertEquals("Heidelberg", entry.getField("address"));
                    assertEquals("Preißel, René", entry.getField("author"));
                    assertEquals("Preissel2016", entry.getField("bibtexkey"));
                    assertEquals("3., aktualisierte und erweiterte Auflage", entry.getField("edition"));
                    assertEquals("978-3-86490-311-3", entry.getField("isbn"));
                    assertEquals("Versionsverwaltung", entry.getField("keywords"));
                    assertEquals("XX, 327 Seiten", entry.getField("pages"));
                    assertEquals("dpunkt.verlag", entry.getField("publisher"));
                    assertEquals("Git: dezentrale Versionsverwaltung im Team : Grundlagen und Workflows",
                            entry.getField("title"));
                    assertEquals("http://d-nb.info/107601965X", entry.getField("url"));
                    assertEquals("2016", entry.getField("year"));
                }
            }
        }
    }

    @Test
    public void testGetFormatName() {
        assertEquals("BibTeX", importer.getFormatName());
    }

    @Test
    public void testGetExtensions() {
        assertEquals("bib", importer.getExtensions());
    }
}
