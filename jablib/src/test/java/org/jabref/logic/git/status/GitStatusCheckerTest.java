package org.jabref.logic.git.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitStatusCheckerTest {
    private Path localLibrary;
    private Git localGit;
    private Git remoteGit;
    private Git seedGit;

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
    }

    @Test
    void untrackedStatusWhenNotGitRepo(@TempDir Path tempDir) {
        Path nonRepoPath = tempDir.resolve("somefile.bib");
        GitStatusSnapshot snapshot = GitStatusChecker.checkStatus(nonRepoPath);
        assertFalse(snapshot.tracking());
        assertEquals(SyncStatus.UNTRACKED, snapshot.syncStatus());
    }

    @Test
    void upToDateStatusAfterInitialSync() {
        GitStatusSnapshot snapshot = GitStatusChecker.checkStatus(localLibrary);
        assertTrue(snapshot.tracking());
        assertEquals(SyncStatus.UP_TO_DATE, snapshot.syncStatus());
    }

    @Test
    void behindStatusWhenRemoteHasNewCommit(@TempDir Path tempDir) throws Exception {
        Path remoteWork = tempDir.resolve("remoteWork");
        Git remoteClone = Git.cloneRepository()
                             .setURI(remoteGit.getRepository().getDirectory().toURI().toString())
                             .setDirectory(remoteWork.toFile())
                             .setBranchesToClone(List.of("refs/heads/main"))
                             .setBranch("main")
                             .call();
        Path remoteFile = remoteWork.resolve("library.bib");
        commitFile(remoteClone, remoteUpdatedContent, "Remote update");
        remoteClone.push()
                   .setRemote("origin")
                   .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
                   .call();

        localGit.fetch().setRemote("origin").call();
        GitStatusSnapshot snapshot = GitStatusChecker.checkStatus(localLibrary);
        assertEquals(SyncStatus.BEHIND, snapshot.syncStatus());
    }

    @Test
    void aheadStatusWhenLocalHasNewCommit() throws Exception {
        commitFile(localGit, localUpdatedContent, "Local update");
        GitStatusSnapshot snapshot = GitStatusChecker.checkStatus(localLibrary);
        assertEquals(SyncStatus.AHEAD, snapshot.syncStatus());
    }

    @Test
    void divergedStatusWhenBothSidesHaveCommits(@TempDir Path tempDir) throws Exception {
        commitFile(localGit, localUpdatedContent, "Local update");

        Path remoteWork = tempDir.resolve("remoteWork");
        Git remoteClone = Git.cloneRepository()
                             .setURI(remoteGit.getRepository().getDirectory().toURI().toString())
                             .setDirectory(remoteWork.toFile())
                             .setBranchesToClone(List.of("refs/heads/main"))
                             .setBranch("main")
                             .call();
        Path remoteFile = remoteWork.resolve("library.bib");
        commitFile(remoteClone, remoteUpdatedContent, "Remote update");
        remoteClone.push()
                   .setRemote("origin")
                   .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
                   .call();

        localGit.fetch().setRemote("origin").call();
        GitStatusSnapshot snapshot = GitStatusChecker.checkStatus(localLibrary);
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
