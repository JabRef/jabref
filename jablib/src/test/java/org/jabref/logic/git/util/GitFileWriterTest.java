package org.jabref.logic.git.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.git.io.GitBibParser;
import org.jabref.logic.git.io.GitFileWriter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitFileWriterTest {
    private ImportFormatPreferences importFormatPreferences;
    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
    }

    @Test
    void writeThenReadBack() throws Exception {
        BibDatabaseContext inputDatabaseContext = GitBibParser.parseBibFromGit(
        """
                    @article{a,
                        author = {Alice},
                        title = {Test}
                    }
                  """, importFormatPreferences);

        Path tempFile = Files.createTempFile("tempgitwriter", ".bib");

        GitFileWriter.write(tempFile, inputDatabaseContext, importFormatPreferences);

        BibDatabaseContext outputCtx = GitBibParser.parseBibFromGit(Files.readString(tempFile), importFormatPreferences);

        List<BibEntry> inputEntries = inputDatabaseContext.getDatabase().getEntries();
        List<BibEntry> outputEntries = outputCtx.getDatabase().getEntries();

        assertEquals(inputEntries.size(), outputEntries.size());
        assertEquals(inputEntries.getFirst().getCitationKey(), outputEntries.getFirst().getCitationKey());
        assertEquals(inputEntries.getFirst().getField(StandardField.AUTHOR), outputEntries.getFirst().getField(StandardField.AUTHOR));
    }
}
