package org.jabref.logic.git.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.git.io.GitFileWriter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.eclipse.jgit.util.SystemReader;
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
        SystemReader.setInstance(new NoopGitSystemReader());

        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
    }

    @Test
    void writeThenReadBack() throws Exception {
        BibDatabaseContext inputDatabaseContext = BibDatabaseContext.of("""
                @article{a,
                    author = {Alice},
                    title = {Test},
                }
                """, importFormatPreferences);

        Path tempFile = Files.createTempFile("tempgitwriter", ".bib");
        GitFileWriter.write(tempFile, inputDatabaseContext, importFormatPreferences);

        String written = Files.readString(tempFile);
        BibDatabaseContext parsedContext = BibDatabaseContext.of(written, importFormatPreferences);
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("a")
                .withField(StandardField.AUTHOR, "Alice")
                .withField(StandardField.TITLE, "Test");
        assertEquals(List.of(expected), parsedContext.getDatabase().getEntries());
    }
}
