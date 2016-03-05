package net.sf.jabref.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibDatabase;

public class AuxCommandLineTest {

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void test() {
        InputStream originalStream = AuxCommandLineTest.class.getResourceAsStream("origin.bib");
        File auxFile = new File(AuxCommandLineTest.class.getResource("paper.aux").getFile());
        try (InputStreamReader originalReader = new InputStreamReader(originalStream)) {
            ParserResult result = BibtexParser.parse(originalReader);

            AuxCommandLine auxCommandLine = new AuxCommandLine(auxFile.getAbsolutePath(), result.getDatabase());
            BibDatabase newDB = auxCommandLine.perform();
            Assert.assertNotNull(newDB);
            Assert.assertEquals(2, newDB.getEntries().size());
        } catch (IOException ex) {
            Assert.fail("Cannot open file");
        }
    }

}
