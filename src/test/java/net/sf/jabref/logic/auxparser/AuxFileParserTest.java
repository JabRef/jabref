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

public class AuxFileParserTest {

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testNormal() {
        InputStream originalStream = AuxFileParserTest.class.getResourceAsStream("origin.bib");
        File auxFile = new File(AuxFileParserTest.class.getResource("paper.aux").getFile());
        try (InputStreamReader originalReader = new InputStreamReader(originalStream)) {
            ParserResult result = BibtexParser.parse(originalReader);

            AuxFileParser auxFileParser = new AuxFileParser(auxFile.getAbsolutePath(), result.getDatabase());
            assertFalse(auxFileParser.getGeneratedBibDatabase().isEmpty());
            assertEquals(0, auxFileParser.getUnresolvedKeysCount());
            BibDatabase newDB = auxFileParser.getGeneratedBibDatabase();
            assertEquals(2, newDB.getEntries().size());
            assertEquals(2, auxFileParser.getResolvedKeysCount());
            assertEquals(2, auxFileParser.getFoundKeysInAux());
            assertEquals(auxFileParser.getFoundKeysInAux(),
                    auxFileParser.getResolvedKeysCount() + auxFileParser.getUnresolvedKeysCount());
            assertEquals(0, auxFileParser.getCrossRefEntriesCount());
        } catch (IOException ex) {
            fail("Cannot open file");
        }
    }

    @Test
    public void testNotAllFound() {
        InputStream originalStream = AuxFileParserTest.class.getResourceAsStream("origin.bib");
        File auxFile = new File(AuxFileParserTest.class.getResource("badpaper.aux").getFile());
        try (InputStreamReader originalReader = new InputStreamReader(originalStream)) {
            ParserResult result = BibtexParser.parse(originalReader);

            AuxFileParser auxFileParser = new AuxFileParser(auxFile.getAbsolutePath(), result.getDatabase());
            assertFalse(auxFileParser.getGeneratedBibDatabase().isEmpty());
            assertEquals(1, auxFileParser.getUnresolvedKeysCount());
            BibDatabase newDB = auxFileParser.getGeneratedBibDatabase();
            assertEquals(2, newDB.getEntries().size());
            assertEquals(2, auxFileParser.getResolvedKeysCount());
            assertEquals(3, auxFileParser.getFoundKeysInAux());
            assertEquals(auxFileParser.getFoundKeysInAux(),
                    auxFileParser.getResolvedKeysCount() + auxFileParser.getUnresolvedKeysCount());
            assertEquals(0, auxFileParser.getCrossRefEntriesCount());
        } catch (IOException ex) {
            fail("Cannot open file");
        }
    }

    @Test
    public void duplicateBibDatabaseConfiguration() {
        InputStream originalStream = AuxFileParserTest.class.getResourceAsStream("config.bib");
        File auxFile = new File(AuxFileParserTest.class.getResource("paper.aux").getFile());
        try (InputStreamReader originalReader = new InputStreamReader(originalStream)) {
            ParserResult result = BibtexParser.parse(originalReader);

            AuxFileParser auxFileParser = new AuxFileParser(auxFile.getAbsolutePath(), result.getDatabase());
            BibDatabase db = auxFileParser.getGeneratedBibDatabase();

            assertEquals("\"Maintained by \" # maintainer", db.getPreamble());
            assertEquals(1, db.getStringCount());
        } catch (IOException ex) {
            fail("Cannot open file");
        }
    }

    @Test
    public void testNestedAux() {
        InputStream originalStream = AuxFileParserTest.class.getResourceAsStream("origin.bib");
        File auxFile = new File(AuxFileParserTest.class.getResource("nested.aux").getFile());
        try (InputStreamReader originalReader = new InputStreamReader(originalStream)) {
            ParserResult result = BibtexParser.parse(originalReader);

            AuxFileParser auxFileParser = new AuxFileParser(auxFile.getAbsolutePath(), result.getDatabase());
            assertFalse(auxFileParser.getGeneratedBibDatabase().isEmpty());
            assertEquals(0, auxFileParser.getUnresolvedKeysCount());
            BibDatabase newDB = auxFileParser.getGeneratedBibDatabase();
            assertEquals(2, newDB.getEntries().size());
            assertEquals(2, auxFileParser.getResolvedKeysCount());
            assertEquals(2, auxFileParser.getFoundKeysInAux());
            assertEquals(auxFileParser.getFoundKeysInAux(),
                    auxFileParser.getResolvedKeysCount() + auxFileParser.getUnresolvedKeysCount());
            assertEquals(0, auxFileParser.getCrossRefEntriesCount());
        } catch (IOException ex) {
            fail("Cannot open file");
        }
    }
}
