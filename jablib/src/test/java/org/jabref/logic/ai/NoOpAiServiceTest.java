package org.jabref.logic.ai;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.plaincitation.PlainCitationParser;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.WordKeywordGroup;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

@NullMarked
class NoOpAiServiceTest {

    private NoOpAiService noOpAiService;

    @BeforeEach
    void setUp() {
        noOpAiService = new NoOpAiService();
    }

    @Test
    void isAvailableReturnsFalse() {
        assertFalse(noOpAiService.isAvailable());
    }

    @Test
    void getLlmPlainCitationParserReturnsEmpty() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        Optional<PlainCitationParser> parser = noOpAiService.getLlmPlainCitationParser(importFormatPreferences);

        assertEquals(Optional.empty(), parser);
    }

    @Test
    void noOpOperationsDoNotThrow() {
        BibDatabaseContext context = mock(BibDatabaseContext.class);
        FilePreferences filePreferences = mock(FilePreferences.class);
        WordKeywordGroup group = mock(WordKeywordGroup.class);

        noOpAiService.setupDatabase(context, false);
        noOpAiService.clearEmbeddingsFor(List.of(), context, filePreferences);
        noOpAiService.generateEmbeddingsForGroup(context, group, List.of(), filePreferences);
        noOpAiService.generateSummariesForEntries(context, List.of(), filePreferences);
        noOpAiService.close();
    }
}
