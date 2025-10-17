package org.jabref.logic.git.merge;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.git.GitSyncService;
import org.jabref.logic.git.io.GitFileReader;
import org.jabref.logic.git.merge.execution.MergeBookkeeper;
import org.jabref.logic.git.model.BookkeepingResult;
import org.jabref.logic.git.model.PullPlan;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.git.util.NoopGitSystemReader;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.SystemReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Agreement:
/// - We produce a PullPlan through GitSyncService.prepareMerge(...);
/// - The final merged content has been written to disk on the GUI layer;
/// - Then call DefaultMergeBookkeeper.resultRecord(...) to perform "bookkeeping";
/// - Use JGit to verify the commit shape.
public class MergeBookkeeperTest {

    private Path remoteDir;
    private Path localDir;

    private Git remoteGit;
    private Git localGit;

    private Path bibPath;

    private ImportFormatPreferences importPrefs;
    private GitHandlerRegistry handlerRegistry;

    private final PersonIdent localUser = new PersonIdent("Local", "local@example.org");

    private final String initialContent = """
            @article{a,
              author = {init},
            }
            """;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        SystemReader.setInstance(new NoopGitSystemReader());

        importPrefs = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importPrefs.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        handlerRegistry = new GitHandlerRegistry();

        // 1) Remote bare repository
        remoteDir = tempDir.resolve("remote.git");
        remoteGit = Git.init()
                       .setBare(true)
                       .setInitialBranch("main")
                       .setDirectory(remoteDir.toFile())
                       .call();

        // 2) local repository
        localDir = tempDir.resolve("local");
        localGit = Git.init()
                      .setInitialBranch("main")
                      .setDirectory(localDir.toFile())
                      .call();
        bibPath = localDir.resolve("library.bib");

        // initial commit pushed to the remote
        writeAndCommit(localGit, bibPath, initialContent, "init", localUser);
        localGit.remoteAdd()
                .setName("origin")
                .setUri(new URIish(remoteDir.toUri().toString()))
                .call();
        localGit.push()
                .setRemote("origin")
                .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
                .call();
        configureTracking(localGit, "main", "origin");
        // use: `git log --graph --oneline --decorate --all` to check the localDir
    }

    @AfterEach
    void tearDown() {
        if (localGit != null) {
            localGit.close();
        }
        if (remoteGit != null) {
            remoteGit.close();
        }
    }

    // Scene 1: BEHIND + content equals the remote -> fast-forward
    @Test
    void behindAndContentEqualsRemote_fastForward() throws Exception {
        // Preparation: The remote moves forward one step (simulating that the remote has a new commit) while the local has not yet pulled
        String remoteAdvance = """
                @article{a,
                  author = {remote-1},
                }
                """;
        advanceRemote(remoteAdvance, "remote advance");

        // Local: Prepare PullPlan (local BEHIND)
        GitSyncService gitSyncService = GitSyncService.create(importPrefs, handlerRegistry);
        String aliceContent = Files.readString(bibPath);
        BibDatabaseContext bibDatabaseContext = BibDatabaseContext.of(aliceContent, importPrefs);
        bibDatabaseContext.setDatabasePath(bibPath);

        Optional<PullPlan> planOpt = gitSyncService.prepareMerge(bibDatabaseContext, bibPath);
        if (planOpt.isEmpty()) {
            throw new IllegalStateException("PullPlan must not be empty");
        }
        PullPlan plan = planOpt.get();

        // The final content saved in the GUI == remote content (fast-forward scenario)
        Files.writeString(bibPath, remoteAdvance, StandardCharsets.UTF_8);

        // Bookkeeping
        MergeBookkeeper bookkeeper = new MergeBookkeeper(handlerRegistry);
        BookkeepingResult result = bookkeeper.resultRecord(bibPath, plan);

        assertEquals(BookkeepingResult.Kind.FAST_FORWARD, result.kind(), "Expected a fast-forward");
        // assert：HEAD == origin/main
        RevCommit headNow = latestCommit(localGit);
        assertEquals(plan.remote().getId(), headNow.getId(), "HEAD must equal remote commit after FF");
        Ref head = localGit.getRepository().exactRef("refs/heads/main");
        Ref origin = localGit.getRepository().exactRef("refs/remotes/origin/main");
        assertEquals(origin.getObjectId(), head.getObjectId(), "HEAD should fast-forward to origin/main");
        // parent=1
        RevCommit newHead = latestCommit(localGit);
        assertEquals(1, newHead.getParentCount(), "FF should keep single-parent tip");
    }

    // Scene 2: BEHIND + Content ≠ Remote → Create a new commit (single parent) on top of the remote tip
    @Test
    void behindAndContentDiffers_createSingleParentCommitOnRemoteTip() throws Exception {
        String remoteAdvance = """
                @article{a,
                  author = {remote-1},
                }
                """;
        advanceRemote(remoteAdvance, "remote advance");

        GitSyncService gitSyncService = GitSyncService.create(importPrefs, handlerRegistry);
        String localContent = Files.readString(bibPath);
        BibDatabaseContext bibDatabaseContext = BibDatabaseContext.of(localContent, importPrefs);
        bibDatabaseContext.setDatabasePath(bibPath);
        Optional<PullPlan> planOpt = gitSyncService.prepareMerge(bibDatabaseContext, bibPath);
        if (planOpt.isEmpty()) {
            throw new IllegalStateException("PullPlan must not be empty");
        }
        PullPlan plan = planOpt.get();

        // GUI layer has already written the final merged content to disk;
        // in this scenario it intentionally differs from the remote tip
        // (e.g. keeps remote changes and also adds a new local entry).
        String finalMerged = """
                @article{a,
                  author = {remote-1},
                }
                @article{b,
                  author = {added-locally-in-merge},
                }
                """;
        Files.writeString(bibPath, finalMerged, StandardCharsets.UTF_8);

        MergeBookkeeper bookkeeper = new MergeBookkeeper(handlerRegistry);
        BookkeepingResult result = bookkeeper.resultRecord(bibPath, plan);

        assertEquals(BookkeepingResult.Kind.NEW_COMMIT,
                result.kind(),
                "Expected a new single-parent commit on top of remote tip");

        // Assert: The new submitted parent == old tip of origin/main; parentCount == 1
        RevCommit head = latestCommit(localGit);
        assertEquals(1, head.getParentCount(), "Should be a single-parent commit");
        RevCommit parent = head.getParent(0);
        RevCommit originTip = latestCommitOnRemoteTracking(localGit);
        assertEquals(originTip.getId(), parent.getId(), "Parent should be previous origin/main tip");

        String committed = GitFileReader.readFileFromCommit(localGit, head, Path.of("library.bib"))
                                        .orElseThrow(() -> new IllegalStateException("library.bib missing in commit"));
        assertEquals(normalize(finalMerged), normalize(committed), "Committed content must equal saved final content");
    }

    // Scenario 3: DIVERGED -> produce a merge commit with two parents ([localHead, remoteTip])
    @Test
    void diverged_createTwoParentMergeCommit() throws Exception {
        String remoteAdvance = """
                @article{a,
                  author = {remote-1},
                }
                """;
        advanceRemote(remoteAdvance, "remote advance");

        // The local has also changed (resulting in DIVERGED).
        String localAdvance = """
                @article{a,
                  author = {local-1},
                }
                """;
        RevCommit localBeforeMerge = writeAndCommit(localGit, bibPath, localAdvance, "local advance", localUser);
        // equals to `git fetch origin`, only update remote references without modifying the working directory
        localGit.fetch().setRemote("origin").call();

        GitSyncService gitSyncService = GitSyncService.create(importPrefs, handlerRegistry);
        String content = Files.readString(bibPath);
        BibDatabaseContext bibDatabaseContext = BibDatabaseContext.of(content, importPrefs);
        bibDatabaseContext.setDatabasePath(bibPath);
        Optional<PullPlan> planOpt = gitSyncService.prepareMerge(bibDatabaseContext, bibPath);
        if (planOpt.isEmpty()) {
            throw new IllegalStateException("PullPlan must not be empty");
        }
        PullPlan plan = planOpt.get();

        // GUI saved merged final content
        String finalMerged = """
                @article{a,
                  author = {local-1 + remote-1},
                }
                """;
        Files.writeString(bibPath, finalMerged, StandardCharsets.UTF_8);

        MergeBookkeeper bookkeeper = new MergeBookkeeper(handlerRegistry);
        BookkeepingResult result = bookkeeper.resultRecord(bibPath, plan);
        assertEquals(BookkeepingResult.Kind.NEW_COMMIT,
                result.kind(),
                "Expected a merge commit");

        RevCommit head = latestCommit(localGit);
        assertEquals(2, head.getParentCount(), "Should be a two-parent merge commit");

        RevCommit remoteTip = latestCommitOnRemoteTracking(localGit);
        List<String> parentIds = List.of(head.getParent(0).getId().name(), head.getParent(1).getId().name());

        RevCommit parent0 = head.getParent(0);
        RevCommit parent1 = head.getParent(1);
        List<ObjectId> actualParents = List.of(parent0.getId(), parent1.getId());

        assertEquals(
                List.of(localBeforeMerge.getId(), remoteTip.getId()).stream().sorted().toList(),
                actualParents.stream().sorted().toList(),
                "Parents should be [local before merge, origin/main tip]"
        );

        String committed = GitFileReader
                .readFileFromCommit(localGit, head, Path.of("library.bib"))
                .orElseThrow(() -> new IllegalStateException("library.bib missing in commit"));
        assertEquals(normalize(finalMerged), normalize(committed), "Committed content must equal saved final content");
    }

    // ---------- helpers ----------

    private static void configureTracking(Git git, String branch, String remote) throws Exception {
        git.branchCreate().setName(branch).setForce(true).call();
        git.branchCreate().setName(branch).setUpstreamMode(org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
           .setStartPoint(remote + "/" + branch).setForce(true).call();
        git.checkout().setName(branch).call();
    }

    private static RevCommit latestCommit(Git git) throws Exception {
        return git.log().setMaxCount(1).call().iterator().next();
    }

    private static RevCommit latestCommitOnRemoteTracking(Git git) throws Exception {
        Iterable<RevCommit> it = git.log().add(git.getRepository().resolve("refs/remotes/origin/main")).setMaxCount(1).call();
        return it.iterator().next();
    }

    private static RevCommit writeAndCommit(Git git, Path file, String content, String message, PersonIdent who) throws Exception {
        Files.writeString(file, content, StandardCharsets.UTF_8);
        git.add().addFilepattern(file.getFileName().toString()).call();
        return git.commit().setAuthor(who).setMessage(message).call();
    }

    private void advanceRemote(String content, String msg) throws Exception {
        // Clone the bare remote repo into a temporary working directory, then push the new commit back to the bare remote (simulating a remote-only update).
        Path temp = localDir.getParent().resolve("tmp-clone");
        Git tmp = Git.cloneRepository()
                     .setURI(remoteDir.toUri().toString())
                     .setDirectory(temp.toFile())
                     .setBranchesToClone(List.of("refs/heads/main"))
                     .setBranch("main").call();
        Path tmpFile = temp.resolve("library.bib");
        Files.writeString(tmpFile, content, StandardCharsets.UTF_8);
        tmp.add().addFilepattern("library.bib").call();
        tmp.commit().setAuthor(localUser).setMessage(msg).call();
        tmp.push().call();
        tmp.close();

        // fetch from origin in the local repo to update its remote-tracking ref(`refs/remotes/origin/main`) without changing the local branch.
        localGit.fetch().setRemote("origin").call();
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").trim();
    }
}
