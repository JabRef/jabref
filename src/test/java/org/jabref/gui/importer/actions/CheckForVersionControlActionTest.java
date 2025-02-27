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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckForVersionControlActionTest {

    @Mock
    private ParserResult parserResult;
    @Mock
    private DialogService dialogService;
    @Mock
    private CliPreferences cliPreferences;
    @Mock
    private BibDatabaseContext databaseContext;
    @Mock
    private GitHandler gitHandler;

    private CheckForVersionControlAction action;

    @BeforeEach
    void setUp() {
        action = new CheckForVersionControlAction();
        when(parserResult.getDatabaseContext()).thenReturn(databaseContext);
    }

    // Test cases for isActionNecessary()

    @Test
    void isActionNecessary_WhenDatabasePathIsEmpty_ShouldReturnFalse() {
        when(databaseContext.getDatabasePath()).thenReturn(Optional.empty());

        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);

        assertFalse(result, "Expected isActionNecessary to return false when no database path exists.");
    }

    @Test
    void isActionNecessary_WhenDatabasePathExistsButNotAGitRepo_ShouldReturnFalse() {
        Path mockPath = Path.of("/path/to/database.bib");
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(mockPath));
        GitHandler mockGitHandler = mock(GitHandler.class);
        when(mockGitHandler.isGitRepository()).thenReturn(false);

        // Inject the mocked GitHandler
        action = new CheckForVersionControlAction();
        action.isActionNecessary(parserResult, dialogService, cliPreferences);

        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);

        assertFalse(result, "Expected isActionNecessary to return false for a non-Git repository.");
    }

    @Test
    void isActionNecessary_WhenDatabasePathExistsAndIsAGitRepo_ShouldReturnTrue() {
        Path mockPath = Path.of("/path/to/database.bib");
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(mockPath));

        GitHandler mockGitHandler = mock(GitHandler.class);
        when(mockGitHandler.isGitRepository()).thenReturn(true);

        // Inject the mocked GitHandler
        action = new CheckForVersionControlAction();
        action.isActionNecessary(parserResult, dialogService, cliPreferences);

        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);

        assertTrue(result, "Expected isActionNecessary to return true for a valid Git repository.");
    }

    // Test cases for performAction()

    @Test
    void performAction_WhenGitPullSucceeds_ShouldNotThrowException() throws IOException {
        Path mockPath = Path.of("/path/to/database.bib");
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(mockPath));

        GitHandler mockGitHandler = mock(GitHandler.class);
        doNothing().when(mockGitHandler).pullOnCurrentBranch();

        // Inject the mocked GitHandler
        action = new CheckForVersionControlAction();
        action.isActionNecessary(parserResult, dialogService, cliPreferences);
        action.performAction(parserResult, dialogService, cliPreferences);

        verify(mockGitHandler, times(1)).pullOnCurrentBranch();
    }

    @Test
    void performAction_WhenGitPullFails_ShouldThrowRuntimeException() throws IOException {
        Path mockPath = Path.of("/path/to/database.bib");
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(mockPath));

        GitHandler mockGitHandler = mock(GitHandler.class);
        doThrow(new IOException("Git pull failed")).when(mockGitHandler).pullOnCurrentBranch();

        action = new CheckForVersionControlAction();
        action.isActionNecessary(parserResult, dialogService, cliPreferences);

        Exception exception = assertThrows(RuntimeException.class, () ->
                action.performAction(parserResult, dialogService, cliPreferences));

        assertTrue(exception.getMessage().contains("Git pull failed"));
    }

    @Test
    void performAction_WhenDatabasePathIsEmpty_ShouldDoNothing() {
        when(databaseContext.getDatabasePath()).thenReturn(Optional.empty());

        action.performAction(parserResult, dialogService, cliPreferences);

        verifyNoInteractions(gitHandler);
    }

    // Additional test case for checking preference behavior (once implemented)
    @Test
    void performAction_WhenPreferenceDisablesAutoPull_ShouldNotPull() throws IOException {
        Path mockPath = Path.of("/path/to/database.bib");
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(mockPath));
        when(cliPreferences.shouldAutoPull()).thenReturn(false);

        GitHandler mockGitHandler = mock(GitHandler.class);
        action = new CheckForVersionControlAction();
        action.isActionNecessary(parserResult, dialogService, cliPreferences);
        action.performAction(parserResult, dialogService, cliPreferences);

        verify(mockGitHandler, never()).pullOnCurrentBranch();
    }
}
