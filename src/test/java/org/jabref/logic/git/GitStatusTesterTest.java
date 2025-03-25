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

class GitStatusTesterTest {

    @TempDir
    Path tempDir;

    @TempDir
    Path nonGitTempDir;

    private GitHandler gitHandler;
    private GitHandler nonGitHandler;

    @BeforeEach
    void setUp() {
        gitHandler = new GitHandler(tempDir, true);
        nonGitHandler = new GitHandler(nonGitTempDir, false);
    }

    @Test
    @DisplayName("Test Git repository initialization and detection")
    void gitRepositoryInitialization() {
        assertTrue(gitHandler.isGitRepository());
        assertFalse(nonGitHandler.isGitRepository());
    }

    @Test
    @DisplayName("Test status of modified files")
    void modifiedFileStatus() throws IOException, GitAPIException {
        Path modifiedFile = tempDir.resolve("modified.txt");
        Files.writeString(modifiedFile, "This file will be modified");
        try (Git git = Git.open(tempDir.toFile())) {
            git.add().addFilepattern(modifiedFile.getFileName().toString()).call();
        }
        gitHandler.createCommitOnCurrentBranch("Add file for modification", false);

        Files.writeString(modifiedFile, "\nThis is modified content", StandardOpenOption.APPEND);
        Optional<GitHandler.GitStatus> status = gitHandler.getFileStatus(modifiedFile);
        assertTrue(status.isPresent());
        assertEquals(GitHandler.GitStatus.MODIFIED, status.get());
    }

    @Test
    @DisplayName("Test status of committed files")
    void committedFileStatus() throws IOException, GitAPIException {
        Path committedFile = tempDir.resolve("committed.txt");
        Files.writeString(committedFile, "This is a committed file");
        try (Git git = Git.open(tempDir.toFile())) {
            git.add().addFilepattern(committedFile.getFileName().toString()).call();
        }
        gitHandler.createCommitOnCurrentBranch("Add committed file", false);
        Optional<GitHandler.GitStatus> status = gitHandler.getFileStatus(committedFile);
        assertTrue(status.isPresent());
        assertEquals(GitHandler.GitStatus.COMMITTED, status.get());
    }

    @Test
    @DisplayName("BibDatabaseContext.getGitStatus integration test")
    void bibDatabaseContextGetGitStatusTest() throws IOException, GitAPIException {
        // Create a test file in the repository
        Path bibFile = tempDir.resolve("database.bib");
        Files.writeString(bibFile, "@Article{test, author={Test}}\n");

        // Add and commit the file
        try (Git git = Git.open(tempDir.toFile())) {
            git.add().addFilepattern(bibFile.getFileName().toString()).call();
            git.commit().setMessage("Add test file").call();
        }

        // Setup BibDatabaseContext with the file
        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(bibFile);

        // Test getGitStatus
        Optional<GitHandler.GitStatus> status = database.getGitStatus();
        assertTrue(status.isPresent());
        assertEquals(GitHandler.GitStatus.COMMITTED, status.get());
    }
    
    @Test
    @DisplayName("Test status for file modified after commit")
    void getFileStatusForModifiedFile() throws IOException, GitAPIException {
        Path filePath = tempDir.resolve("ModifiedTest.txt");
        Files.createFile(filePath);
        Files.writeString(filePath, "Initial content");

        try (Git git = Git.open(tempDir.toFile())) {
            git.add().addFilepattern(filePath.getFileName().toString()).call();
        }
        gitHandler.createCommitOnCurrentBranch("Add test file", false);

        Files.writeString(filePath, "Modified content");
        Optional<GitHandler.GitStatus> status = gitHandler.getFileStatus(filePath);
        assertTrue(status.isPresent());
        assertEquals(GitHandler.GitStatus.MODIFIED, status.get());
    }

    @Test
    @DisplayName("Test getGitStatus method with various file states")
    void getGitStatusMethodTest() throws IOException, GitAPIException {
        Path bibFile = tempDir.resolve("git_status_test.bib");
        Files.writeString(bibFile, "@Article{test, author = {Test Author}, title = {Test Title}}");

        try (Git git = Git.open(tempDir.toFile())) {
            git.add().addFilepattern(bibFile.getFileName().toString()).call();
        }
        gitHandler.createCommitOnCurrentBranch("Add test bib file", false);

        BibDatabaseContext committedContext = new BibDatabaseContext();
        committedContext.setDatabasePath(bibFile);
        Optional<GitHandler.GitStatus> committedStatus = committedContext.getGitStatus();
        assertTrue(committedStatus.isPresent());
        assertEquals(GitHandler.GitStatus.COMMITTED, committedStatus.get());

        Files.writeString(bibFile, "\n@Book{modified, author = {Modified Author}, title = {Modified Title}}", StandardOpenOption.APPEND);
        BibDatabaseContext modifiedContext = new BibDatabaseContext();
        modifiedContext.setDatabasePath(bibFile);
        Optional<GitHandler.GitStatus> modifiedStatus = modifiedContext.getGitStatus();
        assertTrue(modifiedStatus.isPresent());
        assertEquals(GitHandler.GitStatus.MODIFIED, modifiedStatus.get());

        try (Git git = Git.open(tempDir.toFile())) {
            git.add().addFilepattern(bibFile.getFileName().toString()).call();
        }
        Path nonExistentFile = tempDir.resolve("non_existent.bib");
        BibDatabaseContext nonExistentContext = new BibDatabaseContext();
        nonExistentContext.setDatabasePath(nonExistentFile);
        Optional<GitHandler.GitStatus> nonExistentStatus = nonExistentContext.getGitStatus();
        assertFalse(nonExistentStatus.isPresent());
    }

    @Test
    @DisplayName("Test file status in non-Git repository")
    void nonGitRepositoryFileStatus() throws IOException {
        Path testFile = nonGitTempDir.resolve("test.txt");
        Files.writeString(testFile, "Test content");

        GitHandler nonGitHandler = new GitHandler(testFile, false);

        assertFalse(nonGitHandler.isGitRepository());
        Optional<GitHandler.GitStatus> status = nonGitHandler.getFileStatus(testFile);
        assertFalse(status.isPresent());
    }
}
