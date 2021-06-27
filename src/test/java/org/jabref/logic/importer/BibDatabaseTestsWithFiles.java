package org.jabref.logic.importer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class BibDatabaseTestsWithFiles {

    private PreferencesService preferencesService;

    @BeforeEach
    public void setUp() {
        preferencesService = mock(PreferencesService.class, Answers.RETURNS_DEEP_STUBS);
    }

    @Test
    public void resolveStrings() throws IOException {
        try (FileInputStream stream = new FileInputStream("src/test/resources/org/jabref/util/twente.bib");
             InputStreamReader fr = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(preferencesService, new DummyFileUpdateMonitor()).parse(fr);

            BibDatabase db = result.getDatabase();

            assertEquals("Arvind", db.resolveForStrings("#Arvind#"));
            assertEquals("Patterson, David", db.resolveForStrings("#Patterson#"));
            assertEquals("Arvind and Patterson, David", db.resolveForStrings("#Arvind# and #Patterson#"));

            // Strings that are not found return just the given string.
            assertEquals("#unknown#", db.resolveForStrings("#unknown#"));
        }
    }
}
