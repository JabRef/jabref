package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GitHandlerTest {
    @TempDir
    Path repositoryPath;
    @TempDir
    Path outsideRepositoryPath;
    private GitHandler gitHandler;

    @BeforeEach
    void setUpGitHandler() {
        gitHandler = new GitHandler(repositoryPath);
    }

    @Test
    void checkoutNewBranch() throws IOException, GitAPIException {
        gitHandler.checkoutBranch("testBranch");

        try (Git git = Git.open(repositoryPath.toFile())) {
            assertEquals("testBranch", git.getRepository().getBranch());
        }
    }

    @Test
    void createCommitOnCurrentBranch() throws IOException, GitAPIException {
        try (Git git = Git.open(repositoryPath.toFile())) {
            // Create commit
            Files.createFile(Path.of(repositoryPath.toString(), "Test.txt"));
            gitHandler.createCommitOnCurrentBranch("TestCommit", false);

            AnyObjectId head = git.getRepository().resolve(Constants.HEAD);
            Iterator<RevCommit> log = git.log()
                                         .add(head)
                                         .call().iterator();
            assertEquals("TestCommit", log.next().getFullMessage());
            assertEquals("Initial commit", log.next().getFullMessage());
        }
    }

    @Test
    void getCurrentlyCheckedOutBranch() throws IOException {
        assertEquals("main", gitHandler.getCurrentlyCheckedOutBranch());
    }

    @Test
    void getRelativePathForFileInRepository() throws IOException {
        Path testFile = repositoryPath.resolve("test/file.txt");
        Files.createDirectory(testFile.getParent());
        Files.createFile(testFile);
        assertEquals("test/file.txt", gitHandler.getRelativePath(testFile));
    }

    @Test
    void getRelativePathForFileOutsideRepository() throws IOException {
        Path testFile = outsideRepositoryPath.resolve("test/file.txt");
        Files.createDirectory(testFile.getParent());
        Files.createFile(testFile);
        assertEquals("", gitHandler.getRelativePath(testFile));
    }

    @Test
    void getRelativePathForNonExistentFile() throws IOException {
        Path nonExistentFile = repositoryPath.resolve("non-existent.txt");
        assertEquals("", gitHandler.getRelativePath(nonExistentFile));
    }

    @Test
    void getRelativePathWithNullPath() throws IOException {
        assertEquals("", gitHandler.getRelativePath(null));
    }

    @Test
    void getRelativePathForNestedDirectories() throws IOException {
        Path nestedDir = repositoryPath.resolve("dir1/dir2/dir3");
        Files.createDirectories(nestedDir);

        assertEquals("dir1/dir2/dir3", gitHandler.getRelativePath(nestedDir));
    }
}
