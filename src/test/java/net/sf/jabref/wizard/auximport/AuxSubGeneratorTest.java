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

public class AuxSubGeneratorTest {

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testNormal() {
        InputStream originalStream = AuxSubGeneratorTest.class.getResourceAsStream("origin.bib");
        File auxFile = new File(AuxSubGeneratorTest.class.getResource("paper.aux").getFile());
        try (InputStreamReader originalReader = new InputStreamReader(originalStream)) {
            ParserResult result = BibtexParser.parse(originalReader);

            AuxSubGenerator auxSubGenerator = new AuxSubGenerator();
            List<String> unresolved = auxSubGenerator.generate(auxFile.getAbsolutePath(), result.getDatabase());
            assertFalse(auxSubGenerator.emptyGeneratedDatabase());
            assertEquals(0, unresolved.size());
            assertEquals(0, auxSubGenerator.getNotResolvedKeysCount());
            BibDatabase newDB = auxSubGenerator.getGeneratedDatabase();
            assertEquals(2, newDB.getEntries().size());
            assertEquals(2, auxSubGenerator.getResolvedKeysCount());
            assertEquals(2, auxSubGenerator.getFoundKeysInAux());
            assertEquals(auxSubGenerator.getFoundKeysInAux(),
                    auxSubGenerator.getResolvedKeysCount() + auxSubGenerator.getNotResolvedKeysCount());
            assertEquals(0, auxSubGenerator.getNestedAuxCounter());
            assertEquals(0, auxSubGenerator.getCrossreferencedEntriesCount());

            // Clear

            auxSubGenerator.clear();
            assertEquals(0, unresolved.size());
            assertEquals(0, auxSubGenerator.getNotResolvedKeysCount());
            newDB = auxSubGenerator.getGeneratedDatabase();
            assertEquals(0, newDB.getEntries().size());
            assertEquals(0, auxSubGenerator.getResolvedKeysCount());
            assertEquals(0, auxSubGenerator.getFoundKeysInAux());
            assertEquals(auxSubGenerator.getFoundKeysInAux(),
                    auxSubGenerator.getResolvedKeysCount() + auxSubGenerator.getNotResolvedKeysCount());
            assertEquals(0, auxSubGenerator.getNestedAuxCounter());
            assertEquals(0, auxSubGenerator.getCrossreferencedEntriesCount());

        } catch (IOException ex) {
            fail("Cannot open file");
        }
    }

    @Test
    public void testNoInput() {
        AuxSubGenerator auxSubGenerator = new AuxSubGenerator();
        assertTrue(auxSubGenerator.emptyGeneratedDatabase());
        assertEquals(0, auxSubGenerator.getNotResolvedKeysCount());
        BibDatabase newDB = auxSubGenerator.getGeneratedDatabase();
        assertEquals(0, newDB.getEntries().size());
        assertEquals(0, auxSubGenerator.getResolvedKeysCount());
        assertEquals(0, auxSubGenerator.getFoundKeysInAux());
        assertEquals(auxSubGenerator.getFoundKeysInAux(),
                auxSubGenerator.getResolvedKeysCount() + auxSubGenerator.getNotResolvedKeysCount());
    }

    @Test
    public void testNotAllFound() {
        InputStream originalStream = AuxSubGeneratorTest.class.getResourceAsStream("origin.bib");
        File auxFile = new File(AuxSubGeneratorTest.class.getResource("badpaper.aux").getFile());
        try (InputStreamReader originalReader = new InputStreamReader(originalStream)) {
            ParserResult result = BibtexParser.parse(originalReader);

            AuxSubGenerator auxSubGenerator = new AuxSubGenerator();
            List<String> unresolved = auxSubGenerator.generate(auxFile.getAbsolutePath(), result.getDatabase());
            assertFalse(auxSubGenerator.emptyGeneratedDatabase());
            assertEquals(1, unresolved.size());
            assertEquals(1, auxSubGenerator.getNotResolvedKeysCount());
            BibDatabase newDB = auxSubGenerator.getGeneratedDatabase();
            assertEquals(2, newDB.getEntries().size());
            assertEquals(2, auxSubGenerator.getResolvedKeysCount());
            assertEquals(3, auxSubGenerator.getFoundKeysInAux());
            assertEquals(auxSubGenerator.getFoundKeysInAux(),
                    auxSubGenerator.getResolvedKeysCount() + auxSubGenerator.getNotResolvedKeysCount());
            assertEquals(0, auxSubGenerator.getNestedAuxCounter());
            assertEquals(0, auxSubGenerator.getCrossreferencedEntriesCount());
        } catch (IOException ex) {
            fail("Cannot open file");
        }
    }

}
