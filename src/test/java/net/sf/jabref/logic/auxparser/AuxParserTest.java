package net.sf.jabref.logic.auxparser;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibDatabase;

public class AuxParserTest {

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testNormal() throws URISyntaxException {
        InputStream originalStream = AuxParserTest.class.getResourceAsStream("origin.bib");
        File auxFile = Paths.get(AuxParserTest.class.getResource("paper.aux").toURI()).toFile();
        try (InputStreamReader originalReader = new InputStreamReader(originalStream)) {
            ParserResult result = BibtexParser.parse(originalReader);

            AuxParser auxParser = new AuxParser(auxFile.getAbsolutePath(), result.getDatabase());
            AuxParserResult auxResult = auxParser.parse();

            assertFalse(auxResult.getGeneratedBibDatabase().hasNoEntries());
            assertEquals(0, auxResult.getUnresolvedKeysCount());
            BibDatabase newDB = auxResult.getGeneratedBibDatabase();
            assertEquals(2, newDB.getEntries().size());
            assertEquals(2, auxResult.getResolvedKeysCount());
            assertEquals(2, auxResult.getFoundKeysInAux());
            assertEquals(auxResult.getFoundKeysInAux(),
                    auxResult.getResolvedKeysCount() + auxResult.getUnresolvedKeysCount());
            assertEquals(0, auxResult.getCrossRefEntriesCount());
        } catch (IOException ex) {
            fail("Cannot open file");
        }
    }

    @Test
    public void testNotAllFound() throws URISyntaxException {
        InputStream originalStream = AuxParserTest.class.getResourceAsStream("origin.bib");
        File auxFile = Paths.get(AuxParserTest.class.getResource("badpaper.aux").toURI()).toFile();

        try (InputStreamReader originalReader = new InputStreamReader(originalStream)) {
            ParserResult result = BibtexParser.parse(originalReader);

            AuxParser auxParser = new AuxParser(auxFile.getAbsolutePath(), result.getDatabase());
            AuxParserResult auxResult = auxParser.parse();

            assertFalse(auxResult.getGeneratedBibDatabase().hasNoEntries());
            assertEquals(1, auxResult.getUnresolvedKeysCount());
            BibDatabase newDB = auxResult.getGeneratedBibDatabase();
            assertEquals(2, newDB.getEntries().size());
            assertEquals(2, auxResult.getResolvedKeysCount());
            assertEquals(3, auxResult.getFoundKeysInAux());
            assertEquals(auxResult.getFoundKeysInAux(),
                    auxResult.getResolvedKeysCount() + auxResult.getUnresolvedKeysCount());
            assertEquals(0, auxResult.getCrossRefEntriesCount());
        } catch (IOException ex) {
            fail("Cannot open file");
        }
    }

    @Test
    public void duplicateBibDatabaseConfiguration() throws URISyntaxException {
        InputStream originalStream = AuxParserTest.class.getResourceAsStream("config.bib");
        File auxFile = Paths.get(AuxParserTest.class.getResource("paper.aux").toURI()).toFile();
        try (InputStreamReader originalReader = new InputStreamReader(originalStream)) {
            ParserResult result = BibtexParser.parse(originalReader);

            AuxParser auxParser = new AuxParser(auxFile.getAbsolutePath(), result.getDatabase());
            AuxParserResult auxResult = auxParser.parse();
            BibDatabase db = auxResult.getGeneratedBibDatabase();

            assertEquals("\"Maintained by \" # maintainer", db.getPreamble());
            assertEquals(1, db.getStringCount());
        } catch (IOException ex) {
            fail("Cannot open file");
        }
    }

    @Test
    public void testNestedAux() throws URISyntaxException {
        InputStream originalStream = AuxParserTest.class.getResourceAsStream("origin.bib");
        File auxFile = Paths.get(AuxParserTest.class.getResource("nested.aux").toURI()).toFile();
        try (InputStreamReader originalReader = new InputStreamReader(originalStream)) {
            ParserResult result = BibtexParser.parse(originalReader);

            AuxParser auxParser = new AuxParser(auxFile.getAbsolutePath(), result.getDatabase());
            AuxParserResult auxResult = auxParser.parse();

            assertFalse(auxResult.getGeneratedBibDatabase().hasNoEntries());
            assertEquals(0, auxResult.getUnresolvedKeysCount());
            BibDatabase newDB = auxResult.getGeneratedBibDatabase();
            assertEquals(2, newDB.getEntries().size());
            assertEquals(2, auxResult.getResolvedKeysCount());
            assertEquals(2, auxResult.getFoundKeysInAux());
            assertEquals(auxResult.getFoundKeysInAux(),
                    auxResult.getResolvedKeysCount() + auxResult.getUnresolvedKeysCount());
            assertEquals(0, auxResult.getCrossRefEntriesCount());
        } catch (IOException ex) {
            fail("Cannot open file");
        }
    }
}
