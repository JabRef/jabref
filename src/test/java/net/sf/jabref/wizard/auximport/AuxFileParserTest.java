package net.sf.jabref.wizard.auximport;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

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

            AuxFileParser auxFileParser = new AuxFileParser();
            List<String> unresolved = auxFileParser.generateBibDatabase(auxFile.getAbsolutePath(), result.getDatabase());
            assertFalse(auxFileParser.getGeneratedBibDatabase().isEmpty());
            assertEquals(0, unresolved.size());
            assertEquals(0, auxFileParser.getNotResolvedKeysCount());
            BibDatabase newDB = auxFileParser.getGeneratedBibDatabase();
            assertEquals(2, newDB.getEntries().size());
            assertEquals(2, auxFileParser.getResolvedKeysCount());
            assertEquals(2, auxFileParser.getFoundKeysInAux());
            assertEquals(auxFileParser.getFoundKeysInAux(),
                    auxFileParser.getResolvedKeysCount() + auxFileParser.getNotResolvedKeysCount());
            assertEquals(0, auxFileParser.getCrossreferencedEntriesCount());

            // Clear

            auxFileParser.clear();
            assertEquals(0, unresolved.size());
            assertEquals(0, auxFileParser.getNotResolvedKeysCount());
            newDB = auxFileParser.getGeneratedBibDatabase();
            assertEquals(0, newDB.getEntries().size());
            assertEquals(0, auxFileParser.getResolvedKeysCount());
            assertEquals(0, auxFileParser.getFoundKeysInAux());
            assertEquals(auxFileParser.getFoundKeysInAux(),
                    auxFileParser.getResolvedKeysCount() + auxFileParser.getNotResolvedKeysCount());
            assertEquals(0, auxFileParser.getCrossreferencedEntriesCount());

        } catch (IOException ex) {
            fail("Cannot open file");
        }
    }

    @Test
    public void testNoInput() {
        AuxFileParser auxFileParser = new AuxFileParser();
        assertTrue(auxFileParser.getGeneratedBibDatabase().isEmpty());
        assertEquals(0, auxFileParser.getNotResolvedKeysCount());
        BibDatabase newDB = auxFileParser.getGeneratedBibDatabase();
        assertEquals(0, newDB.getEntries().size());
        assertEquals(0, auxFileParser.getResolvedKeysCount());
        assertEquals(0, auxFileParser.getFoundKeysInAux());
        assertEquals(auxFileParser.getFoundKeysInAux(),
                auxFileParser.getResolvedKeysCount() + auxFileParser.getNotResolvedKeysCount());
    }

    @Test
    public void testNotAllFound() {
        InputStream originalStream = AuxFileParserTest.class.getResourceAsStream("origin.bib");
        File auxFile = new File(AuxFileParserTest.class.getResource("badpaper.aux").getFile());
        try (InputStreamReader originalReader = new InputStreamReader(originalStream)) {
            ParserResult result = BibtexParser.parse(originalReader);

            AuxFileParser auxFileParser = new AuxFileParser();
            List<String> unresolved = auxFileParser.generateBibDatabase(auxFile.getAbsolutePath(), result.getDatabase());
            assertFalse(auxFileParser.getGeneratedBibDatabase().isEmpty());
            assertEquals(1, unresolved.size());
            assertEquals(1, auxFileParser.getNotResolvedKeysCount());
            BibDatabase newDB = auxFileParser.getGeneratedBibDatabase();
            assertEquals(2, newDB.getEntries().size());
            assertEquals(2, auxFileParser.getResolvedKeysCount());
            assertEquals(3, auxFileParser.getFoundKeysInAux());
            assertEquals(auxFileParser.getFoundKeysInAux(),
                    auxFileParser.getResolvedKeysCount() + auxFileParser.getNotResolvedKeysCount());
            assertEquals(0, auxFileParser.getCrossreferencedEntriesCount());
        } catch (IOException ex) {
            fail("Cannot open file");
        }
    }

    // TODO strings and preamble test
    // TODO return type of generate during error should be false

}
