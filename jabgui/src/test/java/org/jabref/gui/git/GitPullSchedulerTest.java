package org.jabref.gui.git;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitPullSchedulerTest {

    @TempDir
    Path tempDir;

    @Test
    void startIgnoresLibraryOutsideGitRepository() {
        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(tempDir.resolve("library.bib")));

        start(databaseContext);

        assertFalse(GitPullScheduler.isRunning(databaseContext));
    }

    @Test
    void startIgnoresLibraryWithoutPath() {
        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getDatabasePath()).thenReturn(Optional.empty());

        start(databaseContext);

        assertFalse(GitPullScheduler.isRunning(databaseContext));
    }

    @Test
    void shutdownOfUnscheduledLibraryDoesNothing() {
        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);

        GitPullScheduler.shutdown(databaseContext);

        assertFalse(GitPullScheduler.isRunning(databaseContext));
    }

    private void start(BibDatabaseContext databaseContext) {
        GitPullScheduler.start(databaseContext,
                mock(DialogService.class),
                mock(GuiPreferences.class, RETURNS_DEEP_STUBS),
                mock(StateManager.class),
                new CurrentThreadTaskExecutor(),
                new GitHandlerRegistry(mock(GitPreferences.class)),
                () -> false);
    }
}
