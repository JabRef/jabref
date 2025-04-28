package org.jabref.logic.importer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class BibDatabaseFilesTest {

    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    @Test
    void resolveStrings() throws IOException {
        try (InputStream stream = Files.newInputStream(Path.of("src/test/resources/org/jabref/util/twente.bib"));
             InputStreamReader fr = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences).parse(fr);

            BibDatabase db = result.getDatabase();

            assertEquals("Arvind", db.resolveForStrings("#Arvind#"));
            assertEquals("Patterson, David", db.resolveForStrings("#Patterson#"));
            assertEquals("Arvind and Patterson, David", db.resolveForStrings("#Arvind# and #Patterson#"));

            // Strings that are not found return just the given string.
            assertEquals("#unknown#", db.resolveForStrings("#unknown#"));
        }
    }
}
