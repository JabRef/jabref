package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;

public class SimpleCSVImporterTest {

    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void test() throws IOException {
        try (InputStream is = SimpleCSVImporterTest.class.getResourceAsStream("SimpleCSVImporterExample.scsv")) {
            List<BibEntry> entries = new SimpleCSVImporter().importEntries(is, new OutputPrinterToNull());
            Assert.assertEquals(3, entries.size());
            Assert.assertEquals("John Maynard Keynes", entries.get(0).getField("author"));
            Assert.assertEquals("2003", entries.get(1).getField("year"));
            Assert.assertEquals("The Software Patent Experiment", entries.get(2).getField("title"));
            Assert.assertEquals(BibtexEntryTypes.TECHREPORT.getName().toLowerCase(), entries.get(0).getType());
        }
    }

}
