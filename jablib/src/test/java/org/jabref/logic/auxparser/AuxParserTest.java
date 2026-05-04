package org.jabref.logic.auxparser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AuxParserTest {
    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    @AfterEach
    void tearDown() {
        importFormatPreferences = null;
    }

    @Test
    void normal() throws URISyntaxException, IOException, JabRefException {
        try (InputStream originalStream = AuxParserTest.class.getResourceAsStream("origin.bib")) {
            Path auxFile = Path.of(AuxParserTest.class.getResource("paper.aux").toURI());
            final BibDatabaseContext bibDatabaseContext = BibDatabaseContext.of(originalStream, importFormatPreferences);
            AuxParser auxParser = new DefaultAuxParser(bibDatabaseContext.getDatabase());
            AuxParserResult auxResult = auxParser.parse(auxFile);

            assertTrue(auxResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(0, auxResult.getUnresolvedKeysCount());
            BibDatabase newDB = auxResult.getGeneratedBibDatabase();
            List<BibEntry> newEntries = newDB.getEntries();
            assertEquals(2, newEntries.size());
            assertTrue(newEntries.getFirst().hasChanged());
            assertTrue(newEntries.get(1).hasChanged());
            assertEquals(2, auxResult.getResolvedKeysCount());
            assertEquals(2, auxResult.getFoundKeysInAux());
            assertEquals(auxResult.getFoundKeysInAux() + auxResult.getCrossRefEntriesCount(),
                    auxResult.getResolvedKeysCount() + auxResult.getUnresolvedKeysCount());
            assertEquals(0, auxResult.getCrossRefEntriesCount());
        }
    }

    @Test
    void twoArgMacro() throws URISyntaxException, IOException, JabRefException {
        // Result should be identical to that of testNormal
        try (InputStream originalStream = AuxParserTest.class.getResourceAsStream("origin.bib")) {
            Path auxFile = Path.of(AuxParserTest.class.getResource("papertwoargmacro.aux").toURI());
            final BibDatabaseContext bibDatabaseContext = BibDatabaseContext.of(originalStream, importFormatPreferences);
            AuxParser auxParser = new DefaultAuxParser(bibDatabaseContext.getDatabase());
            AuxParserResult auxResult = auxParser.parse(auxFile);

            assertTrue(auxResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(0, auxResult.getUnresolvedKeysCount());
            BibDatabase newDB = auxResult.getGeneratedBibDatabase();
            List<BibEntry> newEntries = newDB.getEntries();
            assertEquals(2, newEntries.size());
            assertTrue(newEntries.getFirst().hasChanged());
            assertTrue(newEntries.get(1).hasChanged());
            assertEquals(2, auxResult.getResolvedKeysCount());
            assertEquals(2, auxResult.getFoundKeysInAux());
            assertEquals(auxResult.getFoundKeysInAux() + auxResult.getCrossRefEntriesCount(),
                    auxResult.getResolvedKeysCount() + auxResult.getUnresolvedKeysCount());
            assertEquals(0, auxResult.getCrossRefEntriesCount());
        }
    }

    @Test
    void notAllFound() throws URISyntaxException, IOException, JabRefException {
        try (InputStream originalStream = AuxParserTest.class.getResourceAsStream("origin.bib")) {
            Path auxFile = Path.of(AuxParserTest.class.getResource("badpaper.aux").toURI());
            final BibDatabaseContext bibDatabaseContext = BibDatabaseContext.of(originalStream, importFormatPreferences);
            AuxParser auxParser = new DefaultAuxParser(bibDatabaseContext.getDatabase());
            AuxParserResult auxResult = auxParser.parse(auxFile);

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
    void duplicateBibDatabaseConfiguration() throws URISyntaxException, IOException, JabRefException {
        try (InputStream originalStream = AuxParserTest.class.getResourceAsStream("config.bib")) {
            Path auxFile = Path.of(AuxParserTest.class.getResource("paper.aux").toURI());
            final BibDatabaseContext bibDatabaseContext = BibDatabaseContext.of(originalStream, importFormatPreferences);
            AuxParser auxParser = new DefaultAuxParser(bibDatabaseContext.getDatabase());
            AuxParserResult auxResult = auxParser.parse(auxFile);
            BibDatabase db = auxResult.getGeneratedBibDatabase();

            assertEquals(Optional.of("\"Maintained by \" # maintainer"), db.getPreamble());
            assertEquals(1, db.getStringCount());
        }
    }

    @Test
    void nestedAux() throws URISyntaxException, IOException, JabRefException {
        try (InputStream originalStream = AuxParserTest.class.getResourceAsStream("origin.bib")) {
            Path auxFile = Path.of(AuxParserTest.class.getResource("nested.aux").toURI());
            final BibDatabaseContext bibDatabaseContext = BibDatabaseContext.of(originalStream, importFormatPreferences);
            AuxParser auxParser = new DefaultAuxParser(bibDatabaseContext.getDatabase());
            AuxParserResult auxResult = auxParser.parse(auxFile);

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
    void crossRef() throws URISyntaxException, IOException, JabRefException {
        try (InputStream originalStream = AuxParserTest.class.getResourceAsStream("origin.bib")) {
            Path auxFile = Path.of(AuxParserTest.class.getResource("crossref.aux").toURI());
            final BibDatabaseContext bibDatabaseContext = BibDatabaseContext.of(originalStream, importFormatPreferences);
            AuxParser auxParser = new DefaultAuxParser(bibDatabaseContext.getDatabase());
            AuxParserResult auxResult = auxParser.parse(auxFile);

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
    void fileNotFound() {
        AuxParser auxParser = new DefaultAuxParser(new BibDatabase());
        AuxParserResult auxResult = auxParser.parse(Path.of("unknownfile.aux"));

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
