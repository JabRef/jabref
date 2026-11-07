package org.jabref.gui.git;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GitAutoSyncTest {

    private static final Path BIB_FILE = Path.of("library.bib");

    private DialogService dialogService;
    private GitAutoSync gitAutoSync;

    @BeforeEach
    void setUp() {
        dialogService = mock(DialogService.class);
        GitHandlerRegistry gitHandlerRegistry = mock(GitHandlerRegistry.class);
        when(gitHandlerRegistry.fromAnyPath(any())).thenReturn(Optional.empty());

        gitAutoSync = new GitAutoSync(dialogService,
                gitHandlerRegistry,
                new CurrentThreadTaskExecutor(),
                mock(GuiPreferences.class, RETURNS_DEEP_STUBS),
                mock(StateManager.class));
    }

    @Test
    void commitDoesNothingOutsideGitRepository() {
        gitAutoSync.commit(BIB_FILE, mock(BibDatabaseContext.class));

        verifyNoInteractions(dialogService);
    }

    @Test
    void pullDoesNothingOutsideGitRepository() {
        gitAutoSync.pull(BIB_FILE, mock(BibDatabaseContext.class), () -> false);

        verifyNoInteractions(dialogService);
    }
}
