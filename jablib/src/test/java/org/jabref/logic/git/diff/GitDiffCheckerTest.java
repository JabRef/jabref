package org.jabref.logic.git.diff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.util.NoopGitSystemReader;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.WindowCache;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.util.SystemReader;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("git")
class GitDiffCheckerTest {

    @TempDir
    Path repositoryPath;
    @TempDir
    Path unbornHeadRepositoryPath;

    private GitHandler gitHandler;
    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setUp() throws IOException, GitAPIException {
        SystemReader.setInstance(new NoopGitSystemReader());

        GitPreferences gitPreferences = mock(GitPreferences.class);
        when(gitPreferences.getUsername()).thenReturn("");
        when(gitPreferences.getPat()).thenReturn("");
        gitHandler = new GitHandler(repositoryPath, gitPreferences);
        gitHandler.initIfNeeded();

        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
    }

    @AfterEach
    void cleanUp() {
        RepositoryCache.clear();
        WindowCache.reconfigure(new WindowCacheConfig());
    }

    @Test
    void checkDiffAgainstLastCommitReturnsCommittedBibDatabase() throws IOException, GitAPIException {
        Path bibFile = repositoryPath.resolve("library.bib");
        Files.writeString(bibFile, "@article{key, title={Old title}}\n");
        gitHandler.createCommitOnCurrentBranch("Add library", false);
        Files.writeString(bibFile, "@article{key, title={New title}}\n");

        BibDatabaseContext headDatabase = GitDiffChecker.checkDiffAgainstLastCommit(
                gitHandler,
                Path.of("library.bib"),
                importFormatPreferences,
                new DummyFileUpdateMonitor());
        BibDatabaseContext workingTreeDatabase = GitDiffChecker.checkSavedWorkingTreeVersion(
                bibFile,
                importFormatPreferences,
                new DummyFileUpdateMonitor());

        assertEquals("Old title", headDatabase.getEntries().getFirst().getField(StandardField.TITLE).orElseThrow());
        assertEquals("New title", workingTreeDatabase.getEntries().getFirst().getField(StandardField.TITLE).orElseThrow());
    }

    @Test
    void checkDiffAgainstLastCommitReturnsEmptyWhenFileDidNotExistInHead() throws IOException {
        Path bibFile = repositoryPath.resolve("library.bib");
        Files.writeString(bibFile, "@article{key, title={New title}}\n");

        BibDatabaseContext headDatabase = GitDiffChecker.checkDiffAgainstLastCommit(
                gitHandler,
                Path.of("library.bib"),
                importFormatPreferences,
                new DummyFileUpdateMonitor());

        assertTrue(headDatabase.getEntries().isEmpty());
    }

    @Test
    void checkSavedWorkingTreeVersionReturnsEmptyWhenFileMissing() throws IOException {
        Path bibFile = repositoryPath.resolve("library.bib");

        BibDatabaseContext workingTreeDatabase = GitDiffChecker.checkSavedWorkingTreeVersion(
                bibFile,
                importFormatPreferences,
                new DummyFileUpdateMonitor());

        assertTrue(workingTreeDatabase.getEntries().isEmpty());
    }

    @Test
    void checkDiffAgainstLastCommitReturnsEmptyWhenHeadDoesNotExist() throws IOException, GitAPIException {
        try (Git git = Git.init()
                          .setDirectory(unbornHeadRepositoryPath.toFile())
                          .setInitialBranch("main")
                          .call()) {
            // initialize empty repository
        }

        GitPreferences gitPreferences = mock(GitPreferences.class);
        when(gitPreferences.getUsername()).thenReturn("");
        when(gitPreferences.getPat()).thenReturn("");
        GitHandler unbornHeadHandler = new GitHandler(unbornHeadRepositoryPath, gitPreferences);

        Path bibFile = unbornHeadRepositoryPath.resolve("library.bib");
        Files.writeString(bibFile, "@article{key, title={Unborn head}}\n");

        BibDatabaseContext headDatabase = GitDiffChecker.checkDiffAgainstLastCommit(
                unbornHeadHandler,
                Path.of("library.bib"),
                importFormatPreferences,
                new DummyFileUpdateMonitor());

        assertTrue(headDatabase.getEntries().isEmpty());
    }
}
