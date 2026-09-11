package org.jabref.logic.git.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("git")
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

    @Test
    void keepsTargetFileWhenContentCannotBeEncoded(@TempDir Path tempDir) throws Exception {
        BibDatabaseContext databaseContext = BibDatabaseContext.of("""
                @article{a,
                    author = {Алиса},
                    title = {Test},
                }
                """, importFormatPreferences);
        databaseContext.getMetaData().setEncoding(StandardCharsets.ISO_8859_1);

        Path target = tempDir.resolve("library.bib");
        Files.writeString(target, "previous content");

        assertThrows(IOException.class, () -> GitFileWriter.write(target, databaseContext, importFormatPreferences));
        assertEquals("previous content", Files.readString(target));
    }
}
