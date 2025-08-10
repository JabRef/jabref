package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.git.prefs.GitPreferences;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
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

    private CredentialsProvider resolveCredentialsOrLoad() throws IOException {
        if (credentialsProvider != null) {
            return credentialsProvider;
        }

        String user = Optional.ofNullable(System.getenv("GIT_EMAIL")).orElse("");
        String password = Optional.ofNullable(System.getenv("GIT_PW")).orElse("");

        GitPreferences preferences = new GitPreferences();
        if (user.isBlank()) {
            user = preferences.getUsername().orElse("");
        }
        if (password.isBlank()) {
            password = preferences.getPersonalAccessToken().orElse("");
        }

        if (user.isBlank() || password.isBlank()) {
            throw new IOException("Missing Git credentials (username, password or PAT).");
        }

        this.credentialsProvider = new UsernamePasswordCredentialsProvider(user, password);
        return this.credentialsProvider;
    }

    public void setCredentials(String username, String pat) {
        if (username == null) {
            username = "";
        }
        if (pat == null) {
            pat = "";
        }
        this.credentialsProvider = new UsernamePasswordCredentialsProvider(username, pat);
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
    public void pushCommitsToRemoteRepository() throws IOException, GitAPIException {
        CredentialsProvider provider = resolveCredentialsOrLoad();
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            git.push()
               .setCredentialsProvider(provider)
               .call();
        }
    }

    public void pushCurrentBranchCreatingUpstream() throws IOException, GitAPIException {
        try (Git git = open()) {
            CredentialsProvider provider = resolveCredentialsOrLoad();
            String branch = git.getRepository().getBranch();
            git.push()
               .setRemote("origin")
               .setCredentialsProvider(provider)
               .setRefSpecs(new RefSpec("refs/heads/" + branch + ":refs/heads/" + branch))
               .call();
        }
    }

    public void pullOnCurrentBranch() throws IOException {
        CredentialsProvider provider = resolveCredentialsOrLoad();
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            try {
                git.pull()
                   .setCredentialsProvider(provider)
                   .call();
            } catch (GitAPIException e) {
                LOGGER.info("Failed to push");
            }
        }
    }

    public String getCurrentlyCheckedOutBranch() throws IOException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            return git.getRepository().getBranch();
        }
    }

    public void fetchOnCurrentBranch() throws IOException {
        CredentialsProvider provider = resolveCredentialsOrLoad();
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            git.fetch()
               .setCredentialsProvider(provider)
               .call();
        } catch (GitAPIException e) {
            LOGGER.error("Failed to fetch from remote", e);
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
}
