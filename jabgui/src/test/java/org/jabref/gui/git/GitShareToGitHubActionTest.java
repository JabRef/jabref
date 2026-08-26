package org.jabref.gui.git;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.StateManager;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitShareToGitHubActionTest {

    private StateManager stateManager;
    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        stateManager = new JabRefGuiStateManager();
        databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(Path.of("library.bib")));
        stateManager.activeDatabaseProperty().setValue(Optional.of(databaseContext));

        GitHandlerRegistry gitHandlerRegistry = mock(GitHandlerRegistry.class);
        GitHandler gitHandler = mock(GitHandler.class);
        when(gitHandlerRegistry.fromAnyPath(Path.of("library.bib"))).thenReturn(Optional.of(gitHandler));
        when(gitHandler.hasRemote("origin")).thenReturn(true);
        Injector.setModelOrService(GitHandlerRegistry.class, gitHandlerRegistry);
    }

    @Test
    void isEnabledForSavedLocalLibraryWithRemote() {
        GitShareToGitHubAction action = new GitShareToGitHubAction(mock(DialogService.class), stateManager);

        assertTrue(action.executableProperty().get());
    }
}
