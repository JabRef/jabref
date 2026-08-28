package org.jabref.gui.git;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.StateManager;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.Git;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class GitCommitActionTest {

    @TempDir
    Path libraryDirectory;

    @Test
    void committingLibraryOutsideRepositoryInitializesRepositoryAndAddsOnlyTheLibrary() throws Exception {
        Path libraryFile = libraryDirectory.resolve("library.bib");
        Files.writeString(libraryFile, "@Article{test,}");
        Path unrelatedFile = libraryDirectory.resolve("notes.txt");
        Files.writeString(unrelatedFile, "not part of the library");

        executeCommitAction(libraryFile, true);

        assertEquals(Optional.of(libraryDirectory.toAbsolutePath()), GitHandler.findRepositoryRoot(libraryFile));
        try (Git git = Git.open(libraryDirectory.toFile())) {
            // library.bib and the generated .gitignore are committed, everything else stays untracked
            assertEquals(Set.of("notes.txt"), git.status().call().getUntracked());
        }
    }

    @Test
    void cancellingLeavesTheDirectoryUntouched() throws Exception {
        Path libraryFile = libraryDirectory.resolve("library.bib");
        Files.writeString(libraryFile, "@Article{test,}");

        executeCommitAction(libraryFile, false);

        assertEquals(Optional.empty(), GitHandler.findRepositoryRoot(libraryFile));
        try (Stream<Path> files = Files.list(libraryDirectory)) {
            assertEquals(Set.of(libraryFile), files.collect(Collectors.toSet()));
        }
    }

    private void executeCommitAction(Path libraryFile, boolean confirmInitialization) {
        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(libraryFile));
        StateManager stateManager = new JabRefGuiStateManager();
        stateManager.activeDatabaseProperty().setValue(Optional.of(databaseContext));

        DialogService dialogService = mock(DialogService.class);
        when(dialogService.showConfirmationDialogAndWait(anyString(), anyString(), anyString(), anyString())).thenReturn(confirmInitialization);

        new GitCommitAction(dialogService, stateManager, new GitHandlerRegistry(mock(GitPreferences.class)), new CurrentThreadTaskExecutor()).execute();
    }
}
