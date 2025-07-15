package org.jabref.logic.git;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHandlerTest {
    @TempDir
    Path repositoryPath;
    @TempDir
    Path remoteRepoPath;
    @TempDir
    Path clonePath;
    private GitHandler gitHandler;

    @BeforeEach
    void setUpGitHandler() throws IOException, GitAPIException, URISyntaxException {
        gitHandler = new GitHandler(repositoryPath);

        remoteRepoPath = Files.createTempDirectory("remote-repo");
        Git remoteGit = Git.init()
                           .setBare(true)
                           .setDirectory(remoteRepoPath.toFile())
                           .call();
        Path testFile = repositoryPath.resolve("initial.txt");
        Files.writeString(testFile, "init");

        gitHandler.createCommitOnCurrentBranch("Initial commit", false);

        try (Git localGit = Git.open(repositoryPath.toFile())) {
            localGit.remoteAdd()
                    .setName("origin")
                    .setUri(new URIish(remoteRepoPath.toUri().toString()))
                    .call();

            localGit.push()
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
                    .call();
        }

        Files.writeString(remoteRepoPath.resolve("HEAD"), "ref: refs/heads/main");
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
        clonePath = Files.createTempDirectory("clone-of-remote");

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

    @Test
    void fromAnyPathFindsGitRootFromNestedPath() throws IOException {
        Path nested = repositoryPath.resolve("src/org/jabref");
        Files.createDirectories(nested);

        Optional<GitHandler> handlerOpt = GitHandler.fromAnyPath(nested);

        assertTrue(handlerOpt.isPresent(), "Expected GitHandler to be created");
        assertEquals(repositoryPath.toRealPath(), handlerOpt.get().repositoryPath.toRealPath(),
                "Expected repositoryPath to match Git root");
    }
}
