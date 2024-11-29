package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.HttpTransport;
import org.eclipse.jgit.transport.SshTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(GitManager.class);
    private final static String DEFAULT_COMMIT_MESSAGE = "Automatic update via JabRef";
  
    private final Path path;
    private final Git git;
    private final GitPreferences preferences;
    private final GitAuthenticator gitAuthenticator;
    private final GitActionExecutor gitActionExecutor;
    private final GitStatus gitStatus;

    private GitProtocol gitProtocol = GitProtocol.UNKNOWN;
    private boolean requiresAuthentication = false;


    public GitManager(Git git, GitPreferences preferences) {
        this.path = git.getRepository().getDirectory().getParentFile().toPath();

        this.git = git;
        this.gitAuthenticator = new GitAuthenticator(preferences);
        this.gitActionExecutor = new GitActionExecutor(this.git, this.gitAuthenticator);
        this.gitStatus = new GitStatus(this.git);
        this.preferences = preferences;
        testConnection();
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

    public static GitManager openGitRepository(Path path, GitPreferences gitPreferences) throws GitException {
        Optional<Path> optionalPath = findGitRepository(path);
        if (optionalPath.isEmpty()) {
            throw new GitException(path.getFileName() + " is not in a git repository.");
        }
        try {
            return new GitManager(Git.open(optionalPath.get().toFile()), gitPreferences);
        } catch (IOException e) {
            throw new GitException("Failed to open git repository", e);
        }
    }

    /**
     * Initiates git repository at given path.
     */
    public static GitManager initGitRepository(Path path, GitPreferences gitPreferences)
            throws GitException {
        try {
            if (isGitRepository(path)) {
                throw new GitException(path.getFileName() + " is already a git repository.");
            }
            Git git = Git.init()
                         .setDirectory(path.toFile())
                         .setInitialBranch("main")
                         .call();
            LOGGER.info("Git repository initialized successfully.");
            return new GitManager(git, gitPreferences);
        } catch (GitAPIException e) {
            throw new GitException("Initialization of git repository failed", e);
        }
    }

    /**
     * traverse up the directory tree until a .git directory is found or the root is reached.
     *
     * @return to git repository if found or an empty optional otherwise.
     */
    static Optional<Path> findGitRepository(Path path) {
        Path currentPath = path;

        while (currentPath != null) {
            if (Files.isDirectory(currentPath.resolve(".git"))) {
                return Optional.of(currentPath);
            }
            currentPath = currentPath.getParent();
        }
        return Optional.empty();

    public boolean requiresAuthentication() {
        return requiresAuthentication;
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

    private void testConnection() {
        LsRemoteCommand lsRemoteCommand = git.lsRemote();
        lsRemoteCommand.setTransportConfigCallback(transport -> {
            if (transport instanceof SshTransport) {
                gitProtocol = GitProtocol.SSH;
            } else if (transport instanceof HttpTransport) {
                gitProtocol = GitProtocol.HTTPS;
            } else {
                gitProtocol = GitProtocol.UNKNOWN;
            }
        });
        try {
            lsRemoteCommand.call();
            requiresAuthentication = false;
        } catch (GitAPIException e) {
            LOGGER.warn("Error while testing connection to origin for git repository", e);
            requiresAuthentication = gitProtocol == GitProtocol.SSH || gitProtocol == GitProtocol.HTTPS;
        }
    }
}
