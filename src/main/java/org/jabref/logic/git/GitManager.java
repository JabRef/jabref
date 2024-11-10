package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(GitManager.class);

    private Path path;
    private Git git;
    private GitActionExecutor gitActionExecutor;
    private GitStatus gitStatus;

    public GitManager(Git git) {
        this.path = git.getRepository().getDirectory().getParentFile().toPath();
        this.git = git;
        this.gitActionExecutor = new GitActionExecutor(this.git);
        this.gitStatus = new GitStatus(this.git);
    }

    /**
     * TODO
     *  commits given bibFile -> pull -> push
     *  it must make sure that the state of the repository is not affected
     *  and no side effects like pushing other changes are performed
     */
    public void synchronize(Path filePath) throws GitException {
        throw new NotImplementedException();
    }

    /**
     * TODO
     *  pulls changes handling possible problems
     */
    public void update() throws GitException {
        throw new NotImplementedException();
    }

    /**
     *
     * @return Returns true if the given repository path to the GitManager object to a directory that is a git repository (contains a .git folder)
     */
    public static boolean isGitRepository(Path path) {
        return path.resolve(".git").toFile().exists();
    }

    public static GitManager openGitRepository(Path path) throws GitException {
        if (!isGitRepository(path)) {
            throw new GitException(path.getFileName() + " is not a git repository.");
        }
        try {
            return new GitManager(Git.open(path.toFile()));
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

    GitActionExecutor getGitActionExecutor() {
        return this.gitActionExecutor;
    }

    Path getPath() {
        return path;
    }
}
