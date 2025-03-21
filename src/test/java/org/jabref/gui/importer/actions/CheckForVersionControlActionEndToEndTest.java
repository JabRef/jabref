package org.jabref.gui.importer.actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CheckForVersionControlActionEndToEndTest {
    @TempDir
    Path tempDir;

    private CheckForVersionControlAction action;
    private ParserResult parserResult;
    private DialogService dialogService;
    private CliPreferences cliPreferences;
    private BibDatabaseContext databaseContext;
    private Path gitRepo;

    @BeforeEach
    void setUp() throws IOException, GitAPIException {
        gitRepo = tempDir.resolve("git-test-repo");
        Files.createDirectories(gitRepo);

        Git.init()
           .setDirectory(gitRepo.toFile())
           .call()
           .close();

        Path testFile = gitRepo.resolve("test.bib");
        Files.writeString(testFile, "@article{test, author={Test Author}, title={Test Title}}");

        action = new CheckForVersionControlAction();
        parserResult = mock(ParserResult.class);
        dialogService = mock(DialogService.class);
        cliPreferences = mock(CliPreferences.class);
        databaseContext = mock(BibDatabaseContext.class);

        when(parserResult.getDatabaseContext()).thenReturn(databaseContext);
    }

    @Test
    void isActionNecessary_WhenDatabasePathExistsAndIsAGitRepo_ShouldReturnTrue() {
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(gitRepo.resolve("test.bib")));

        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);

        assertTrue(result);
    }

    @Test
    void isActionNecessary_WhenDatabasePathExistsButNotAGitRepo_ShouldReturnFalse() {
        Path nonGitDir = tempDir.resolve("non-git-dir");
        try {
            Files.createDirectories(nonGitDir);
            Path nonGitFile = nonGitDir.resolve("test.bib");
            Files.writeString(nonGitFile, "@article{test, author={Test Author}, title={Test Title}}");

            when(databaseContext.getDatabasePath()).thenReturn(Optional.of(nonGitFile));

            boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);

            assertFalse(result);
        } catch (IOException e) {
            throw new AssertionError("Failed to set up test directory", e);
        }
    }

    @Test
    void performAction_WhenGitPullSucceeds_ShouldNotThrowException() {
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(gitRepo.resolve("test.bib")));

        action.isActionNecessary(parserResult, dialogService, cliPreferences);

        assertDoesNotThrow(() -> action.performAction(parserResult, dialogService, cliPreferences));
    }
}
