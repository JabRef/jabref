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
        viewModel = new GitCommitDialogViewModel(
                stateManager,
                dialogService,
                new CurrentThreadTaskExecutor(),
                gitHandlerRegistry,
                mock(ImportFormatPreferences.class),
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
}
