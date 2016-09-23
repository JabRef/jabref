package net.sf.jabref.logic.auxparser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Optional;

import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuxParserTest {
    private ImportFormatPreferences importFormatPreferences;

    @Before
    public void setUp() {
        importFormatPreferences = JabRefPreferences.getInstance().getImportFormatPreferences();
    }

    @After
    public void tearDown() {
        importFormatPreferences = null;
    }

    @Test
    public void testNormal() throws URISyntaxException, IOException {
        InputStream originalStream = AuxParserTest.class.getResourceAsStream("origin.bib");
        File auxFile = Paths.get(AuxParserTest.class.getResource("paper.aux").toURI()).toFile();
        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = BibtexParser.parse(originalReader, importFormatPreferences);

            AuxParser auxParser = new AuxParser(auxFile.getAbsolutePath(), result.getDatabase());
            AuxParserResult auxResult = auxParser.parse();

            assertTrue(auxResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(0, auxResult.getUnresolvedKeysCount());
            BibDatabase newDB = auxResult.getGeneratedBibDatabase();
            assertEquals(2, newDB.getEntries().size());
            assertEquals(2, auxResult.getResolvedKeysCount());
            assertEquals(2, auxResult.getFoundKeysInAux());
            assertEquals(auxResult.getFoundKeysInAux() + auxResult.getCrossRefEntriesCount(),
                    auxResult.getResolvedKeysCount() + auxResult.getUnresolvedKeysCount());
            assertEquals(0, auxResult.getCrossRefEntriesCount());
        }
    }

    @Test
    public void testNotAllFound() throws URISyntaxException, IOException {
        InputStream originalStream = AuxParserTest.class.getResourceAsStream("origin.bib");
        File auxFile = Paths.get(AuxParserTest.class.getResource("badpaper.aux").toURI()).toFile();

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = BibtexParser.parse(originalReader, importFormatPreferences);

            AuxParser auxParser = new AuxParser(auxFile.getAbsolutePath(), result.getDatabase());
            AuxParserResult auxResult = auxParser.parse();

            assertTrue(auxResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(1, auxResult.getUnresolvedKeysCount());
            BibDatabase newDB = auxResult.getGeneratedBibDatabase();
            assertEquals(2, newDB.getEntries().size());
            assertEquals(2, auxResult.getResolvedKeysCount());
            assertEquals(3, auxResult.getFoundKeysInAux());
            assertEquals(auxResult.getFoundKeysInAux() + auxResult.getCrossRefEntriesCount(),
                    auxResult.getResolvedKeysCount() + auxResult.getUnresolvedKeysCount());
            assertEquals(0, auxResult.getCrossRefEntriesCount());
        }
    }

    @Test
    public void duplicateBibDatabaseConfiguration() throws URISyntaxException, IOException {
        InputStream originalStream = AuxParserTest.class.getResourceAsStream("config.bib");
        File auxFile = Paths.get(AuxParserTest.class.getResource("paper.aux").toURI()).toFile();
        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = BibtexParser.parse(originalReader, importFormatPreferences);

            AuxParser auxParser = new AuxParser(auxFile.getAbsolutePath(), result.getDatabase());
            AuxParserResult auxResult = auxParser.parse();
            BibDatabase db = auxResult.getGeneratedBibDatabase();

            assertEquals(Optional.of("\"Maintained by \" # maintainer"), db.getPreamble());
            assertEquals(1, db.getStringCount());
        }
    }

    @Test
    public void testNestedAux() throws URISyntaxException, IOException {
        InputStream originalStream = AuxParserTest.class.getResourceAsStream("origin.bib");
        File auxFile = Paths.get(AuxParserTest.class.getResource("nested.aux").toURI()).toFile();
        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = BibtexParser.parse(originalReader, importFormatPreferences);

            AuxParser auxParser = new AuxParser(auxFile.getAbsolutePath(), result.getDatabase());
            AuxParserResult auxResult = auxParser.parse();

            assertTrue(auxResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(0, auxResult.getUnresolvedKeysCount());
            BibDatabase newDB = auxResult.getGeneratedBibDatabase();
            assertEquals(2, newDB.getEntries().size());
            assertEquals(2, auxResult.getResolvedKeysCount());
            assertEquals(2, auxResult.getFoundKeysInAux());
            assertEquals(auxResult.getFoundKeysInAux() + auxResult.getCrossRefEntriesCount(),
                    auxResult.getResolvedKeysCount() + auxResult.getUnresolvedKeysCount());
            assertEquals(0, auxResult.getCrossRefEntriesCount());
        }
    }

    @Test
    public void testCrossRef() throws URISyntaxException, IOException {
        InputStream originalStream = AuxParserTest.class.getResourceAsStream("origin.bib");
        File auxFile = Paths.get(AuxParserTest.class.getResource("crossref.aux").toURI()).toFile();
        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = BibtexParser.parse(originalReader, importFormatPreferences);

            AuxParser auxParser = new AuxParser(auxFile.getAbsolutePath(), result.getDatabase());
            AuxParserResult auxResult = auxParser.parse();

            assertTrue(auxResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(2, auxResult.getUnresolvedKeysCount());
            BibDatabase newDB = auxResult.getGeneratedBibDatabase();
            assertEquals(4, newDB.getEntries().size());
            assertEquals(3, auxResult.getResolvedKeysCount());
            assertEquals(4, auxResult.getFoundKeysInAux());
            assertEquals(auxResult.getFoundKeysInAux() + auxResult.getCrossRefEntriesCount(),
                    auxResult.getResolvedKeysCount() + auxResult.getUnresolvedKeysCount());
            assertEquals(1, auxResult.getCrossRefEntriesCount());
        }
    }

    @Test
    public void testFileNotFound() {
        AuxParser auxParser = new AuxParser("unknownfile.bib", new BibDatabase());
        AuxParserResult auxResult = auxParser.parse();

        assertFalse(auxResult.getGeneratedBibDatabase().hasEntries());
        assertEquals(0, auxResult.getUnresolvedKeysCount());
        BibDatabase newDB = auxResult.getGeneratedBibDatabase();
        assertEquals(0, newDB.getEntries().size());
        assertEquals(0, auxResult.getResolvedKeysCount());
        assertEquals(0, auxResult.getFoundKeysInAux());
        assertEquals(auxResult.getFoundKeysInAux() + auxResult.getCrossRefEntriesCount(),
                auxResult.getResolvedKeysCount() + auxResult.getUnresolvedKeysCount());
        assertEquals(0, auxResult.getCrossRefEntriesCount());
    }
}
