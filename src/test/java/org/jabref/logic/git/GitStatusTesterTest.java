package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests Git status detection functionality
 * This test class integrates tests for multiple Git status detection methods
 */
class GitStatusTesterTest {

    @TempDir
    Path tempDir;

    private GitHandler gitHandler;

    @BeforeEach
    void setUp() {
        gitHandler = new GitHandler(tempDir, true);
    }

    @Test
    @DisplayName("Test Git repository initialization and detection")
    void gitRepositoryInitialization() {
        // Verify GitHandler correctly initializes repository
        assertTrue(gitHandler.isGitRepository());
    }

    @Test
    @DisplayName("Test status of untracked files")
    void untrackedFileStatus() throws IOException {
        // Create a new file but don't add to Git
        Path untrackedFile = tempDir.resolve("untracked.txt");
        Files.writeString(untrackedFile, "This is an untracked file");

        // Verify file status should be UNTRACKED
        Optional<GitHandler.GitStatus> status = gitHandler.getFileStatus(untrackedFile);
        assertTrue(status.isPresent());
        assertEquals(GitHandler.GitStatus.UNTRACKED, status.get(), "Untracked file status should be UNTRACKED");
    }

    @Test
    @DisplayName("Test status of staged files")
    void stagedFileStatus() throws IOException, GitAPIException {
        // First make an initial commit
        Path initialFile = tempDir.resolve("initial.txt");
        Files.writeString(initialFile, "Initial commit file");
        try (Git git = Git.open(tempDir.toFile())) {
            git.add().addFilepattern(initialFile.getFileName().toString()).call();
        }
        gitHandler.createCommitOnCurrentBranch("Initial commit", false);

        // Then create a new file and stage it
        Path stagedFile = tempDir.resolve("staged.txt");
        Files.writeString(stagedFile, "This is a staged file");
        try (Git git = Git.open(tempDir.toFile())) {
            git.add().addFilepattern(stagedFile.getFileName().toString()).call();
        }

        // Verify file status is STAGED
        Optional<GitHandler.GitStatus> status = gitHandler.getFileStatus(stagedFile);
        assertTrue(status.isPresent());
        assertEquals(GitHandler.GitStatus.STAGED, status.get(), "Newly staged file status should be STAGED");
    }

    @Test
    @DisplayName("Test status of modified files")
    void modifiedFileStatus() throws IOException, GitAPIException {
        // Create and commit a file
        Path modifiedFile = tempDir.resolve("modified.txt");
        Files.writeString(modifiedFile, "This file will be modified");
        try (Git git = Git.open(tempDir.toFile())) {
            git.add().addFilepattern(modifiedFile.getFileName().toString()).call();
        }
        gitHandler.createCommitOnCurrentBranch("Add file for modification", false);

        // Modify the file without staging
        Files.writeString(modifiedFile, "\nThis is modified content", StandardOpenOption.APPEND);

        // Verify file status is MODIFIED
        Optional<GitHandler.GitStatus> status = gitHandler.getFileStatus(modifiedFile);
        assertTrue(status.isPresent(), "Status should be present for modified file");
        assertEquals(GitHandler.GitStatus.MODIFIED, status.get(), "Modified but unstaged file status should be MODIFIED");
    }

    @Test
    @DisplayName("Test status of committed files")
    void committedFileStatus() throws IOException, GitAPIException {
        // Create, stage and commit a file
        Path committedFile = tempDir.resolve("committed.txt");
        Files.writeString(committedFile, "This is a committed file");
        try (Git git = Git.open(tempDir.toFile())) {
            git.add().addFilepattern(committedFile.getFileName().toString()).call();
        }
        gitHandler.createCommitOnCurrentBranch("Add committed file", false);

        // Verify file status is COMMITTED
        Optional<GitHandler.GitStatus> status = gitHandler.getFileStatus(committedFile);
        assertTrue(status.isPresent(), "Status should be present for committed file");
        assertEquals(GitHandler.GitStatus.COMMITTED, status.get(), "Committed and unmodified file status should be COMMITTED");
    }

    @Test
    @DisplayName("Test status for newly created file")
    void getFileStatusForNewFile() throws IOException, GitAPIException {
        // Create a new file
        Path filePath = tempDir.resolve("NewTest.txt");
        Files.createFile(filePath);

        // Check status for newly created file
        Optional<GitHandler.GitStatus> status = gitHandler.getFileStatus(filePath);
        assertTrue(status.isPresent(), "Status should be present for newly created file");
        assertEquals(GitHandler.GitStatus.UNTRACKED, status.get(), "Newly created file status should be UNTRACKED");
    }

    @Test
    @DisplayName("Test status for file modified after commit")
    void getFileStatusForModifiedFile() throws IOException, GitAPIException {
        // Create a file and commit it first
        Path filePath = tempDir.resolve("ModifiedTest.txt");
        Files.createFile(filePath);
        Files.writeString(filePath, "Initial content");

        try (Git git = Git.open(tempDir.toFile())) {
            git.add().addFilepattern(filePath.getFileName().toString()).call();
        }
        gitHandler.createCommitOnCurrentBranch("Add test file", false);

        // Modify the file
        Files.writeString(filePath, "Modified content");

        // Check status (should be MODIFIED)
        Optional<GitHandler.GitStatus> status = gitHandler.getFileStatus(filePath);
        assertTrue(status.isPresent(), "Status should be present for modified file");
        assertEquals(GitHandler.GitStatus.MODIFIED, status.get(), "Modified file after commit should have MODIFIED status");
    }

    @Test
    @DisplayName("Test Git status detection in BibDatabaseContext")
    void bibDatabaseContextGitStatus() throws IOException, GitAPIException {
        // Create test file
        Path testFile = tempDir.resolve("test_database.bib");
        Files.writeString(testFile, "@Article{test, author = {Test Author}, title = {Test Title}}");

        // Stage and commit file
        try (Git git = Git.open(tempDir.toFile())) {
            git.add().addFilepattern(testFile.getFileName().toString()).call();
        }
        gitHandler.createCommitOnCurrentBranch("Add bib file", false);

        // Set up BibDatabaseContext
        BibDatabaseContext context = new BibDatabaseContext();
        context.setDatabasePath(testFile);
        context.setUnderVersionControl(true);

        // Modify file
        Files.writeString(testFile, "\n@Book{test2, author = {Another Author}, title = {Another Title}}", StandardOpenOption.APPEND);

        // Verify Git status
        Optional<GitHandler.GitStatus> status = context.getGitStatus();
        assertTrue(status.isPresent(), "BibDatabaseContext should have Git status");
        assertEquals(GitHandler.GitStatus.MODIFIED, status.get(), "Modified file status should be MODIFIED");
    }

    @Test
    @DisplayName("Test file status in non-Git repository")
    void nonGitRepositoryFileStatus() throws IOException {
        // Create a temporary directory (non-Git repository)
        Path nonGitDir = Files.createTempDirectory("non-git-dir");
        Path testFile = nonGitDir.resolve("test.txt");
        Files.writeString(testFile, "Test content");

        // Create GitHandler pointing to non-Git repository
        GitHandler nonGitHandler = new GitHandler(testFile, false);

        // Verify it's not a Git repository
        assertFalse(nonGitHandler.isGitRepository(), "Should recognize as non-Git repository");

        // Verify file status is empty for non-Git repository
        Optional<GitHandler.GitStatus> status = nonGitHandler.getFileStatus(testFile);
        assertFalse(status.isPresent(), "Status should be empty for file in non-Git repository");

        // Clean up
        Files.delete(testFile);
        Files.delete(nonGitDir);
    }
}
