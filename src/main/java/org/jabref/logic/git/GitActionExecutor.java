package org.jabref.logic.git;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitActionExecutor {

    private final Logger LOGGER = LoggerFactory.getLogger(GitActionExecutor.class);

    private Git git;

    public GitActionExecutor(Git git) {
        this.git = git;
    }

    //git.add() returns an 'AddCommand object'
    //the .addFilepattern() call also returns an AddCommand object
    //we need to call .call() on an AddCommand object to execute the command
    public void add(Path path) {
        try {
            // addFilePattern takes relative paths bruhhhh  ://////////
            // total time spent wasted on feeding it absolute path: 2 hrs

            File gitRepository = git.getRepository().getDirectory(); // this the file path including .git at the end
            File parentFile = gitRepository.getParentFile();
            Path pathOfParentFile = parentFile.toPath();

            Path relativePath = pathOfParentFile.relativize(path);

            git.add().addFilepattern(relativePath.toString()).call();
            LOGGER.info("File added to staging: " + path);
        } catch (GitAPIException e) {
            LOGGER.error("Failed to add file: " + e.getMessage());
        }
    }

    public void add(List<Path> paths) {

        File gitRepository = git.getRepository().getDirectory();
        File parentFile = gitRepository.getParentFile();
        Path pathOfParentFile = parentFile.toPath();

        try {
            for (Path path : paths) {
                Path relativePath = pathOfParentFile.relativize(path);
                git.add().addFilepattern(relativePath.toString()).call();
            }
            LOGGER.info("Files added to staging: " + paths);
        } catch (GitAPIException e) {
            LOGGER.error("Failed to add files: " + e.getMessage());
        }
    }


    public void commit(String message, boolean append) {
        try {
            git.commit().setMessage(message).setAmend(append).call();
            LOGGER.info("Commit successful with message: " + message);
        } catch (GitAPIException e) {
            LOGGER.error("Commit failed: " + e.getMessage());
        }
    }

    public void push(String remote, String branch) {
        try {
            git.push().setRemote(remote).add(branch).call();
            LOGGER.info("Pushed to remote: " + remote + ", branch: " + branch);
        } catch (GitAPIException e) {
            LOGGER.error("Push failed: " + e.getMessage());
        }
    }

    // Unsure how to test this..
    public void push() {
        try {
            git.push().call();
            LOGGER.info("Pushed to default remote and branch.");
        } catch (GitAPIException e) {
            LOGGER.error("Push failed: " + e.getMessage());
        }
    }

    public void pull(boolean withRebase, String remote, String branch) {
        try {
            git.pull()
               .setRebase(withRebase)
               .setRemote(remote)
               .setRemoteBranchName(branch)
               .call();
            System.out.println("Pulled from remote: " + remote + ", branch: " + branch);
        } catch (GitAPIException e) {
            System.err.println("Pull failed: " + e.getMessage());
        }
    }

    public void pull(String remote, String branch) {
        pull(false, remote, branch);
    }


    public Git getGit(){
        return git;
    }

    public void undoPull() {
        try {

            git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).call();
            LOGGER.info("Last pull undone (hard reset).");
        } catch (GitAPIException e) {
            LOGGER.error("Undo pull failed: " + e.getMessage());
        }
    }

    public void stash() {
        try {
            git.stashCreate().call();
            LOGGER.info("Current changes stashed.");
        } catch (GitAPIException e) {
            LOGGER.error("Stash failed: " + e.getMessage());
        }
    }

    public void unstash() {
        try {
            git.stashApply().call();
            LOGGER.info("Stash applied.");
        } catch (GitAPIException e) {
            LOGGER.error("Unstash failed: " + e.getMessage());
        }
    }
}
