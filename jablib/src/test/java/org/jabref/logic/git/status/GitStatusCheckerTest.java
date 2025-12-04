package org.jabref.logic.git.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.util.GitHandlerRegistry;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.WindowCache;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GitStatusCheckerTest {
    private Path localLibrary;
    private Git localGit;
    private Git remoteGit;
    private Git seedGit;
    private GitHandlerRegistry gitHandlerRegistry;

    private final PersonIdent author = new PersonIdent("Tester", "tester@example.org");

    private final String baseContent = """
            @article{a,
              author = {initial-author},
              doi = {xya},
            }

            @article{b,
              author = {initial-author},
              doi = {xyz},
            }
            """;

    private final String remoteUpdatedContent = """
            @article{a,
              author = {initial-author},
              doi = {xya},
            }

            @article{b,
              author = {remote-update},
              doi = {xyz},
            }
            """;

    private final String localUpdatedContent = """
            @article{a,
              author = {local-update},
              doi = {xya},
            }

            @article{b,
              author = {initial-author},
              doi = {xyz},
            }
            """;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws Exception {
        GitPreferences gitPreferences = mock(GitPreferences.class, Answers.RETURNS_DEEP_STUBS);
        gitHandlerRegistry = new GitHandlerRegistry(gitPreferences);
        Path remoteDir = tempDir.resolve("remote.git");
        remoteGit = Git.init().setBare(true).setDirectory(remoteDir.toFile()).call();

        Path seedDir = tempDir.resolve("seed");
        seedGit = Git.init()
                     .setInitialBranch("main")
                     .setDirectory(seedDir.toFile())
                     .call();
        Path seedFile = seedDir.resolve("library.bib");
        Files.writeString(seedFile, baseContent, StandardCharsets.UTF_8);

        seedGit.add().addFilepattern("library.bib").call();
        seedGit.commit().setAuthor(author).setMessage("Initial commit").call();

        seedGit.remoteAdd()
               .setName("origin")
               .setUri(new URIish(remoteDir.toUri().toString()))
               .call();
        seedGit.push()
               .setRemote("origin")
               .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
               .call();
        Files.writeString(remoteDir.resolve("HEAD"), "ref: refs/heads/main");

        Path localDir = tempDir.resolve("local");
        localGit = Git.cloneRepository()
                      .setURI(remoteDir.toUri().toString())
                      .setDirectory(localDir.toFile())
                      .setBranch("main")
                      .call();
        localGit.branchCreate()
                .setName("main")
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .setStartPoint("origin/main")
                .setForce(true)
                .call();

        this.localLibrary = localDir.resolve("library.bib");
    }

    @AfterEach
    void tearDown() {
        if (seedGit != null) {
            seedGit.close();
        }
        if (localGit != null) {
            localGit.close();
        }
        if (remoteGit != null) {
            remoteGit.close();
        }

        // Required by JGit
        // See https://github.com/eclipse-jgit/jgit/issues/155#issuecomment-2765437816 for details
        RepositoryCache.clear();
        // See https://github.com/eclipse-jgit/jgit/issues/155#issuecomment-3095957214
        WindowCache.reconfigure(new WindowCacheConfig());
    }

    @Test
    void untrackedStatusWhenNotGitRepo(@TempDir Path tempDir) {
        Path nonRepoPath = tempDir.resolve("somefile.bib");

        GitPreferences gitPreferences = mock(GitPreferences.class, Answers.RETURNS_DEEP_STUBS);
        GitStatusSnapshot snapshot = GitStatusChecker.checkStatus(nonRepoPath, gitPreferences);

        assertFalse(snapshot.tracking());
        assertEquals(SyncStatus.UNTRACKED, snapshot.syncStatus());
    }

    @Test
    void upToDateStatusAfterInitialSync() {
        GitHandler gitHandler = gitHandlerRegistry.get(localLibrary.getParent());
        GitStatusSnapshot snapshot = GitStatusChecker.checkStatus(gitHandler);

        assertTrue(snapshot.tracking());
        assertEquals(SyncStatus.UP_TO_DATE, snapshot.syncStatus());
    }

    @Test
    void behindStatusWhenRemoteHasNewCommit(@TempDir Path tempDir) throws Exception {
        Path remoteWork = tempDir.resolve("remoteWork");
        try (Git remoteClone = Git.cloneRepository()
                                  .setURI(remoteGit.getRepository().getDirectory().toURI().toString())
                                  .setDirectory(remoteWork.toFile())
                                  .setBranchesToClone(List.of("refs/heads/main"))
                                  .setBranch("main")
                                  .call()) {
            commitFile(remoteClone, remoteUpdatedContent, "Remote update");
            remoteClone.push()
                       .setRemote("origin")
                       .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
                       .call();
        }
        localGit.fetch().setRemote("origin").call();
        GitHandler gitHandler = gitHandlerRegistry.get(localLibrary.getParent());
        GitStatusSnapshot snapshot = GitStatusChecker.checkStatus(gitHandler);

        assertEquals(SyncStatus.BEHIND, snapshot.syncStatus());
    }

    @Test
    void aheadStatusWhenLocalHasNewCommit() throws Exception {
        commitFile(localGit, localUpdatedContent, "Local update");
        GitHandler gitHandler = gitHandlerRegistry.get(localLibrary.getParent());
        GitStatusSnapshot snapshot = GitStatusChecker.checkStatus(gitHandler);
        assertEquals(SyncStatus.AHEAD, snapshot.syncStatus());
    }

    @Test
    void divergedStatusWhenBothSidesHaveCommits(@TempDir Path tempDir) throws Exception {
        commitFile(localGit, localUpdatedContent, "Local update");

        Path remoteWork = tempDir.resolve("remoteWork");
        try (Git remoteClone = Git.cloneRepository()
                                  .setURI(remoteGit.getRepository().getDirectory().toURI().toString())
                                  .setDirectory(remoteWork.toFile())
                                  .setBranchesToClone(List.of("refs/heads/main"))
                                  .setBranch("main")
                                  .call()) {
            commitFile(remoteClone, remoteUpdatedContent, "Remote update");
            remoteClone.push()
                       .setRemote("origin")
                       .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
                       .call();
        }
        localGit.fetch().setRemote("origin").call();
        GitHandler gitHandler = gitHandlerRegistry.get(localLibrary.getParent());
        GitStatusSnapshot snapshot = GitStatusChecker.checkStatus(gitHandler);
        assertEquals(SyncStatus.DIVERGED, snapshot.syncStatus());
    }

    private RevCommit commitFile(Git git, String content, String message) throws Exception {
        Path file = git.getRepository().getWorkTree().toPath().resolve("library.bib");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        String relativePath = git.getRepository().getWorkTree().toPath().relativize(file).toString();
        git.add().addFilepattern(relativePath).call();
        return git.commit().setAuthor(author).setMessage(message).call();
    }
}
