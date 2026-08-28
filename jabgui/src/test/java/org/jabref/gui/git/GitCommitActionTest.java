package org.jabref.gui.git;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.StateManager;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitCommitActionTest {

    @TempDir
    Path libraryDirectory;

    @Test
    void committingLibraryOutsideRepositoryInitializesRepositoryAndAddsLibrary() throws Exception {
        Path libraryFile = libraryDirectory.resolve("library.bib");
        Files.writeString(libraryFile, "@Article{test,}");

        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(libraryFile));
        StateManager stateManager = new JabRefGuiStateManager();
        stateManager.activeDatabaseProperty().setValue(Optional.of(databaseContext));

        DialogService dialogService = mock(DialogService.class);
        when(dialogService.showConfirmationDialogAndWait(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        new GitCommitAction(dialogService, stateManager, new GitHandlerRegistry(mock(GitPreferences.class))).execute();

        assertEquals(Optional.of(libraryDirectory.toAbsolutePath()), GitHandler.findRepositoryRoot(libraryFile));
        try (Git git = Git.open(libraryDirectory.toFile())) {
            assertTrue(git.status().call().getUntracked().isEmpty());
        }
    }
}
