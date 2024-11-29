package org.jabref.logic.git;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitManagerTest {

    private Path tempPath;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        this.tempPath = tempDir;
    }

    // Helper methods
    private Git createBareRepository(Path path) throws GitAPIException {
        return Git.init().setBare(true).setDirectory(path.toFile()).call();
    }

    private Git createRepository(Path path) throws GitAPIException {
        return Git.init().setDirectory(path.toFile()).call();
    }

    private void addRemote(Git git, String name, Path remotePath) throws GitAPIException, URISyntaxException {
        git.remoteAdd()
           .setName(name)
           .setUri(new URIish(remotePath.toUri().toString()))
           .call();
    }

    private void addFileAndCommit(Git git, Path filePath, String content, String message) throws Exception {
        Files.writeString(filePath, content);
        git.add().addFilepattern(filePath.getFileName().toString()).call();
        git.commit().setMessage(message).call();
    }

    // Tests
    @Test
    void initGitRepositoryCreatesNewRepositoryWhenNoneExists() {
        assertFalse(GitManager.isGitRepository(tempPath));
        assertThrows(GitException.class, () -> GitManager.openGitRepository(tempPath));
        assertDoesNotThrow(() -> GitManager.initGitRepository(tempPath));
        assertTrue(GitManager.isGitRepository(tempPath));
    }

    @Test
    void initGitRepositoryOpensExistingRepository() throws GitAPIException {
        try (Git git = createRepository(tempPath)) {
            assertThrows(GitException.class, () -> GitManager.initGitRepository(tempPath));
            assertDoesNotThrow(() -> GitManager.openGitRepository(tempPath));
            assertTrue(GitManager.isGitRepository(tempPath));
        }
    }

    @Test
    void findGitRepositoryFindsRepositoryInParentDirectory() throws Exception {
        Path parentRepoPath = tempPath.resolve("parent-repo");
        Path nestedPath = parentRepoPath.resolve("nested/directory/structure");

        Files.createDirectories(nestedPath);
        createRepository(parentRepoPath);

        Optional<Path> foundPath = GitManager.findGitRepository(nestedPath);
        assertTrue(foundPath.isPresent());
        assertEquals(parentRepoPath, foundPath.get());
    }

    @Test
    void synchronizeWithNewFileOnEmptyStage() throws Exception {
        Path localRepoPath = tempPath.resolve("test-local-repo");
        Path remoteRepoPath = tempPath.resolve("test-remote-repo");

        try (Git remoteGit = createBareRepository(remoteRepoPath);
             Git localGit = createRepository(localRepoPath)) {
            addRemote(localGit, "origin", remoteRepoPath);

            Path initialFile = localRepoPath.resolve("initialFile.txt");
            addFileAndCommit(localGit, initialFile, "Initial file content.", "Initial commit");

            localGit.push().setRemote("origin").setRefSpecs(new RefSpec("master:master")).call();

            Path newFile = localRepoPath.resolve("newFile.txt");
            Files.writeString(newFile, "This is a new test file.");

            GitManager gitManager = new GitManager(localGit);
            assertDoesNotThrow(() -> gitManager.synchronize(newFile));

            try (Git clonedRemoteGit = Git.cloneRepository()
                                          .setURI(remoteRepoPath.toUri().toString())
                                          .setDirectory(Files.createTempDirectory("cloned-remote-repo").toFile())
                                          .call()) {
                Path clonedFile = clonedRemoteGit.getRepository().getWorkTree().toPath().resolve("newFile.txt");
                assertTrue(Files.exists(clonedFile));
            }
        }
    }

    @Test
    void synchronizeWithNewFileOnNoneEmptyStage() throws Exception {
        Path localRepoPath = tempPath.resolve("test-local-repo");
        Path remoteRepoPath = tempPath.resolve("test-remote-repo");

        try (Git remoteGit = createBareRepository(remoteRepoPath);
             Git localGit = createRepository(localRepoPath)) {
            addRemote(localGit, "origin", remoteRepoPath);

            Path initialFile = localRepoPath.resolve("initialFile.txt");
            addFileAndCommit(localGit, initialFile, "Initial file content.", "Initial commit");

            localGit.push().setRemote("origin").setRefSpecs(new RefSpec("master:master")).call();

            Path stagedFile = localRepoPath.resolve("File.txt");
            Files.writeString(stagedFile, "This file will be staged but not committed.");
            localGit.add().addFilepattern("File.txt").call();

            Path newFile = localRepoPath.resolve("newFile.txt");
            Files.writeString(newFile, "This is a new test file.");

            GitManager gitManager = new GitManager(localGit);
//            TODO: adjust this after extending the GitManager class
//            assertThrows(GitException.class, () -> gitManager.synchronize(newFile));
        }
    }

    @Test
    void updateWithSuccessfulPullRebase() throws Exception {
        Path localRepoPath = tempPath.resolve("test-local-repo");
        Path remoteRepoPath = tempPath.resolve("test-remote-repo");

        try (Git remoteGit = createBareRepository(remoteRepoPath);
             Git localGit = createRepository(localRepoPath)) {
            addRemote(localGit, "origin", remoteRepoPath);

            Path initialFile = localRepoPath.resolve("initialFile.txt");
            addFileAndCommit(localGit, initialFile, "Initial content", "Initial commit");

            localGit.push().setRemote("origin").setRefSpecs(new RefSpec("master:master")).call();

            GitManager gitManager = new GitManager(localGit);
            assertDoesNotThrow(gitManager::update);
        }
    }
}
