package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(GitManager.class);
    private final static String DEFAULT_COMMIT_MESSAGE = "Automatic update via JabRef";
    private final Path path;
    private final GitActionExecutor gitActionExecutor;
    private final GitStatus gitStatus;

    public GitManager(Git git) {
        this.path = git.getRepository().getDirectory().getParentFile().toPath();
        this.gitActionExecutor = new GitActionExecutor(git);
        this.gitStatus = new GitStatus(git);
    }

    public void synchronize(Path filePath) throws GitException {
        if (!gitStatus.hasUntrackedFiles()) {
            LOGGER.debug("No changes detected in {}. Skipping git operations.", path);
            return;
        }
        // TODO: when changes are detected check that given filePath are in untrackedFiles
        if (gitStatus.hasTrackedFiles()) {
            // TODO: stash tracked file and apply stash after commit (with error handling)
            LOGGER.warn("Staging area is not empty.");
            return;
        }
        gitActionExecutor.add(filePath);
        LOGGER.debug("file was added to staging area successfully");
        gitActionExecutor.commit(DEFAULT_COMMIT_MESSAGE, false);
        LOGGER.info("Committed changes for {}", filePath);
        update();
        gitActionExecutor.push();
        LOGGER.debug("{} was pushed successfully", filePath);
    }

    public void update() throws GitException {
        try {
            gitActionExecutor.pull(true);
            LOGGER.debug("Git pull with rebase was successful.");
            return;
        } catch (GitException e) {
            LOGGER.warn("Pull with rebase failed. Attempting to undo changes done by the pull operation...");
            gitActionExecutor.undoPull();
        }
        LOGGER.debug("Attempting pull with merge strategy...");
        gitActionExecutor.pull(false);
        LOGGER.debug("Git pull with merge strategy was successful.");
    }

    /**
     *
     * @return Returns true if the given repository path to the GitManager object to a directory that is a git repository (contains a .git folder)
     */
    public static boolean isGitRepository(Path path) {
        return findGitRepository(path).isPresent();
    }

    public static GitManager openGitRepository(Path path) throws GitException {
        Optional<Path> optionalPath = findGitRepository(path);
        if (optionalPath.isEmpty()) {
            throw new GitException(path.getFileName() + " is not in a git repository.");
        }
        try {
            return new GitManager(Git.open(optionalPath.get().toFile()));
        } catch (IOException e) {
            throw new GitException("Failed to open git repository", e);
        }
    }

    /**
     * Initiates git repository at given path.
     */
    public static GitManager initGitRepository(Path path) throws GitException {
        try {
            if (isGitRepository(path)) {
                throw new GitException(path.getFileName() + " is already a git repository.");
            }
            Git git = Git.init()
                         .setDirectory(path.toFile())
                         .setInitialBranch("main")
                         .call();
            LOGGER.info("Git repository initialized successfully.");
            return new GitManager(git);
        } catch (GitAPIException e) {
            throw new GitException("Initialization of git repository failed", e);
        }
    }

    /**
     * traverse up the directory tree until a .git directory is found or the root is reached.
     *
     * @return to git repository if found or an empty optional otherwise.
     */
    private static Optional<Path> findGitRepository(Path path) {
        Path currentPath = path;

        while (currentPath != null) {
            if (Files.isDirectory(currentPath.resolve(".git"))) {
                LOGGER.warn(currentPath.toString());
                return Optional.of(currentPath);
            }
            currentPath = currentPath.getParent();
        }
        return Optional.empty();
    }

    GitActionExecutor getGitActionExecutor() {
        return this.gitActionExecutor;
    }

    GitStatus getGitStatus() {
        return this.gitStatus;
    }

    Path getPath() {
        return path;
    }
}
