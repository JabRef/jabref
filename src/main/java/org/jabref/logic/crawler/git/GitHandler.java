package org.jabref.logic.crawler.git;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the updating of the local and remote git repository that is located at the repository path
 */
public class GitHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHandler.class);
    private final Path repositoryPath;
    private final CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(System.getenv("GIT_EMAIL"), System.getenv("GIT_PW"));

    /**
     * Initialize the handler for the given repository
     *
     * @param repositoryPath The root of the intialized git repository
     */
    public GitHandler(Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    /**
     * Updates the local repository based on the main branch of the original remote repository
     */
    public void updateLocalRepository() throws IOException, GitAPIException {
        try (Git git = Git.open(this.repositoryPath.toFile())) {
            git.pull()
               .setRemote("origin")
               .setRemoteBranchName("main")
               .setCredentialsProvider(credentialsProvider)
               .call();
        }
    }

    /**
     * Adds all the added, changed, and removed files to the index and updates the remote origin repository
     * If pushiong to remote fails it fails silently
     *
     * @param commitMessage The commit message used for the commit to the remote repository
     */
    public void updateRemoteRepository(String commitMessage) throws IOException, GitAPIException {
        // First get up to date
        this.updateLocalRepository();
        try (Git git = Git.open(this.repositoryPath.toFile())) {
            Status status = git.status().call();
            if (!status.isClean()) {
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
                   .setAllowEmpty(false)
                   .setMessage(commitMessage)
                   .call();
                try {

                    git.push()
                       .setCredentialsProvider(credentialsProvider)
                       .call();
                } catch (GitAPIException e) {
                    LOGGER.info("Failed to push");
                }
            }
        }
    }
}
