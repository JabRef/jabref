package net.sf.jabref.importer.fileformat;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class BibtexImporterTest {

    private BibtexImporter importer;

    @Before
    public void setUp() throws Exception {
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
    public void testImportEntries() throws Exception {
        try (InputStream stream = BibtexImporterTest.class.getResourceAsStream("BibtexImporter.examples.bib")) {
            List<BibEntry> bibEntries = importer.importEntries(stream, new OutputPrinterToNull());

            assertEquals(4, bibEntries.size());

            BibEntry be0 = bibEntries.get(0);
            assertEquals("Aks{\\i}n, {\\\"O}zge and T{\\\"u}rkmen, Hayati and Artok, Levent and {\\c{C}}etinkaya, " +
                    "Bekir and Ni, Chaoying and B{\\\"u}y{\\\"u}kg{\\\"u}ng{\\\"o}r, Orhan and {\\\"O}zkal, Erhan",
                     be0.getField("author"));
            assertEquals("aksin", be0.getField("bibtexkey"));
            assertEquals("2006", be0.getField("date"));
            assertEquals("Effect of immobilization on catalytic characteristics", be0.getField("indextitle"));
            assertEquals("#jomch#", be0.getField("journaltitle"));
            assertEquals("13", be0.getField("number"));
            assertEquals("3027-3036", be0.getField("pages"));
            assertEquals("Effect of immobilization on catalytic characteristics of saturated {Pd-N}-heterocyclic " +
                    "carbenes in {Mizoroki-Heck} reactions", be0.getField("title"));
            assertEquals("691", be0.getField("volume"));

            BibEntry be1 = bibEntries.get(1);
            assertEquals("A \\texttt{set} with three members discussing the standard model of particle physics. " +
                    "The \\texttt{crossref} field in the \\texttt{@set} entry and the \\texttt{entryset} field in " +
                    "each set member entry is needed only when using BibTeX as the backend",
                    be1.getField("annotation"));
            assertEquals("stdmodel", be1.getField("bibtexkey"));
            assertEquals("glashow,weinberg,salam", be1.getField("entryset"));

            BibEntry be2 = bibEntries.get(2);
            assertEquals("A \\texttt{set} with three members. The \\texttt{crossref} field in the \\texttt{@set} " +
                    "entry and the \\texttt{entryset} field in each set member entry is needed only when using " +
                    "BibTeX as the backend", be2.getField("annotation"));
            assertEquals("set", be2.getField("bibtexkey"));
            assertEquals("herrmann,aksin,yoon", be2.getField("entryset"));

            BibEntry be3 = bibEntries.get(3);
            assertEquals("Heidelberg", be3.getField("address"));
            assertEquals("Preißel, René", be3.getField("author"));
            assertEquals("Preissel2016", be3.getField("bibtexkey"));
            assertEquals("3., aktualisierte und erweiterte Auflage", be3.getField("edition"));
            assertEquals("978-3-86490-311-3", be3.getField("isbn"));
            assertEquals("Versionsverwaltung", be3.getField("keywords"));
            assertEquals("XX, 327 Seiten", be3.getField("pages"));
            assertEquals("dpunkt.verlag", be3.getField("publisher"));
            assertEquals("Git: dezentrale Versionsverwaltung im Team : Grundlagen und Workflows",
                    be3.getField("title"));
            assertEquals("http://d-nb.info/107601965X", be3.getField("url"));
            assertEquals("2016", be3.getField("year"));
        }
    }

    @Test
    public void testGetFormatName() throws Exception {
        assertEquals("BibTeX", importer.getFormatName());
    }

    @Test
    public void testGetExtensions() throws Exception {
        assertEquals("bib", importer.getExtensions());
    }
}
