package org.jabref.logic.git.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    void testWriteThenReadBack() throws Exception {
        BibDatabaseContext inputCtx = GitBibParser.parseBibFromGit(
        """
                    @article{a,
                        author = {Alice},
                        title = {Test}
                    }
                  """, importFormatPreferences);

        Path tempFile = Files.createTempFile("tempgitwriter", ".bib");

        GitFileWriter.write(tempFile, inputCtx, importFormatPreferences);

        BibDatabaseContext outputCtx = GitBibParser.parseBibFromGit(Files.readString(tempFile), importFormatPreferences);

        List<BibEntry> inputEntries = inputCtx.getDatabase().getEntries();
        List<BibEntry> outputEntries = outputCtx.getDatabase().getEntries();

        assertEquals(inputEntries.size(), outputEntries.size());
        assertEquals(inputEntries.get(0).getCitationKey(), outputEntries.get(0).getCitationKey());
        assertEquals(inputEntries.get(0).getField(StandardField.AUTHOR), outputEntries.get(0).getField(StandardField.AUTHOR));
    }
}
