package net.sf.jabref.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibDatabase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AuxCommandLineTest {

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void test() throws URISyntaxException, IOException {
        InputStream originalStream = AuxCommandLineTest.class.getResourceAsStream("origin.bib");

        File auxFile = Paths.get(AuxCommandLineTest.class.getResource("paper.aux").toURI()).toFile();
        try (InputStreamReader originalReader = new InputStreamReader(originalStream)) {
            ParserResult result = BibtexParser.parse(originalReader);

            AuxCommandLine auxCommandLine = new AuxCommandLine(auxFile.getAbsolutePath(), result.getDatabase());
            BibDatabase newDB = auxCommandLine.perform();
            Assert.assertNotNull(newDB);
            Assert.assertEquals(2, newDB.getEntries().size());
        }
    }

}
