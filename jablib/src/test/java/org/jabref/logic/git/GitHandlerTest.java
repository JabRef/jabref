package org.jabref.logic.git;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.JabRefException;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.util.NoopGitSystemReader;
import org.jabref.logic.l10n.Localization;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.WindowCache;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.SystemReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("git")
class GitHandlerTest {
    @TempDir
    Path repositoryPath;
    @TempDir
    Path remoteRepoPath;
    @TempDir
    Path clonePath;
    @TempDir
    Path libraryPath;
    private GitHandler gitHandler;

    @BeforeEach
    void setUpGitHandler() throws IOException, GitAPIException, URISyntaxException {
        GitPreferences gitPreferences = mock(GitPreferences.class);
        when(gitPreferences.getUsername()).thenReturn("");
        when(gitPreferences.getPat()).thenReturn("");
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

    // [utest->req~git.commit.initialize-repository~1]
    @Test
    void initAndCommitTracksOnlyTheGivenFile() throws Exception {
        Path libraryFile = libraryPath.resolve("library.bib");
        Files.writeString(libraryFile, "@Article{test,}");
        Files.writeString(libraryPath.resolve("notes.txt"), "unrelated");
        GitHandler handler = new GitHandler(libraryPath, mock(GitPreferences.class, Answers.RETURNS_DEEP_STUBS));

        handler.initAndCommit(libraryFile);

        try (Git git = Git.open(libraryPath.toFile())) {
            assertEquals(Set.of(".gitignore", "library.bib"), committedPaths(git));
            assertEquals(Set.of("notes.txt"), git.status().call().getUntracked());
        }
    }

    // [utest->req~git.commit.initialize-repository~1]
    @Test
    void initAndCommitRejectsAFileOutsideTheRepository(@TempDir Path otherDirectory) throws Exception {
        Path libraryFile = otherDirectory.resolve("library.bib");
        Files.writeString(libraryFile, "@Article{test,}");
        GitHandler handler = new GitHandler(libraryPath, mock(GitPreferences.class, Answers.RETURNS_DEEP_STUBS));

        assertThrows(JabRefException.class, () -> handler.initAndCommit(libraryFile));

        assertFalse(Files.exists(libraryPath.resolve(".git")));
    }

    // [utest->req~git.commit.initialize-repository~1]
    @Test
    void initAndCommitTracksANestedFile() throws Exception {
        Path libraryFile = Files.createDirectory(libraryPath.resolve("sub")).resolve("library.bib");
        Files.writeString(libraryFile, "@Article{test,}");
        GitHandler handler = new GitHandler(libraryPath, mock(GitPreferences.class, Answers.RETURNS_DEEP_STUBS));

        handler.initAndCommit(libraryFile);

        try (Git git = Git.open(libraryPath.toFile())) {
            assertEquals(Set.of(".gitignore", "sub/library.bib"), committedPaths(git));
        }
    }

    // [utest->req~git.commit.initialize-repository~1]
    @Test
    @DisabledOnOs(OS.WINDOWS)
    // creating symlinks requires elevated rights on Windows
    void initAndCommitRefusesADanglingDotGitSymlink() throws Exception {
        Path libraryFile = libraryPath.resolve("library.bib");
        Files.writeString(libraryFile, "@Article{test,}");
        Path dotGit = libraryPath.resolve(".git");
        Files.createSymbolicLink(dotGit, libraryPath.resolve("missing-target"));
        GitHandler handler = new GitHandler(libraryPath, mock(GitPreferences.class, Answers.RETURNS_DEEP_STUBS));

        assertThrows(JabRefException.class, () -> handler.initAndCommit(libraryFile));

        assertTrue(Files.exists(dotGit, LinkOption.NOFOLLOW_LINKS));
    }

    // [utest->req~git.commit.initialize-repository~1]
    @Test
    void initAndCommitKeepsAPreexistingGitignoreUncommitted() throws Exception {
        Path libraryFile = libraryPath.resolve("library.bib");
        Files.writeString(libraryFile, "@Article{test,}");
        Files.writeString(libraryPath.resolve(".gitignore"), "notes.txt");
        GitHandler handler = new GitHandler(libraryPath, mock(GitPreferences.class, Answers.RETURNS_DEEP_STUBS));

        handler.initAndCommit(libraryFile);

        try (Git git = Git.open(libraryPath.toFile())) {
            assertEquals(Set.of("library.bib"), committedPaths(git));
            assertEquals(Set.of(".gitignore"), git.status().call().getUntracked());
        }
    }

    // [utest->req~git.commit.initialize-repository~1]
    @Test
    void initAndCommitRemovesItsTracesWhenTheFileIsIgnored() throws Exception {
        Path libraryFile = libraryPath.resolve("library.bib");
        Files.writeString(libraryFile, "@Article{test,}");
        Path gitignore = libraryPath.resolve(".gitignore");
        Files.writeString(gitignore, "*.bib");
        GitHandler handler = new GitHandler(libraryPath, mock(GitPreferences.class, Answers.RETURNS_DEEP_STUBS));

        assertThrows(JabRefException.class, () -> handler.initAndCommit(libraryFile));

        assertFalse(Files.exists(libraryPath.resolve(".git")));
        assertEquals("*.bib", Files.readString(gitignore));
    }

    private static Set<String> committedPaths(Git git) throws IOException {
        Repository repository = git.getRepository();
        Set<String> paths = new HashSet<>();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(repository.resolve(Constants.HEAD + "^{tree}"));
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                paths.add(treeWalk.getPathString());
            }
        }
        return paths;
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
    void pushReportsRejectedRemoteUpdate() throws IOException, GitAPIException {
        try (Git cloneGit = Git.cloneRepository()
                               .setURI(remoteRepoPath.toUri().toString())
                               .setDirectory(clonePath.toFile())
                               .call()) {
            Files.writeString(clonePath.resolve("remote.txt"), "remote change");
            cloneGit.add().addFilepattern("remote.txt").call();
            cloneGit.commit().setMessage("Remote commit").call();
            cloneGit.push().call();
        }

        Files.writeString(repositoryPath.resolve("local.txt"), "local change");
        gitHandler.createCommitOnCurrentBranch("Local commit", false);

        JabRefException exception = assertThrows(JabRefException.class, gitHandler::pushCommitsToRemoteRepository);

        assertEquals(
                Localization.lang("Push to %0 was rejected (%1).", "refs/heads/main", RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD),
                exception.getLocalizedMessage());
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

    @Test
    void commitForFileStagesOnlyThatFile() throws Exception {
        Path libraryFile = repositoryPath.resolve("library.bib");
        Files.writeString(libraryFile, "@Article{test,}");
        Files.writeString(repositoryPath.resolve("notes.txt"), "unrelated");

        assertTrue(gitHandler.createCommitForFileOnCurrentBranch(libraryFile, "Update references"));

        try (Git git = Git.open(repositoryPath.toFile())) {
            assertTrue(committedPaths(git).contains("library.bib"));
            assertEquals(Set.of("notes.txt"), git.status().call().getUntracked());
        }
    }

    @Test
    void commitForFileDoesNothingWhenUnchanged() throws Exception {
        Path libraryFile = repositoryPath.resolve("library.bib");
        Files.writeString(libraryFile, "@Article{test,}");
        gitHandler.createCommitForFileOnCurrentBranch(libraryFile, "Update references");

        assertFalse(gitHandler.createCommitForFileOnCurrentBranch(libraryFile, "Update references"));
    }
}
