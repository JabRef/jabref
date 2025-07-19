package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitSyncService;
import org.jabref.logic.git.model.MergeResult;

import org.eclipse.jgit.api.errors.GitAPIException;

public class GitPullViewModel extends AbstractViewModel {
    private final GitSyncService syncService;
    private final GitStatusViewModel gitStatusViewModel;
    private final Path bibFilePath;

    public GitPullViewModel(GitSyncService syncService, GitStatusViewModel gitStatusViewModel) {
        this.syncService = syncService;
        this.gitStatusViewModel = gitStatusViewModel;
        this.bibFilePath = gitStatusViewModel.getCurrentBibFile();
    }

    public MergeResult pull() throws IOException, GitAPIException, JabRefException {
        MergeResult result = syncService.fetchAndMerge(bibFilePath);

        if (result.isSuccessful()) {
            gitStatusViewModel.updateStatusFromPath(bibFilePath);
        }

        return result;
    }
}
