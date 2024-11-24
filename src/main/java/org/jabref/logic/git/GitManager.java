package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.HttpTransport;
import org.eclipse.jgit.transport.SshTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(GitManager.class);

    private Path path;
    private Git git;
    private final GitPreferences preferences;
    private final GitAuthenticator gitAuthenticator;
    private GitActionExecutor gitActionExecutor;
    private GitStatus gitStatus;
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

    /**
     * TODO
     *  commits given bibFile -> pull -> push
     *  it must make sure that the state of the repository is not affected
     *  and no side effects like pushing other changes are performed
     */
    public void synchronize(Path filePath) throws GitException {
        throw new GitException("NotImplemented");
    }

    /**
     * TODO
     *  pulls changes handling possible problems
     */
    public void update() throws GitException {
        throw new GitException("NotImplemented");
    }

    /**
     *
     * @return Returns true if the given repository path to the GitManager object to a directory that is a git repository (contains a .git folder)
     */
    public static boolean isGitRepository(Path path) {
        return path.resolve(".git").toFile().exists();
    }

    public static GitManager openGitRepository(Path path, GitPreferences gitPreferences) throws GitException {
        if (!isGitRepository(path)) {
            throw new GitException(path.getFileName() + " is not a git repository.");
        }
        try {
            return new GitManager(Git.open(path.toFile()), gitPreferences);
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

    public boolean requiresAuthentication() {
        return requiresAuthentication;
    }

    GitActionExecutor getGitActionExecutor() {
        return this.gitActionExecutor;
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
