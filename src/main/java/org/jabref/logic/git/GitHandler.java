package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.CredentialsProvider;
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
    String gitUsername = Optional.ofNullable(System.getenv("GIT_EMAIL")).orElse("");
    String gitPassword = Optional.ofNullable(System.getenv("GIT_PW")).orElse("");
    final CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(gitUsername, gitPassword);

    /**
     * Initialize the handler for the given repository
     *
     * @param repositoryPath The root of the initialized git repository
     */
    public GitHandler(Path repositoryPath) {
        this(repositoryPath, true);
    }

    /**
     * Initialize the handler for the given repository
     *
     * @param repositoryPath The root of the initialized git repository
     * @param createRepo If true, initializes a repository if the file path does not contain a repository
     */
    public GitHandler(Path repositoryPath, boolean createRepo) {
        if (Files.isRegularFile(repositoryPath)) {
            repositoryPath = repositoryPath.getParent();
        }

        this.repositoryPath = repositoryPath;
        this.repositoryPathAsFile = this.repositoryPath.toFile();

        if (createRepo && !isGitRepository()) {
            try {
                Git.init()
                   .setDirectory(repositoryPathAsFile)
                   .setInitialBranch("main")
                   .call();
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
                LOGGER.error("Initialization failed");
            }
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
    public boolean isGitRepository() {
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
    public void pushCommitsToRemoteRepository() throws IOException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            try {
                git.push()
                   .setCredentialsProvider(credentialsProvider)
                   .call();
            } catch (GitAPIException e) {
                LOGGER.info("Failed to push");
            }
        }
    }

    public boolean pullOnCurrentBranch() throws IOException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            try {
                git.pull()
                   .setCredentialsProvider(credentialsProvider)
                   .call();
            } catch (GitAPIException e) {
                LOGGER.info("Failed to push");
            }
        }
        return false;
    }

    public String getCurrentlyCheckedOutBranch() throws IOException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            return git.getRepository().getBranch();
        }
    }

    /**
     * Represents the Git status of a file within a repository
     */
    public enum GitStatus {
        UNKNOWN("Unknown"),
        MODIFIED("Modified"),
        STAGED("Staged"),
        COMMITTED("Committed"),
        UP_TO_DATE("Up to date"),
        BEHIND_REMOTE("Behind remote"),
        AHEAD_OF_REMOTE("Ahead of remote"),
        UNTRACKED("Untracked");

        private final String displayName;

        GitStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Gets the Git status of a file
     *
     * @param filePath The path to the file to check
     * @return The Git status of the file
     */
    public GitStatus getFileStatus(Path filePath) {
        try {
            if (!isGitRepository()) {
                return GitStatus.UNKNOWN;
            }

            // Check if file exists
            Path absoluteFilePath = filePath.toAbsolutePath().normalize();
            if (!Files.exists(absoluteFilePath)) {
                return GitStatus.UNKNOWN;
            }

            // Get relative path in repository
            String relativePath = getRelativePath(filePath);
            if (relativePath.isEmpty()) {
                return GitStatus.UNKNOWN;
            }

            try (Git git = Git.open(repositoryPathAsFile)) {
                Status status = git.status().call();

                // Check for untracked files
                if (status.getUntracked().contains(relativePath)) {
                    return GitStatus.UNTRACKED;
                }

                // Check for staged files
                if (status.getAdded().contains(relativePath) || status.getChanged().contains(relativePath)) {
                    return GitStatus.STAGED;
                }

                // Check for modified files
                if (status.getModified().contains(relativePath)) {
                    return GitStatus.MODIFIED;
                }

                // If file is in the repository but not modified, staged, or untracked, it must be committed
                return GitStatus.COMMITTED;
            } catch (GitAPIException | IOException e) {
                return GitStatus.UNKNOWN;
            }
        } catch (Exception e) {
            return GitStatus.UNKNOWN;
        }
    }

    /**
     * Gets the relative path of a file to the repository root
     *
     * @param filePath The absolute path to the file
     * @return The relative path to the repository, or empty string if the file is not in the repository
     */
    private String getRelativePath(Path filePath) {
        try {
            Path absoluteFilePath = filePath.toAbsolutePath().normalize();
            Path absoluteRepoPath = repositoryPath.toAbsolutePath().normalize();

            // First try simple relativize
            if (absoluteFilePath.startsWith(absoluteRepoPath)) {
                String relativePathStr = absoluteRepoPath.relativize(absoluteFilePath).toString();
                return relativePathStr;
            }

            // Try with canonical paths for symlinks
            try {
                absoluteFilePath = absoluteFilePath.toFile().getCanonicalFile().toPath();
                absoluteRepoPath = absoluteRepoPath.toFile().getCanonicalFile().toPath();

                if (absoluteFilePath.startsWith(absoluteRepoPath)) {
                    return absoluteRepoPath.relativize(absoluteFilePath).toString();
                } else {
                    return "";
                }
            } catch (IOException e) {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }
}

