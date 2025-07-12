package org.jabref.logic.git;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHandlerTest {
    @TempDir
    Path repositoryPath;
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
    void fetchOnCurrentBranch() throws IOException, GitAPIException, URISyntaxException {
        Path remoteRepoPath = Files.createTempDirectory("remote-repo");
        try (Git remoteGit = Git.init()
                                .setDirectory(remoteRepoPath.toFile())
                                .setBare(true)
                                .call()) {
            try (Git localGit = Git.open(repositoryPath.toFile())) {
                localGit.remoteAdd()
                        .setName("origin")
                        .setUri(new URIish(remoteRepoPath.toUri().toString()))
                        .call();
            }

            Path testFile = repositoryPath.resolve("test.txt");
            Files.writeString(testFile, "hello");
            gitHandler.createCommitOnCurrentBranch("First commit", false);
            try (Git localGit = Git.open(repositoryPath.toFile())) {
                localGit.push().setRemote("origin").call();
            }

            Path clonePath = Files.createTempDirectory("clone-of-remote");
            try (Git cloneGit = Git.cloneRepository()
                                   .setURI(remoteRepoPath.toUri().toString())
                                   .setDirectory(clonePath.toFile())
                                   .call()) {
                Files.writeString(clonePath.resolve("another.txt"), "world");
                cloneGit.add().addFilepattern("another.txt").call();
                cloneGit.commit().setMessage("Second commit").call();
                cloneGit.push().call();
            }

            gitHandler.fetchOnCurrentBranch();

            try (Git git = Git.open(repositoryPath.toFile())) {
                assertTrue(git.getRepository().getRefDatabase().hasRefs());
                assertTrue(git.getRepository().exactRef("refs/remotes/origin/main") != null);
            }
        }
    }

    @Test
    void fromAnyPathFindsGitRootFromNestedPath() throws IOException {
        // Arrange: create a nested directory structure inside the temp Git repo
        Path nested = repositoryPath.resolve("src/org/jabref");
        Files.createDirectories(nested);

        // Act: attempt to construct GitHandler from nested path
        var handlerOpt = GitHandler.fromAnyPath(nested);

        assertTrue(handlerOpt.isPresent(), "Expected GitHandler to be created");
        assertEquals(repositoryPath.toRealPath(), handlerOpt.get().repositoryPath.toRealPath(),
                "Expected repositoryPath to match Git root");
    }
}
