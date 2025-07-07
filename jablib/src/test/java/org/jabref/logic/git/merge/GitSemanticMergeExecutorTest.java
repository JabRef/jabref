package org.jabref.logic.git.merge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.git.model.MergeResult;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class GitSemanticMergeExecutorTest {

    private BibDatabaseContext base;
    private BibDatabaseContext local;
    private BibDatabaseContext remote;
    private ImportFormatPreferences preferences;
    private GitSemanticMergeExecutor executor;
    private Path tempFile;

    @BeforeEach
    public void setup() throws IOException {
        base = new BibDatabaseContext();
        local = new BibDatabaseContext();
        remote = new BibDatabaseContext();

        BibEntry baseEntry = new BibEntry().withCitationKey("Smith2020").withField(StandardField.TITLE, "Old Title");
        BibEntry localEntry = (BibEntry) baseEntry.clone();
        BibEntry remoteEntry = (BibEntry) baseEntry.clone();
        remoteEntry.setField(StandardField.TITLE, "New Title");

        base.getDatabase().insertEntry(baseEntry);
        local.getDatabase().insertEntry(localEntry);
        remote.getDatabase().insertEntry(remoteEntry);

        preferences = mock(ImportFormatPreferences.class);
        executor = new GitSemanticMergeExecutorImpl(preferences);

        tempFile = Files.createTempFile("merged", ".bib");
        tempFile.toFile().deleteOnExit();
    }

    @Test
    public void successfulMergeAndWrite() throws IOException {
        MergeResult result = executor.merge(base, local, remote, tempFile);

        assertTrue(result.isSuccessful());
        String content = Files.readString(tempFile);
        assertTrue(content.contains("New Title"));
    }
}
