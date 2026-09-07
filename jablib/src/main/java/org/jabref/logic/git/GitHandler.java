package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.JabRefException;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FileUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// This class handles the updating of the local and remote git repository that is located at the repository path
/// This provides an easy-to-use interface to manage a git repository
public class GitHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHandler.class);

    static {
        SshAgentConnectorFactory.install();
    }

    final Path repositoryPath;

    final File repositoryPathAsFile;

    private final GitPreferences gitPreferences;

    /// Initialize the handler for the given repository
    ///
    /// @param repositoryPath The root of the initialized git repository
    public GitHandler(Path repositoryPath, GitPreferences gitPreferences) {
        this.repositoryPath = repositoryPath;
        this.repositoryPathAsFile = this.repositoryPath.toFile();
        this.gitPreferences = gitPreferences;
    }

    public void initIfNeeded() {
        if (isGitRepository()) {
            return;
        }
        try {
            // Git.init().call() returns a handle that must be closed before the repository is reopened below.
            Git.init()
               .setDirectory(repositoryPathAsFile)
               .setInitialBranch("main")
               .call()
               .close();
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

    /// Resolves symlinks so repository detection and repository-relative paths refer to the real file.
    /// Falls back to the lexical absolute path when resolution fails (e.g. the file vanished meanwhile).
    public static @NonNull Path resolveToRealPath(@NonNull Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            LOGGER.warn("Could not resolve the library path {} — using it as given", path, e);
            return path.toAbsolutePath().normalize();
        }
    }

    /// Creates the repository and commits `fileToCommit` together with the generated `.gitignore`.
    /// Unlike [#initIfNeeded()] this stages nothing else, so unrelated files in the directory
    /// (PDFs, notes, other libraries) stay untracked until the user adds them deliberately.
    ///
    /// Fails with a [JabRefException] if there already is a repository, or if the file cannot be staged —
    /// an ignore rule excludes it from `git add` silently. Everything this call created is removed again
    /// on failure, so the user can fix the cause and retry, or clone into the directory instead.
    ///
    /// `synchronized` so that concurrent invocations (the GUI submits this as a background task) cannot
    /// interleave: the rollback below may only ever delete a `.git` directory this invocation created,
    /// which the entry check guarantees while the lock is held. The registry hands out one handler per
    /// repository path, so the lock covers all in-process callers.
    public synchronized void initAndCommit(@NonNull Path fileToCommit) throws IOException, GitAPIException, JabRefException {
        // NOFOLLOW_LINKS: a dangling .git symlink is invisible to isGitRepository() but is still a
        // pre-existing user-owned entry — initializing over it would let rollback delete it
        if (isGitRepository() || Files.exists(repositoryPath.resolve(Constants.DOT_GIT), LinkOption.NOFOLLOW_LINKS)) {
            throw new JabRefException(Localization.lang("There already is a Git repository in %0", repositoryPath.toString()));
        }
        Path repositoryRoot = repositoryPath.toAbsolutePath().normalize();
        Path fileInRepository = fileToCommit.toAbsolutePath().normalize();
        if (!fileInRepository.startsWith(repositoryRoot)) {
            throw new JabRefException("%s is not inside the repository root %s".formatted(fileInRepository, repositoryRoot));
        }
        Path gitignore = repositoryRoot.resolve(".gitignore");
        // NOFOLLOW_LINKS: a dangling .gitignore symlink is still a pre-existing user-owned entry that rollback must not delete
        boolean gitignoreExisted = Files.exists(gitignore, LinkOption.NOFOLLOW_LINKS);

        String pathInRepository = relativizeToRepository(fileToCommit);
        try (Git git = Git.init()
                          .setDirectory(repositoryPathAsFile)
                          .setInitialBranch("main")
                          .call()) {
            copyGitIgnoreIfAbsent();
            // A pre-existing .gitignore is user-owned: honor its rules, but do not commit it uninvited
            List<String> pathsToCommit = gitignoreExisted ? List.of(pathInRepository) : List.of(pathInRepository, ".gitignore");
            AddCommand add = git.add();
            pathsToCommit.forEach(add::addFilepattern);
            add.call();
            DirCache index = git.getRepository().readDirCache();
            for (String path : pathsToCommit) {
                if (index.findEntry(path) < 0) {
                    throw new JabRefException(Localization.lang("Could not add %0 to the Git repository. Check the .gitignore file.", path));
                }
            }
            git.commit()
               .setMessage("Initial commit")
               .call();
        } catch (IOException | GitAPIException | JGitInternalException | JabRefException e) {
            LOGGER.debug("Rolling back failed Git repository initialization at {}", repositoryPath, e);
            try {
                FileUtils.delete(repositoryRoot.resolve(Constants.DOT_GIT).toFile(), FileUtils.RECURSIVE | FileUtils.SKIP_MISSING);
                if (!gitignoreExisted) {
                    Files.deleteIfExists(gitignore);
                }
            } catch (IOException cleanupException) {
                LOGGER.warn("Could not clean up after failed Git repository initialization at {}", repositoryPath, cleanupException);
                e.addSuppressed(cleanupException);
            }
            throw e;
        }
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
        try {
            copyGitIgnoreIfAbsent();
        } catch (IOException e) {
            LOGGER.error("Error occurred during copying of the gitignore file into the git repository.", e);
        }
    }

    private void copyGitIgnoreIfAbsent() throws IOException {
        Path gitignore = repositoryPath.resolve(".gitignore");
        if (!Files.exists(gitignore, LinkOption.NOFOLLOW_LINKS)) {
            try (InputStream inputStream = this.getClass().getResourceAsStream("git.gitignore")) {
                Files.copy(inputStream, gitignore);
            }
        }
    }

    /// Returns true if the given path points to a directory that is a git repository (contains a .git folder)
    boolean isGitRepository() {
        // For some reason the solution from https://www.eclipse.org/lists/jgit-dev/msg01892.html does not work
        // This solution is quite simple but might not work in special cases, for us it should suffice.
        return Files.exists(Path.of(repositoryPath.toString(), ".git"));
    }

    /// Checkout the branch with the specified name, if it does not exist create it
    ///
    /// @param branchToCheckout Name of the branch to check out
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

    /// Returns the reference of the specified branch
    /// If it does not exist returns an empty optional
    Optional<Ref> getRefForBranch(String branchName) throws GitAPIException, IOException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            return git.branchList()
                      .call()
                      .stream()
                      .filter(ref -> ref.getName().equals("refs/heads/" + branchName))
                      .findAny();
        }
    }

    /// Creates a commit on the currently checked out branch
    ///
    /// @param amend Whether to amend to the last commit (true), or not (false)
    /// @return Returns true if a new commit was created. This is the case if the repository was not clean on method invocation
    public boolean createCommitOnCurrentBranch(String commitMessage, boolean amend) throws IOException, GitAPIException {
        boolean commitCreated = false;
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            Status status = git.status().call();

            if (!status.isClean()) {
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
            }
        }
        return commitCreated;
    }

    /// Commits `fileToCommit` on the current branch, staging nothing else.
    ///
    /// Unlike [#createCommitOnCurrentBranch(String,boolean)], which stages the whole working tree.
    ///
    /// @return true if a commit was created, false if the file was unchanged
    public boolean createCommitForFileOnCurrentBranch(Path fileToCommit, String commitMessage) throws IOException, GitAPIException {
        String pathInRepository = relativizeToRepository(fileToCommit);
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            if (git.status().addPath(pathInRepository).call().isClean()) {
                return false;
            }
            git.add().addFilepattern(pathInRepository).call();
            git.commit()
               .setOnly(pathInRepository)
               .setAllowEmpty(false)
               .setMessage(commitMessage)
               .call();
            return true;
        }
    }

    /// Merges the source branch into the target branch
    ///
    /// @param targetBranch the name of the branch that is merged into
    /// @param sourceBranch the name of the branch that gets merged
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

    /// Pushes all commits made to the branch that is tracked by the currently checked out branch.
    public void pushCommitsToRemoteRepository() throws IOException, GitAPIException, JabRefException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            Optional<String> urlOpt = currentRemoteUrl(git.getRepository());
            Optional<CredentialsProvider> credsOpt = getCredentialsProvider();

            boolean needCreds = urlOpt.map(GitHandler::requiresCredentialsForUrl).orElse(false);
            if (needCreds && credsOpt.isEmpty()) {
                throw new IOException("Missing Git credentials (username and Personal Access Token).");
            }

            PushCommand pushCommand = git.push();
            credsOpt.ifPresent(pushCommand::setCredentialsProvider);
            LOGGER.info("Pushing current branch to the configured remote.");
            verifyPushResults(pushCommand.call());
            LOGGER.info("Push to the configured remote completed.");
        }
    }

    public void pushCurrentBranchCreatingUpstream() throws IOException, GitAPIException, JabRefException {
        try (Git git = open()) {
            Repository repo = git.getRepository();
            StoredConfig config = repo.getConfig();
            String remoteUrl = config.getString("remote", "origin", "url");

            Optional<CredentialsProvider> credsOpt = getCredentialsProvider();
            boolean needCreds = (remoteUrl != null) && requiresCredentialsForUrl(remoteUrl);
            if (needCreds && credsOpt.isEmpty()) {
                throw new IOException("Missing Git credentials (username and Personal Access Token).");
            }

            String branch = repo.getBranch();

            PushCommand pushCommand = git.push()
                                         .setRemote("origin")
                                         .setRefSpecs(new RefSpec("refs/heads/" + branch + ":refs/heads/" + branch));

            credsOpt.ifPresent(pushCommand::setCredentialsProvider);
            LOGGER.info("Pushing branch {} to origin and configuring its upstream.", branch);
            verifyPushResults(pushCommand.call());

            config.setString("branch", branch, "remote", "origin");
            config.setString("branch", branch, "merge", "refs/heads/" + branch);
            config.save();
            LOGGER.info("Push to origin completed and upstream configured for branch {}.", branch);
        }
    }

    /// Pulls from the current branch’s upstream.
    /// If no remote is configured, silently performs local merge.
    /// This ensures SLR repositories without remotes still initialize correctly.
    public void pullOnCurrentBranch() throws IOException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            Optional<CredentialsProvider> credsOpt = getCredentialsProvider();
            PullCommand pullCommand = git.pull();
            credsOpt.ifPresent(pullCommand::setCredentialsProvider);
            pullCommand.call();
        } catch (GitAPIException e) {
            LOGGER.info("Failed to pull.");
        }
    }

    public String getCurrentlyCheckedOutBranch() throws IOException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            return git.getRepository().getBranch();
        }
    }

    public void fetchOnCurrentBranch() throws JabRefException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            Optional<CredentialsProvider> credentials = getCredentialsProvider();
            boolean needCredentials = currentRemoteUrl(git.getRepository())
                    .map(GitHandler::requiresCredentialsForUrl)
                    .orElse(false);
            if (needCredentials && credentials.isEmpty()) {
                throw new JabRefException("Missing Git credentials (username and Personal Access Token).");
            }
            FetchCommand fetchCommand = git.fetch();
            credentials.ifPresent(fetchCommand::setCredentialsProvider);
            fetchCommand.call();
        } catch (TransportException e) {
            LOGGER.error("Error during transport", e);
            Throwable throwable = e;
            while (throwable != null) {
                if (throwable instanceof NoRemoteRepositoryException) {
                    throw new JabRefException("No repository found at the configured remote. Please check the URL or your token settings.", e);
                }
                throwable = throwable.getCause();
            }
            String message = e.getMessage();
            throw new JabRefException("Failed to fetch from remote: " + (message == null ? "unknown transport error" : message), e);
        } catch (GitAPIException | IOException e) {
            LOGGER.error("Failed to fetch", e);
            throw new JabRefException("Failed to fetch from remote: " + e.getMessage(), e);
        }
    }

    /// Try to locate the Git repository root by walking up the directory tree starting from the given path.
    ///
    /// If a directory containing a .git folder is found, return that path.
    ///
    /// @param anyPathInsideRepo the file or directory path that is assumed to be located inside a Git repository
    /// @return an optional containing the path to the Git repository root if found
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

    public static Optional<GitHandler> fromAnyPath(Path anyPathInsideRepo, GitPreferences gitPreferences) {
        return findRepositoryRoot(anyPathInsideRepo).map(path -> new GitHandler(path, gitPreferences));
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

    public Optional<CredentialsProvider> getCredentialsProvider() {
        if (gitPreferences.getPat().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(
                new UsernamePasswordCredentialsProvider(
                        gitPreferences.getUsername(),
                        gitPreferences.getPat()));
    }

    private static void verifyPushResults(Iterable<PushResult> pushResults) throws JabRefException {
        // [impl->req~git.push.rejected-update-reporting~1]
        for (PushResult pushResult : pushResults) {
            String remoteMessage = pushResult.getMessages();
            if (StringUtil.isNotBlank(remoteMessage)) {
                LOGGER.info("Remote push response: {}", remoteMessage);
            }
            for (RemoteRefUpdate update : pushResult.getRemoteUpdates()) {
                LOGGER.info("Push update for {} completed with status {}.", update.getRemoteName(), update.getStatus());
                if (update.getStatus() != RemoteRefUpdate.Status.OK
                        && update.getStatus() != RemoteRefUpdate.Status.UP_TO_DATE) {
                    String localizedMessage = getLocalizedPushRejectionMessage(update, remoteMessage);
                    LOGGER.warn("Push update for {} was rejected with status {}. Update message: {}. Remote response: {}.",
                            update.getRemoteName(), update.getStatus(), update.getMessage(), remoteMessage);
                    throw new JabRefException("Git push rejected", localizedMessage);
                }
            }
        }
    }

    private static String getLocalizedPushRejectionMessage(RemoteRefUpdate update, String remoteMessage) {
        String updateMessage = update.getMessage();
        if (StringUtil.isNotBlank(updateMessage) && StringUtil.isNotBlank(remoteMessage)) {
            return Localization.lang("Push to %0 was rejected (%1). %2 %3", update.getRemoteName(), update.getStatus(), updateMessage, remoteMessage);
        }
        if (StringUtil.isNotBlank(updateMessage)) {
            return Localization.lang("Push to %0 was rejected (%1). %2", update.getRemoteName(), update.getStatus(), updateMessage);
        }
        if (StringUtil.isNotBlank(remoteMessage)) {
            return Localization.lang("Push to %0 was rejected (%1). %2", update.getRemoteName(), update.getStatus(), remoteMessage);
        }
        return Localization.lang("Push to %0 was rejected (%1).", update.getRemoteName(), update.getStatus());
    }

    /// The Git index always uses forward slashes, independent of the platform.
    private String relativizeToRepository(Path file) {
        return repositoryPath.toAbsolutePath().normalize()
                             .relativize(file.toAbsolutePath().normalize())
                             .toString().replace('\\', '/');
    }
}
