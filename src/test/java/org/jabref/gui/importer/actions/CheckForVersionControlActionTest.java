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
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CheckForVersionControlActionTest {
    @TempDir
    Path tempDir;

    private CheckForVersionControlAction action;
    private ParserResult parserResult;
    private DialogService dialogService;
    private CliPreferences cliPreferences;
    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        action = new CheckForVersionControlAction();
        parserResult = mock(ParserResult.class);
        dialogService = mock(DialogService.class);
        cliPreferences = mock(CliPreferences.class);
        databaseContext = mock(BibDatabaseContext.class);

        when(parserResult.getDatabaseContext()).thenReturn(databaseContext);
    }

    @Test
    void isActionNecessary_WhenDatabasePathIsEmpty_ShouldReturnFalse() {
        when(databaseContext.getDatabasePath()).thenReturn(Optional.empty());

        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);

        assertFalse(result);
    }

    @Test
    void isActionNecessary_WhenDatabasePathExistsButNotAGitRepo_ShouldReturnFalse() {
        Path testRepo = tempDir.resolve("test-repo");
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(testRepo));

        GitHandler gitHandlerMock = mock(GitHandler.class);
        when(gitHandlerMock.isGitRepository()).thenReturn(false);

        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);

        assertFalse(result);
    }

    @Test
    void isActionNecessary_WhenDatabasePathExistsAndIsAGitRepo_ShouldReturnTrue() {
        GitHandler gitHandlerMock = mock(GitHandler.class);
        when(gitHandlerMock.isGitRepository()).thenReturn(true);

        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);

        assertTrue(result);
    }

    @Test
    void performAction_WhenGitPullSucceeds_ShouldNotThrowException() throws IOException {
        Path testRepo = tempDir.resolve("test-repo");
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(testRepo));

        GitHandler gitHandlerMock = mock(GitHandler.class);
        doNothing().when(gitHandlerMock).pullOnCurrentBranch();

        assertDoesNotThrow(() -> action.performAction(parserResult, dialogService, cliPreferences));
    }

    @Test
    void performAction_WhenGitPullFails_ShouldHandleIOException() throws IOException {
        Path testRepo = tempDir.resolve("test-repo");
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(testRepo));

        GitHandler gitHandlerMock = mock(GitHandler.class);
        doThrow(new IOException("Git pull failed")).when(gitHandlerMock).pullOnCurrentBranch();

        assertThrows(IOException.class, () ->
                action.performAction(parserResult, dialogService, cliPreferences));
    }
}
