package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitManager {
    private final Logger LOGGER = LoggerFactory.getLogger(GitManager.class);

    private Path pathToRepository;
    private File repositoryPathAsFile;

    private Git git;

    private GitActionExecutor gitActionExecutor;

    private GitStatus gitStatus;

    public GitManager(Path pathToRepository) {
        this.pathToRepository = pathToRepository;
        this.repositoryPathAsFile = pathToRepository.toFile();
        // we need to call the .initGitRepository to get our git object and
        // create our GitActionExecutor
        initGitRepository();
        // here we have invariant: private Git git != null, so we can create the GitActionExecutor object
        this.gitActionExecutor = new GitActionExecutor(this.git);
        this.gitStatus = new GitStatus(this.git);
    }

    /**
     *
     * @return Returns true if the given repository path to the GitManager object to a directory that is a git repository (contains a .git folder)
     */
    public boolean isGitRepository() {
        return Files.exists(Path.of(pathToRepository.toString(), ".git"));
        // .of() returns the concatenated path, .exists() then checks if at this path
        // the .git file is there
    }

    /**
     *
     * @param filePath
     * @return Returns true if the given path points to a directory that is a git repository (contains a .git folder)
     */
    public boolean isGitRepository(Path filePath) {
        return Files.exists(Path.of(filePath.toString(), ".git"));
    }

    // Note: difference to UML, function does not return a GitManager
    // its also private being called by the constructor implicitly
    /**
     * Initiates git repository at path specified in the constructor
     */
    private void initGitRepository() {
        try {
            if (!isGitRepository()) {
                Git git = Git.init()
                             .setDirectory(repositoryPathAsFile)
                             .setInitialBranch("main")
                             .call();
                LOGGER.info("Git repository initialized successfully.");
                this.git = git;
            } else {
                this.git = Git.open(repositoryPathAsFile);
            }
        } catch (GitAPIException | IOException e) {
            LOGGER.error("Initialization failed");
        }
    }

    public GitActionExecutor getGitActionExecutor() {
        return this.gitActionExecutor;
    }
}
