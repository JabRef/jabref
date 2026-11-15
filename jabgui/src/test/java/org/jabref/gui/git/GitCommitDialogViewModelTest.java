package org.jabref.gui.git;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.WindowCache;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("git")
class GitCommitDialogViewModelTest {

    private DialogService dialogService;
    private StateManager stateManager;
    private GitHandlerRegistry gitHandlerRegistry;
    private GitCommitDialogViewModel viewModel;

    @BeforeEach
    void setUp() {
        dialogService = mock(DialogService.class);
        stateManager = mock(StateManager.class);
        GitPreferences gitPreferences = mock(GitPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(gitPreferences.getPat()).thenReturn("");
        gitHandlerRegistry = new GitHandlerRegistry(gitPreferences);
        // Deep stubs so when parsing .bib files in diffTask() can read the keyword separator and avoid NullPointerException
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        viewModel = new GitCommitDialogViewModel(
                stateManager,
                dialogService,
                new CurrentThreadTaskExecutor(),
                gitHandlerRegistry,
                importFormatPreferences,
                new DummyFileUpdateMonitor());
    }

    @AfterEach
    void tearDown() {
        RepositoryCache.clear();
        WindowCache.reconfigure(new WindowCacheConfig());
    }

    @Test
    void indexLockShowsLocalizedCommitError(@TempDir Path tempDir) throws Exception {
        Path repoDir = tempDir.resolve("repo");
        Path libraryFile = repoDir.resolve("library.bib");
        Files.createDirectories(repoDir);

        try (Git git = Git.init().setInitialBranch("main").setDirectory(repoDir.toFile()).call()) {
            Files.writeString(libraryFile, "@article{a, title={initial}}\n");
            git.add().addFilepattern("library.bib").call();
            git.commit().setMessage("Initial commit").call();
        }

        Files.writeString(libraryFile, "@article{a, title={changed}}\n");
        Files.writeString(repoDir.resolve(".git").resolve("index.lock"), "locked");

        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(libraryFile));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));

        viewModel.commit(() -> {
        });

        verify(dialogService).showErrorDialogAndWait(
                Localization.lang("Git commit failed"),
                Localization.lang("The Git repository is locked. Close other Git, JabRef, or IDE processes and try again."));
    }

    // [utest->req~git.commit.generated-message~1]
    @Test
    void prefillsGeneratedCommitMessage(@TempDir Path tempDir) throws Exception {
        Path libraryFile = commitLibrary(tempDir, "@article{Smith2024, author={Smith}, title={Git basics}}\n");
        Files.writeString(libraryFile, "@article{Smith2024, author={Smith, John}, title={Git basics}}\n");

        viewModel.prefillGeneratedCommitMessage();

        assertEquals("Modify author of Smith2024", viewModel.commitMessageProperty().get());
    }

    @Test
    void prefillKeepsMessageTypedByUser(@TempDir Path tempDir) throws Exception {
        Path libraryFile = commitLibrary(tempDir, "@article{Smith2024, author={Smith}, title={Git basics}}\n");
        Files.writeString(libraryFile, "@article{Smith2024, author={Smith, John}, title={Git basics}}\n");
        viewModel.commitMessageProperty().set("My own message");

        viewModel.prefillGeneratedCommitMessage();

        assertEquals("My own message", viewModel.commitMessageProperty().get());
    }

    // [utest->req~git.commit.generated-message~1]
    @Test
    void commitBeforePrefillFinishesUsesGeneratedMessage(@TempDir Path tempDir) throws Exception {
        Path libraryFile = commitLibrary(tempDir, "@article{Smith2024, author={Smith}, title={Git basics}}\n");
        Files.writeString(libraryFile, "@article{Smith2024, author={Smith, John}, title={Git basics}}\n");

        viewModel.commit(() -> {
        });

        assertEquals("Modify author of Smith2024", lastCommitMessage(tempDir));
    }

    @Test
    void commitKeepsMessageTypedByUser(@TempDir Path tempDir) throws Exception {
        Path libraryFile = commitLibrary(tempDir, "@article{Smith2024, author={Smith}, title={Git basics}}\n");
        Files.writeString(libraryFile, "@article{Smith2024, author={Smith, John}, title={Git basics}}\n");
        viewModel.commitMessageProperty().set("My own message");

        viewModel.commit(() -> {
        });

        assertEquals("My own message", lastCommitMessage(tempDir));
    }

    private String lastCommitMessage(Path tempDir) throws Exception {
        try (Git git = Git.open(tempDir.resolve("repo").toFile())) {
            return git.log().setMaxCount(1).call().iterator().next().getFullMessage();
        }
    }

    private Path commitLibrary(Path tempDir, String content) throws Exception {
        Path repoDir = tempDir.resolve("repo");
        Path libraryFile = repoDir.resolve("library.bib");
        Files.createDirectories(repoDir);

        try (Git git = Git.init().setInitialBranch("main").setDirectory(repoDir.toFile()).call()) {
            Files.writeString(libraryFile, content);
            git.add().addFilepattern("library.bib").call();
            git.commit().setMessage("Initial commit").call();
        }

        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(libraryFile));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));
        return libraryFile;
    }
}
