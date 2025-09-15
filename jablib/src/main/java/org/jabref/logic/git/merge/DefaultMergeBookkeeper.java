package org.jabref.logic.git.merge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.io.GitFileReader;
import org.jabref.logic.git.model.FinalizeResult;
import org.jabref.logic.git.model.PullComputation;
import org.jabref.logic.git.util.GitHandlerRegistry;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public final class DefaultMergeBookkeeper implements MergeBookkeeper {
    private final GitHandlerRegistry registry;

    // TODO(temporary): Boundaries are messy between orchestrator/GUI/bookkeeper.
    // This class is a minimal "bookkeeping-only" implementation to get the flow working:
    // - GUI writes bytes; we just stage + write a commit with the correct parents, or FF if identical.

    public DefaultMergeBookkeeper(GitHandlerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public FinalizeResult resultRecord(Path bibFilePath, PullComputation computation)
            throws IOException, GitAPIException, JabRefException {

        Optional<Path> repoRoot = GitHandler.findRepositoryRoot(bibFilePath);
        if (repoRoot.isEmpty()) {
            throw new JabRefException("Finalize" +
                    " aborted: Path is not inside a Git repository.");
        }
        GitHandler gitHandler = registry.get(repoRoot.get());

        try (Git git = gitHandler.open()) {
            // Resolve & invariants
            Repository repo = git.getRepository();
            Path workTree = repo.getWorkTree().toPath().toRealPath();
            Path bibPath = bibFilePath.toRealPath();
            if (!bibPath.startsWith(workTree)) {
                throw new JabRefException("Given .bib file is not inside repository");
            }
            String relPath = workTree.relativize(bibPath).toString().replace('\\', '/');

            RevCommit head = git.log().setMaxCount(1).call().iterator().next();
            RevCommit localHead = computation.localHead();
            RevCommit remote = computation.remote();
            if (!head.equals(localHead)) {
                throw new JabRefException("Finalize aborted: HEAD moved since prepare. Please re-run pull.");
            }

            // Stage only the target file (GUI already wrote bytes)
            git.add().addFilepattern(relPath).call();

            // Prepare current index tree for CommitBuilder
            ObjectId treeId;
            try (ObjectInserter oi = repo.newObjectInserter()) {
                DirCache dc = repo.readDirCache();
                treeId = dc.writeTree(oi);
                oi.flush();
            }
            String branchRef = repo.getFullBranch(); // refs/heads/<branch>

            boolean localIsAncestorOfRemote = isAncestor(git, localHead, remote);
            boolean bibEqualsRemote = blobEqualsCommitPath(git, remote, relPath, bibPath);

            if (localIsAncestorOfRemote) { // BEHIND (we know remote is ahead of local)
                if (bibEqualsRemote) {
                    // clean fast-forward; does not change bytes we care about
                    gitHandler.fastForwardTo(remote);
                    return FinalizeResult.fastForward();
                }
                // 本地工作树 != remote：需要把当前索引树作为新提交挂到 remote 之上（单亲）
                return commitWithParents(repo, branchRef, treeId,
                        "Semantic merge (GUI-applied) on top of remote",
                        remote.getId());
            } else {
                // DIVERGED or other -> merge commit with parents [localHead, remote]
                return commitWithParents(repo, branchRef, treeId,
                        "Semantic merge (GUI-applied)",
                        localHead.getId(), remote.getId());
            }
        }
    }

    private static boolean isAncestor(Git git, RevCommit maybeAncestor, RevCommit tip) throws IOException {
        try (RevWalk rw = new RevWalk(git.getRepository())) {
            RevCommit a = rw.parseCommit(maybeAncestor.getId());
            RevCommit b = rw.parseCommit(tip.getId());
            return rw.isMergedInto(a, b); // instance method, not static
        }
    }

    private static boolean blobEqualsCommitPath(Git git, RevCommit commit, String relPath, Path file) {
        try {
            Optional<String> content = GitFileReader.readFileFromCommit(git, commit, Path.of(relPath));
            if (content.isEmpty()) {
                return false;
            }
            String remoteText = content.get();
            String current = Files.readString(file);
            return Objects.equals(remoteText, current);
        } catch (IOException e) {
            return false;
        } catch (JabRefException e) {
            throw new RuntimeException(e);
        }
    }

    private FinalizeResult commitWithParents(Repository repo,
                                             String branchRef,
                                             ObjectId treeId,
                                             String message,
                                             ObjectId... parents) throws IOException {

        try (ObjectInserter inserter = repo.newObjectInserter()) {
            CommitBuilder cb = new CommitBuilder();
            cb.setTreeId(treeId);
            cb.setParentIds(parents); // <-- 指定父提交（单亲或双亲）
            cb.setAuthor(new PersonIdent(repo));
            cb.setCommitter(new PersonIdent(repo));
            cb.setMessage(message);

            ObjectId newCommitId = inserter.insert(cb);
            inserter.flush();

            RefUpdate ru = repo.updateRef(branchRef);
            ObjectId expectedOld = repo.resolve(Constants.HEAD);
            if (expectedOld != null) {
                ru.setExpectedOldObjectId(expectedOld);
            }
            ru.setNewObjectId(newCommitId);
            ru.setRefLogMessage("commit: " + message, false);
            RefUpdate.Result r = ru.update();
            switch (r) {
                case FAST_FORWARD:
                case NEW:
                case FORCED:
                case NO_CHANGE:
                case RENAMED:
                    return FinalizeResult.newCommit(newCommitId.getName());
                default:
                    throw new IOException("Ref update failed: " + r);
            }
        }
    }
}
