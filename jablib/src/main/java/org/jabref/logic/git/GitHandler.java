package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jabref.logic.JabRefException;
import org.jabref.model.strings.StringUtil;

import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the updating of the local and remote git repository that is located at the repository path
 * This provides an easy-to-use interface to manage a git repository
 */
public class GitHandler {
    static final Logger LOGGER = LoggerFactory.getLogger(GitHandler.class);
    final Path repositoryPath;
    final File repositoryPathAsFile;
    private CredentialsProvider credentialsProvider;

    /**
     * Initialize the handler for the given repository
     *
     * @param repositoryPath The root of the initialized git repository
     */
    public GitHandler(Path repositoryPath) {
        this.repositoryPath = repositoryPath;
        this.repositoryPathAsFile = this.repositoryPath.toFile();
    }

    public void initIfNeeded() {
        if (isGitRepository()) {
            return;
        }
        try {
            try (Git git = Git.init()
                              .setDirectory(repositoryPathAsFile)
                              .setInitialBranch("main")
                              .call()) {
                // "git" object is not used later, but we need to close it after initialization
            }
            setupGitIgnore();
            String initialCommit = "Initial commit";
            if (!createCommitOnCurrentBranch(initialCommit, false)) {
                // Maybe, setupGitIgnore failed and did not add something
                // Then, we create an empty commit
                try (Git git = Git.open(repositoryPathAsFile)) {
                    git.commit()
                       .setAllowEmpty(true)
                       .setMessage(initialCommit)
                       .call();
                }
            }
        } catch (GitAPIException | IOException e) {
            LOGGER.error("Git repository initialization failed at {}", repositoryPath, e);
        }
    }

    private Optional<CredentialsProvider> resolveCredentials() {
        if (credentialsProvider != null) {
            return Optional.of(credentialsProvider);
        }

        // TODO: This should be removed - only GitPasswordPreferences should be used
        //       Not implemented in August, 2025, because implications to SLR component not clear.
        String user = Optional.ofNullable(System.getenv("GIT_EMAIL")).orElse("");
        String password = Optional.ofNullable(System.getenv("GIT_PW")).orElse("");

        if (user.isBlank() || password.isBlank()) {
            return Optional.empty();
        }

        this.credentialsProvider = new UsernamePasswordCredentialsProvider(user, password);
        return Optional.of(this.credentialsProvider);
    }

    // TODO: GitHandlerRegistry should get passed GitPasswordPreferences (or similar), pass this to GitHandler instance, which uses it here#
    //       As a result, this method will be gone
    public void setCredentials(String username, String pat) {
        if (username == null) {
            username = "";
        }
        if (pat == null) {
            pat = "";
        }
        this.credentialsProvider = new UsernamePasswordCredentialsProvider(username, pat);
    }

    private static Optional<String> currentRemoteUrl(Repository repo) {
        try {
            StoredConfig config = repo.getConfig();
            String branch = repo.getBranch();

            String remote = config.getString("branch", branch, "remote");
            if (remote == null) {
                Set<String> remotes = config.getSubsections("remote");
                if (remotes.contains("origin")) {
                    remote = "origin";
                } else if (!remotes.isEmpty()) {
                    remote = remotes.iterator().next();
                }
            }
            if (remote == null) {
                return Optional.empty();
            }
            String url = config.getString("remote", remote, "url");
            if (StringUtil.isBlank(url)) {
                return Optional.empty();
            }
            return Optional.of(url);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static boolean requiresCredentialsForUrl(String url) {
        try {
            URIish uri = new URIish(url);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }
            return "https".equalsIgnoreCase(scheme);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    void setupGitIgnore() {
        Path gitignore = Path.of(repositoryPath.toString(), ".gitignore");
        if (!Files.exists(gitignore)) {
            try (InputStream inputStream = this.getClass().getResourceAsStream("git.gitignore")) {
                Files.copy(inputStream, gitignore);
            } catch (IOException e) {
                LOGGER.error("Error occurred during copying of the gitignore file into the git repository.", e);
            }
        }
    }

    /**
     * Returns true if the given path points to a directory that is a git repository (contains a .git folder)
     */
    boolean isGitRepository() {
        // For some reason the solution from https://www.eclipse.org/lists/jgit-dev/msg01892.html does not work
        // This solution is quite simple but might not work in special cases, for us it should suffice.
        return Files.exists(Path.of(repositoryPath.toString(), ".git"));
    }

    /**
     * Checkout the branch with the specified name, if it does not exist create it
     *
     * @param branchToCheckout Name of the branch to check out
     */
    public void checkoutBranch(String branchToCheckout) throws IOException, GitAPIException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            Optional<Ref> branch = getRefForBranch(branchToCheckout);
            git.checkout()
               // If the branch does not exist, create it
               .setCreateBranch(branch.isEmpty())
               .setName(branchToCheckout)
               .call();
        }
    }

    /**
     * Returns the reference of the specified branch
     * If it does not exist returns an empty optional
     */
    Optional<Ref> getRefForBranch(String branchName) throws GitAPIException, IOException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            return git.branchList()
                      .call()
                      .stream()
                      .filter(ref -> ref.getName().equals("refs/heads/" + branchName))
                      .findAny();
        }
    }

    /**
     * Creates a commit on the currently checked out branch
     *
     * @param amend Whether to amend to the last commit (true), or not (false)
     * @return Returns true if a new commit was created. This is the case if the repository was not clean on method invocation
     */
    public boolean createCommitOnCurrentBranch(String commitMessage, boolean amend) throws IOException, GitAPIException {
        boolean commitCreated = false;
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            Status status = git.status().call();
            boolean dirty = !status.isClean();
            RepositoryState state = git.getRepository().getRepositoryState();
            boolean inMerging = (state == RepositoryState.MERGING) || (state == RepositoryState.MERGING_RESOLVED);

            if (dirty) {
                commitCreated = true;
                // Add new and changed files to index
                git.add()
                   .addFilepattern(".")
                   .call();
                // Add all removed files to index
                if (!status.getMissing().isEmpty()) {
                    RmCommand removeCommand = git.rm()
                                                 .setCached(true);
                    status.getMissing().forEach(removeCommand::addFilepattern);
                    removeCommand.call();
                }
                git.commit()
                   .setAmend(amend)
                   .setAllowEmpty(false)
                   .setMessage(commitMessage)
                   .call();
            } else if (inMerging) {
                // No content changes, but merge must be completed (create parent commit)
                commitCreated = true;
                git.commit()
                   .setAmend(amend)
                   .setAllowEmpty(true)
                   .setMessage(commitMessage)
                   .call();
            }
        }
        return commitCreated;
    }

    /**
     * Merges the source branch into the target branch
     *
     * @param targetBranch the name of the branch that is merged into
     * @param sourceBranch the name of the branch that gets merged
     */
    public void mergeBranches(String targetBranch, String sourceBranch, MergeStrategy mergeStrategy) throws IOException, GitAPIException {
        String currentBranch = this.getCurrentlyCheckedOutBranch();
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            Optional<Ref> sourceBranchRef = getRefForBranch(sourceBranch);
            if (sourceBranchRef.isEmpty()) {
                // Do nothing
                return;
            }
            this.checkoutBranch(targetBranch);
            git.merge()
               .include(sourceBranchRef.get())
               .setStrategy(mergeStrategy)
               .setMessage("Merge search branch into working branch.")
               .call();
        }
        this.checkoutBranch(currentBranch);
    }

    /**
     * Pushes all commits made to the branch that is tracked by the currently checked out branch.
     * If pushing to remote fails, it fails silently.
     */
    public void pushCommitsToRemoteRepository() throws IOException, GitAPIException, JabRefException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            Optional<String> urlOpt = currentRemoteUrl(git.getRepository());
            Optional<CredentialsProvider> credsOpt = resolveCredentials();

            boolean needCreds = urlOpt.map(GitHandler::requiresCredentialsForUrl).orElse(false);
            if (needCreds && credsOpt.isEmpty()) {
                throw new IOException("Missing Git credentials (username and Personal Access Token).");
            }

            PushCommand pushCommand = git.push();
            if (credsOpt.isPresent()) {
                pushCommand.setCredentialsProvider(credsOpt.get());
            }
            pushCommand.call();
        }
    }

    public void pushCurrentBranchCreatingUpstream() throws IOException, GitAPIException, JabRefException {
        try (Git git = open()) {
            Repository repo = git.getRepository();
            StoredConfig config = repo.getConfig();
            String remoteUrl = config.getString("remote", "origin", "url");

            Optional<CredentialsProvider> credsOpt = resolveCredentials();
            boolean needCreds = (remoteUrl != null) && requiresCredentialsForUrl(remoteUrl);
            if (needCreds && credsOpt.isEmpty()) {
                throw new IOException("Missing Git credentials (username and Personal Access Token).");
            }

            String branch = repo.getBranch();

            PushCommand pushCommand = git.push()
                                         .setRemote("origin")
                                         .setRefSpecs(new RefSpec("refs/heads/" + branch + ":refs/heads/" + branch));

            if (credsOpt.isPresent()) {
                pushCommand.setCredentialsProvider(credsOpt.get());
            }
            pushCommand.call();
        }
    }

    /// Pulls from the current branch’s upstream.
    /// If no remote is configured, silently performs local merge.
    /// This ensures SLR repositories without remotes still initialize correctly.
    public void pullOnCurrentBranch() throws IOException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            Optional<CredentialsProvider> credsOpt = resolveCredentials();
            PullCommand pullCommand = git.pull();
            if (credsOpt.isPresent()) {
                pullCommand.setCredentialsProvider(credsOpt.get());
            }
            pullCommand.call();
        } catch (GitAPIException e) {
            LOGGER.info("Failed to push");
        }
    }

    public String getCurrentlyCheckedOutBranch() throws IOException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            return git.getRepository().getBranch();
        }
    }

    public void fetchOnCurrentBranch() throws IOException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            Optional<String> urlOpt = currentRemoteUrl(git.getRepository());
            Optional<CredentialsProvider> credsOpt = resolveCredentials();

            boolean needCreds = urlOpt.map(GitHandler::requiresCredentialsForUrl).orElse(false);
            if (needCreds && credsOpt.isEmpty()) {
                throw new IOException("Missing Git credentials (username and Personal Access Token).");
            }
            FetchCommand fetchCommand = git.fetch();
            if (credsOpt.isPresent()) {
                fetchCommand.setCredentialsProvider(credsOpt.get());
            }
            fetchCommand.call();
        } catch (TransportException e) {
            Throwable throwable = e;
            while (throwable != null) {
                if (throwable instanceof NoRemoteRepositoryException) {
                    throw new IOException("No repository found at the configured remote. Please check the URL or your token settings.", e);
                }
                throwable = throwable.getCause();
            }
            String message = e.getMessage();
            throw new IOException("Failed to fetch from remote: " + (message == null ? "unknown transport error" : message), e);
        } catch (GitAPIException e) {
            throw new IOException("Failed to fetch from remote: " + e.getMessage(), e);
        }
    }

    /**
     * Try to locate the Git repository root by walking up the directory tree starting from the given path.
     * <p>
     * If a directory containing a .git folder is found, return that path.
     *
     * @param anyPathInsideRepo the file or directory path that is assumed to be located inside a Git repository
     * @return an optional containing the path to the Git repository root if found
     */
    public static Optional<Path> findRepositoryRoot(Path anyPathInsideRepo) {
        Path current = anyPathInsideRepo.toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve(".git"))) {
                return Optional.of(current);
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    public static Optional<GitHandler> fromAnyPath(Path anyPathInsideRepo) {
        return findRepositoryRoot(anyPathInsideRepo).map(GitHandler::new);
    }

    public File getRepositoryPathAsFile() {
        return repositoryPathAsFile;
    }

    public Git open() throws IOException {
        return Git.open(this.repositoryPathAsFile);
    }

    public boolean hasRemote(String remoteName) {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            return git.getRepository().getConfig()
                      .getSubsections("remote")
                      .contains(remoteName);
        } catch (IOException e) {
            LOGGER.error("Failed to check remote configuration", e);
            return false;
        }
    }

    /// Pre-stage a merge (two parents) but do NOT commit yet.
    /// Equivalent to: `git merge -s ours --no-commit <remote>`
    /// Puts the repo into MERGING state, sets MERGE_HEAD=remote; working tree becomes "ours".
    public void beginOursMergeNoCommit(RevCommit remote) throws IOException, GitAPIException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            git.merge()
               .include(remote)
               .setStrategy(org.eclipse.jgit.merge.MergeStrategy.OURS)
               .setFastForward(org.eclipse.jgit.api.MergeCommand.FastForwardMode.NO_FF)
               .setCommit(false)
               .call();
        }
    }

    /// Fast-forward only to <remote> (when local is strictly behind).
    /// Equivalent to: `git merge --ff-only <remote>`
    public void fastForwardTo(RevCommit remote) throws IOException, GitAPIException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            git.merge()
               .include(remote)
               .setFastForward(org.eclipse.jgit.api.MergeCommand.FastForwardMode.FF_ONLY)
               .setCommit(true)
               .call();
        }
    }

    /// Abort a pre-commit semantic merge in a minimal/safe way:
    /// 1) Clear merge state files (MERGE_HEAD / MERGE_MSG, etc.). Since there is no direct equivalent for git merge --abort in JGit.
    /// 2) Restore ONLY the given file back to HEAD (both index + working tree).
    ///
    /// NOTE: Callers should ensure the working tree was clean before starting,
    /// otherwise this can overwrite the user's uncommitted changes for that file.
    public void abortSemanticMerge(Path absoluteFilePath, boolean allowHardReset) throws IOException, GitAPIException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            Repository repo = git.getRepository();

            // Only act if a branch is actually in a merge state
            RepositoryState state = repo.getRepositoryState();
            boolean inMerging = (state == RepositoryState.MERGING) || (state == RepositoryState.MERGING_RESOLVED);
            if (!inMerging) {
                return;
            }

            // 1) Clear merge state files + possible REVERT/CHERRY_PICK state
            repo.writeMergeCommitMsg(null);
            repo.writeMergeHeads(null);
            repo.writeRevertHead(null);
            repo.writeCherryPickHead(null);

            // 2) Targeted rollback: only restore the file we touched back to HEAD
            Path workTree = repo.getWorkTree().toPath().toRealPath();
            Path targetAbs = absoluteFilePath.toRealPath();
            if (!targetAbs.startsWith(workTree)) {
                return;
            }
            String rel = workTree.relativize(targetAbs).toString().replace('\\', '/');

            // 2.1 Reset the file in the index to HEAD (Equivalent to: `git reset -- <path>`)
            git.reset()
               .addPath(rel)
               .call();

            // 2.2 Restore the file in the working tree from HEAD (Equivalent to: `git checkout -- <path>`)
            git.checkout()
               .setStartPoint("HEAD")
               .addPath(rel)
               .call();
        }
    }

    /// Start a "semantic-merge merge-state" and return a guard:
    public MergeGuard beginSemanticMergeGuard(RevCommit remote, Path bibFilePath) throws IOException, GitAPIException {
        beginOursMergeNoCommit(remote);
        return new MergeGuard(this, bibFilePath);
    }

    public static final class MergeGuard implements AutoCloseable {
        private final GitHandler handler;
        private final Path bibFilePath;
        private final AtomicBoolean active = new AtomicBoolean(true);
        private volatile boolean committed = false;

        private MergeGuard(GitHandler handler, Path bibFilePath) {
            this.handler = handler;
            this.bibFilePath = bibFilePath;
        }

        // Finalize: create the commit (in MERGING this becomes a merge commit with two parents).
        public void commit(String message) throws IOException, GitAPIException {
            if (!active.get()) {
                return;
            }
            handler.createCommitOnCurrentBranch(message, false);
            committed = true;
        }

        // If not committed and still active, best-effort rollback:
        // only this .bib file + clear MERGE_*; never throw from close().
        @Override
        public void close() {
            if (!active.compareAndSet(true, false)) {
                return;
            }
            if (committed) {
                return;
            }
            try {
                handler.abortSemanticMerge(bibFilePath, false);
            } catch (IOException | GitAPIException e) {
                LOGGER.debug("Abort semantic merge failed (best-effort cleanup). path={}", bibFilePath, e);
            } catch (RuntimeException e) {
                // Deliberately catching RuntimeException here because this is a best-effort cleanup in AutoCloseable.close().
                // have to NOT throw from close() to avoid masking the primary failure/result of pull/merge.
                LOGGER.warn("Unexpected runtime exception during cleanup; rethrowing. path={}", bibFilePath, e);
            }
        }
    }
}
