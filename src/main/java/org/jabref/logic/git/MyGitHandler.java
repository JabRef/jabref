package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;

public class MyGitHandler {
    final Path repositoryPath;
    final File repositoryPathAsFile;
    public MyGitHandler(Path repositoryPath) {
        this.repositoryPath = repositoryPath;
        this.repositoryPathAsFile = this.repositoryPath.toFile();
        // TODO: init .git
    }

    boolean isLocalGitRepository() {
        Path gitFolderPath = Path.of(repositoryPath.toString(), ".git");
        return Files.exists(gitFolderPath) && Files.isDirectory(gitFolderPath);
    }

    public boolean createCommitOnCurrentBranch(String commitMessage, boolean amend) {
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
        } catch (
                IOException |
                GitAPIException e) {
            throw new RuntimeException(e);
        }
        return commitCreated;
    }
}
