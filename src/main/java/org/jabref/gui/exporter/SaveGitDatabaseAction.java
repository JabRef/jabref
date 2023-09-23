package org.jabref.gui.exporter;

import java.nio.file.Path;

import org.jabref.logic.git.MyGitHandler;

public class SaveGitDatabaseAction {
    final Path repositoryPath;
    final String automaticCommitMsg = "Automatic update via JabRef";

    public SaveGitDatabaseAction(Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public boolean automaticUpdate() {
        MyGitHandler git = new MyGitHandler(repositoryPath);
        git.createCommitOnCurrentBranch(automaticCommitMsg, false);
        
        return true;
    }
}
