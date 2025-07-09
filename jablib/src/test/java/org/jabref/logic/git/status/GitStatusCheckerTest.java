package org.jabref.logic.git.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
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

        Path localDir = tempDir.resolve("local");
        localGit = Git.cloneRepository()
                      .setURI(remoteDir.toUri().toString())
                      .setDirectory(localDir.toFile())
                      .setBranch("main")
                      .call();

        this.localLibrary = localDir.resolve("library.bib");

        // Initial commit
        commitFile(localGit, baseContent, "Initial commit");

        // Push to remote
        localGit.push().setRemote("origin").call();
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
                             .call();
        Path remoteFile = remoteWork.resolve("library.bib");
        commitFile(remoteClone, remoteUpdatedContent, "Remote update");
        remoteClone.push().setRemote("origin").call();

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
                             .call();
        Path remoteFile = remoteWork.resolve("library.bib");
        commitFile(remoteClone, remoteUpdatedContent, "Remote update");
        remoteClone.push().setRemote("origin").call();

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
