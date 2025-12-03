package org.jabref.logic.git;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.util.NoopGitSystemReader;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.WindowCache;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.SystemReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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
        GitPreferences gitPreferences = mock(GitPreferences.class, Answers.RETURNS_DEEP_STUBS);
        gitHandler = new GitHandler(repositoryPath, gitPreferences);

        SystemReader.setInstance(new NoopGitSystemReader());

        try (Git remoteGit = Git.init()
                                .setBare(true)
                                .setDirectory(remoteRepoPath.toFile())
                                .setInitialBranch("main")
                                .call()) {
            // This ensures the remote repository is initialized and properly closed
        }

        gitHandler.initIfNeeded();
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

            localGit.branchCreate()
                    .setName("main")
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                    .setStartPoint("origin/main")
                    .setForce(true)
                    .call();
        }
    }

    @AfterEach
    void cleanUp() {
        // Required by JGit
        // See https://github.com/eclipse-jgit/jgit/issues/155#issuecomment-2765437816 for details
        RepositoryCache.clear();
        // See https://github.com/eclipse-jgit/jgit/issues/155#issuecomment-3095957214
        WindowCache.reconfigure(new WindowCacheConfig());
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
    void fetchOnCurrentBranch() throws IOException, GitAPIException, JabRefException {
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

        GitPreferences gitPreferences = mock(GitPreferences.class, Answers.RETURNS_DEEP_STUBS);
        Optional<GitHandler> handlerOpt = GitHandler.fromAnyPath(nested, gitPreferences);

        assertTrue(handlerOpt.isPresent(), "Expected GitHandler to be created");
        assertEquals(repositoryPath.toRealPath(), handlerOpt.get().repositoryPath.toRealPath(),
                "Expected repositoryPath to match Git root");
    }
}
