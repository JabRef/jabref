package org.jabref.logic.git.merge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javafx.collections.FXCollections;

import org.jabref.logic.JabRefException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GitSemanticMergeExecutorTest {

    private BibDatabaseContext base;
    private BibDatabaseContext local;
    private BibDatabaseContext remote;
    private ImportFormatPreferences preferences;
    private GitSemanticMergeExecutor executor;
    private Path tempFile;
    @TempDir
    private Path tempDir;

    @BeforeEach
    public void setup() throws IOException {
        base = new BibDatabaseContext();
        local = new BibDatabaseContext();
        remote = new BibDatabaseContext();

        BibEntry baseEntry = new BibEntry().withCitationKey("Smith2020")
                                           .withField(StandardField.TITLE, "Old Title");
        BibEntry localEntry = new BibEntry(baseEntry);
        BibEntry remoteEntry = new BibEntry(baseEntry);
        remoteEntry.setField(StandardField.TITLE, "New Title");

        base.getDatabase().insertEntry(baseEntry);
        local.getDatabase().insertEntry(localEntry);
        remote.getDatabase().insertEntry(remoteEntry);

        preferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.fieldPreferences().getNonWrappableFields())
                .thenReturn(FXCollections.emptyObservableList());

        executor = new GitSemanticMergeExecutorImpl(preferences);

        tempFile = tempDir.resolve("merged.bib");
    }

    @Test
    public void successfulMergeAndWrite() throws IOException, JabRefException {
        MergePlan plan = executor.merge(base, local, remote, tempFile);

        Map<String, Map<Field, String>> patches = plan.fieldPatches();
        assertEquals(1, patches.size(), "There should be exactly one entry patched");
        Map<Field, String> smithPatch = patches.get("Smith2020");
        assertEquals("New Title", smithPatch.get(StandardField.TITLE), "Title should be updated to 'New Title'");
        assertEquals(List.of(), plan.newEntries(), "No new entries expected");

        String mergedContent = Files.readString(tempFile);
        BibDatabaseContext mergedContext = BibDatabaseContext.of(mergedContent, preferences);

        BibEntry expected = new BibEntry()
                .withCitationKey("Smith2020")
                .withField(StandardField.TITLE, "New Title");

        assertEquals(List.of(expected), mergedContext.getDatabase().getEntries());
    }
}
