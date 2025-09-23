package org.jabref.logic.git.merge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.io.GitFileReader;
import org.jabref.logic.git.io.GitRevisionLocator;
import org.jabref.logic.git.model.FinalizeResult;
import org.jabref.logic.git.model.PullPlan;
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

public final class DefaultMergeBookkeeper implements MergeBookkeeper {
    private final GitHandlerRegistry registry;

    public DefaultMergeBookkeeper(GitHandlerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public FinalizeResult resultRecord(Path bibFilePath, PullPlan computation)
            throws IOException, GitAPIException, JabRefException {

        Optional<Path> repoRoot = GitHandler.findRepositoryRoot(bibFilePath);
        if (repoRoot.isEmpty()) {
            throw new JabRefException("Finalize aborted: Path is not inside a Git repository.");
        }
        GitHandler gitHandler = registry.get(repoRoot.get());

        try (Git git = gitHandler.open()) {
            Repository repo = git.getRepository();
            Path workTree = repo.getWorkTree().toPath().toRealPath();
            Path bibPath = bibFilePath.toRealPath();
            if (!bibPath.startsWith(workTree)) {
                throw new JabRefException("Given .bib file is not inside repository");
            }
            String relativePath = workTree.relativize(bibPath).toString().replace('\\', '/');

            RevCommit headNow = git.log().setMaxCount(1).call().iterator().next();
            RevCommit localHead = computation.localHead();
            RevCommit remote = computation.remote();
            if (!headNow.equals(localHead)) {
                throw new JabRefException(
                        "Finalize aborted: Repository HEAD changed since prepare. Please re-run Pull.\n\n"
                                + "expected: " + localHead.getId().abbreviate(10).name() + "\n"
                                + "actual  : " + headNow.getId().abbreviate(10).name()
                );
            }

            // Stage only the target file (GUI already wrote bytes)
            git.add().addFilepattern(relativePath).call();

            // Prepare current index tree for CommitBuilder
            ObjectId treeId;
            try (ObjectInserter objectInserter = repo.newObjectInserter()) {
                DirCache dirCache = repo.readDirCache();
                treeId = dirCache.writeTree(objectInserter);
                objectInserter.flush();
            }
            String branchRef = repo.getFullBranch(); // refs/heads/<branch>

            boolean localIsAncestorOfRemote = GitRevisionLocator.isAncestor(repo, localHead.getId(), remote.getId());
            boolean bibEqualsRemote = blobEqualsCommitPath(git, remote, relativePath, bibPath);

            if (localIsAncestorOfRemote) { // BEHIND (we know remote is ahead of local)
                if (bibEqualsRemote) {
                    gitHandler.fastForwardTo(remote);
                    return FinalizeResult.fastForward();
                }
                // Local working tree != remote: The current index tree needs to be attached as a new commit on top of the remote (single parent).
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
            throw new RuntimeException(e); // TODO: do not thwrow runtime exception
        }
    }

    private FinalizeResult commitWithParents(Repository repo,
                                             String branchRef,
                                             ObjectId treeId,
                                             String message,
                                             ObjectId... parents) throws IOException {

        try (ObjectInserter inserter = repo.newObjectInserter()) {
            CommitBuilder commitBuilder = new CommitBuilder();
            commitBuilder.setTreeId(treeId);
            commitBuilder.setParentIds(parents);
            commitBuilder.setAuthor(new PersonIdent(repo));
            commitBuilder.setCommitter(new PersonIdent(repo));
            commitBuilder.setMessage(message);

            ObjectId newCommitId = inserter.insert(commitBuilder);
            inserter.flush();

            RefUpdate refUpdate = repo.updateRef(branchRef);
            ObjectId expectedOld = repo.resolve(Constants.HEAD);
            if (expectedOld != null) {
                refUpdate.setExpectedOldObjectId(expectedOld);
            }
            refUpdate.setNewObjectId(newCommitId);
            refUpdate.setRefLogMessage("commit: " + message, false);
            RefUpdate.Result updated = refUpdate.update();
            switch (updated) {
                case FAST_FORWARD:
                case NEW:
                case FORCED:
                case NO_CHANGE:
                case RENAMED:
                    return FinalizeResult.newCommit(newCommitId.getName());
                default:
                    throw new IOException("Ref update failed: " + updated);
            }
        }
    }
}
