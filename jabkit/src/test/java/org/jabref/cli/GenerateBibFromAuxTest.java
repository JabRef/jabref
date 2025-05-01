package org.jabref.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class GenerateBibFromAuxTest {

    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    @Test
    void test() throws URISyntaxException, IOException {
        InputStream originalStream = GenerateBibFromAuxTest.class.getResourceAsStream("origin.bib");

        Path auxFile = Path.of(GenerateBibFromAuxTest.class.getResource("paper.aux").toURI());
        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences).parse(originalReader);

            BibDatabase newDB = GenerateBibFromAux.createFromAux(result, auxFile);
            assertNotNull(newDB);
            assertEquals(2, newDB.getEntries().size());
        }
    }
}
