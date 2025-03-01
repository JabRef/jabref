package org.jabref.gui.importer.actions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckForVersionControlActionTest {
    private CheckForVersionControlAction action;
    private ParserResult parserResult;
    private DialogService dialogService;
    private CliPreferences cliPreferences;
    private BibDatabaseContext databaseContext;
    private GitHandler gitHandler;

    @BeforeEach
    void setUp() {
        action = new CheckForVersionControlAction();
        parserResult = mock(ParserResult.class);
        dialogService = mock(DialogService.class);
        cliPreferences = mock(CliPreferences.class);
        databaseContext = mock(BibDatabaseContext.class);
        gitHandler = mock(GitHandler.class);

        when(parserResult.getDatabaseContext()).thenReturn(databaseContext);
    }

    @Test
    void isActionNecessary_WhenDatabasePathIsEmpty_ShouldReturnFalse() {
        when(databaseContext.getDatabasePath()).thenReturn(Optional.empty());

        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);

        assertFalse(result, "Expected isActionNecessary to return false when no database path exists.");
    }

    @Test
    void isActionNecessary_WhenDatabasePathExistsButNotAGitRepo_ShouldReturnFalse() {
        Path mockPath = Path.of("test-repo");
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(mockPath));

        GitHandler gitHandlerMock = mock(GitHandler.class);
        when(gitHandlerMock.isGitRepository()).thenReturn(false);

        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);

        assertFalse(result, "Expected isActionNecessary to return false for a non-Git repository.");
    }

    @Test
    void isActionNecessary_WhenDatabasePathExistsAndIsAGitRepo_ShouldReturnTrue() {
        Path mockPath = Path.of("test-repo");
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(mockPath));

        GitHandler gitHandlerMock = mock(GitHandler.class);
        when(gitHandlerMock.isGitRepository()).thenReturn(true);

        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);

        assertTrue(result, "Expected isActionNecessary to return true for a valid Git repository.");
    }

    @Test
    void performAction_WhenGitPullSucceeds_ShouldNotThrowException() throws IOException {
        Path mockPath = Path.of("test-repo");
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(mockPath));
        when(cliPreferences.shouldAutoPull()).thenReturn(true);

        GitHandler gitHandlerMock = mock(GitHandler.class);
        doNothing().when(gitHandlerMock).pullOnCurrentBranch();

        assertDoesNotThrow(() -> action.performAction(parserResult, dialogService, cliPreferences),
                "Expected performAction to complete without throwing exceptions.");
    }

    @Test
    void performAction_WhenGitPullFails_ShouldLogError() throws IOException {
        Path mockPath = Path.of("test-repo");
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(mockPath));
        when(cliPreferences.shouldAutoPull()).thenReturn(true);

        GitHandler gitHandlerMock = mock(GitHandler.class);
        doThrow(new IOException("Git pull failed")).when(gitHandlerMock).pullOnCurrentBranch();

        Exception exception = assertThrows(RuntimeException.class, () ->
                action.performAction(parserResult, dialogService, cliPreferences));

        assertTrue(exception.getMessage().contains("Git pull failed"),
                "Expected RuntimeException when Git pull fails.");
    }
}
