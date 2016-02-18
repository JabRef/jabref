package net.sf.jabref.wizard.auximport;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
            assertEquals(2, newDB.getEntries().size());
        } catch (IOException ex) {
            fail("Cannot open file");
        }
    }

}
