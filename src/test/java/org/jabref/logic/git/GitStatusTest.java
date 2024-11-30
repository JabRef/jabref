package org.jabref.logic.git;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Set;

import org.jabref.logic.shared.security.Password;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitStatusTest {

    private Git git;
    private Path repositoryPath;
    private GitStatus gitStatus;
    private GitActionExecutor gitActionExecutor;

    private final Logger LOGGER = LoggerFactory.getLogger(GitStatusTest.class);

    @BeforeEach
    void setUp(@TempDir Path temporaryRepository) throws GitAPIException, GeneralSecurityException, UnsupportedEncodingException {
        git = Git.init().setDirectory(temporaryRepository.toFile()).call();
        repositoryPath = temporaryRepository;
        GitPreferences gitPreferences = new GitPreferences(true, "username",
                new Password("password".toCharArray(), "username").encrypt(), false,
                "", false, false, false);
        gitStatus = new GitStatus(git);
        gitActionExecutor = new GitActionExecutor(git, new GitAuthenticator(gitPreferences));
    }

    @AfterEach
    void tearDown() throws IOException {
        git.close();
    }

    @Test
    void hasUntrackedFiles_NoUntrackedFiles() throws GitAPIException, GitException {
        assertFalse(gitStatus.hasUntrackedFiles(), "Expected no untracked files");
    }

    @Test
    void hasUntrackedFiles_WithUntrackedFiles() throws IOException, GitAPIException, GitException {
        Path newFile = Files.createFile(repositoryPath.resolve("untracked.txt"));
        assertTrue(gitStatus.hasUntrackedFiles(), "Expected untracked files");
    }

    @Test
    void getUntrackedFiles_NoUntrackedFiles() throws GitAPIException, GitException {
        Set<String> untrackedFiles = gitStatus.getUntrackedFiles();
        assertTrue(untrackedFiles.isEmpty(), "Expected no untracked files");
    }

    @Test
    void getUntrackedFiles_WithUntrackedFiles() throws IOException, GitAPIException, GitException {
        Path newFile = Files.createFile(repositoryPath.resolve("untracked.txt"));
        Set<String> untrackedFiles = gitStatus.getUntrackedFiles();
        assertTrue(untrackedFiles.contains("untracked.txt"), "Expected 'untracked.txt' in untracked files");
    }

    @Test
    void hasTrackedFiles_NoTrackedFiles() throws GitAPIException, GitException {
        assertFalse(gitStatus.hasTrackedFiles(), "Expected no tracked files");
    }

    @Test
    void hasTrackedFiles_WithTrackedFiles() throws IOException, GitAPIException, GitException {
        Path trackedFile = Files.createFile(repositoryPath.resolve("tracked.txt"));
        git.add().addFilepattern("tracked.txt").call();
        assertTrue(gitStatus.hasTrackedFiles(), "Expected tracked files");
    }

    @Test
    void getTrackedFiles_NoTrackedFiles() throws GitAPIException, GitException {
        Set<String> trackedFiles = gitStatus.getTrackedFiles();
        assertTrue(trackedFiles.isEmpty(), "Expected no tracked files");
    }

    @Test
    void getTrackedFiles_WithTrackedFiles() throws IOException, GitAPIException, GitException {
        Path trackedFile = Files.createFile(repositoryPath.resolve("tracked.txt"));
        git.add().addFilepattern("tracked.txt").call();
        Set<String> trackedFiles = gitStatus.getTrackedFiles();
        assertTrue(trackedFiles.contains("tracked.txt"), "Expected 'tracked.txt' in tracked files");
    }

    @Test
    void getBranchNames_SingleBranch() throws GitAPIException, GitException, IOException {
        Path dummyFile = Files.createFile(repositoryPath.resolve("dummy.txt"));
        git.add().addFilepattern("tracked.txt").call();
        git.commit().setMessage("Dummy commit").call();

        List<String> branchNames = gitStatus.getBranchNames();
        assertEquals(1, branchNames.size(), "Expected one branch");
        assertTrue(branchNames.contains("refs/heads/master"), "Expected 'refs/heads/master' branch");
    }

    @Test
    void getBranchNames_MultipleBranches() throws GitAPIException, GitException, IOException {
        Path dummyFileBranch1 = Files.createFile(repositoryPath.resolve("dummy1.txt"));
        Path dummyFileBranch2 = Files.createFile(repositoryPath.resolve("dummy2.txt"));

        git.add().addFilepattern("dummy1.txt").addFilepattern("tracked.txt").call();
        git.commit().setMessage("Dummy commit").call();

        git.checkout().setCreateBranch(true).setName("develop").call();

        git.add().addFilepattern("dummy2.txt").addFilepattern("tracked.txt").call();
        git.commit().setMessage("Dummy commit").call();

        List<String> branchNames = gitStatus.getBranchNames();
        assertEquals(2, branchNames.size(), "Expected two branches");
        assertTrue(branchNames.contains("refs/heads/master"), "Expected 'refs/heads/master' branch");
        assertTrue(branchNames.contains("refs/heads/develop"), "Expected 'refs/heads/develop' branch");
    }

    @Test
    void trackedAndUntrackedFilesStatus() throws GitException, IOException {
        assertFalse(gitStatus.hasUntrackedFiles());
        assertFalse(gitStatus.hasTrackedFiles());
        Path pathToTempFile = Files.createTempFile(repositoryPath, null, null);
        assertTrue(gitStatus.hasUntrackedFiles());
        assertFalse(gitStatus.hasTrackedFiles());
        gitActionExecutor.add(pathToTempFile);
        assertFalse(gitStatus.hasUntrackedFiles());
        assertTrue(gitStatus.hasTrackedFiles());
        Path tempDir = Files.createTempDirectory(repositoryPath, null);
        Files.createTempFile(tempDir, null, null);
        assertTrue(gitStatus.hasUntrackedFiles());
        assertTrue(gitStatus.hasTrackedFiles());
        assertEquals(1, gitStatus.getUntrackedFiles().size());
        assertEquals(1, gitStatus.getTrackedFiles().size());
    }
}
