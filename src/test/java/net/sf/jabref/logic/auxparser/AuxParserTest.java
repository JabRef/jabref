package net.sf.jabref.logic.auxparser;

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

public class AuxParserTest {

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testNormal() {
        InputStream originalStream = AuxParserTest.class.getResourceAsStream("origin.bib");
        File auxFile = new File(AuxParserTest.class.getResource("paper.aux").getFile());
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
    public void testNotAllFound() {
        InputStream originalStream = AuxParserTest.class.getResourceAsStream("origin.bib");
        File auxFile = new File(AuxParserTest.class.getResource("badpaper.aux").getFile());
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
    public void duplicateBibDatabaseConfiguration() {
        InputStream originalStream = AuxParserTest.class.getResourceAsStream("config.bib");
        File auxFile = new File(AuxParserTest.class.getResource("paper.aux").getFile());
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
    public void testNestedAux() {
        InputStream originalStream = AuxParserTest.class.getResourceAsStream("origin.bib");
        File auxFile = new File(AuxParserTest.class.getResource("nested.aux").getFile());
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
