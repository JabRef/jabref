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
import org.jabref.logic.git.model.BookkeepingResult;
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
    public BookkeepingResult resultRecord(Path bibFilePath, PullPlan computation)
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

            RevCommit head = git.log().setMaxCount(1).call().iterator().next();
            RevCommit localHead = computation.localHead();
            RevCommit remote = computation.remote();
            if (!head.equals(localHead)) {
                throw new JabRefException("Finalize aborted: HEAD moved since prepare. Please re-run pull.");
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
                    return BookkeepingResult.fastForward();
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

    private static boolean blobEqualsCommitPath(Git git, RevCommit commit, String relativePath, Path file) {
        try {
            Optional<String> content = GitFileReader.readFileFromCommit(git, commit, Path.of(relativePath));
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

    private BookkeepingResult commitWithParents(Repository repo,
                                                String branchRef,
                                                ObjectId treeId,
                                                String message,
                                                ObjectId... parents) throws IOException {

        try (ObjectInserter inserter = repo.newObjectInserter()) {
            CommitBuilder cb = new CommitBuilder();
            cb.setTreeId(treeId);
            cb.setParentIds(parents);
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
                    return BookkeepingResult.newCommit(newCommitId.getName());
                default:
                    throw new IOException("Ref update failed: " + r);
            }
        }
    }
}
