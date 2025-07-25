package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitSyncService;
import org.jabref.logic.git.model.MergeResult;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.errors.GitAPIException;

public class GitPullViewModel extends AbstractViewModel {
    private final GitSyncService syncService;
    private final GitStatusViewModel gitStatusViewModel;

    public GitPullViewModel(GitSyncService syncService, GitStatusViewModel gitStatusViewModel) {
        this.syncService = syncService;
        this.gitStatusViewModel = gitStatusViewModel;
    }

    public MergeResult pull() throws IOException, GitAPIException, JabRefException {
        Optional<BibDatabaseContext> maybeContext = gitStatusViewModel.getDatabaseContext();
        if (maybeContext.isEmpty()) {
            throw new JabRefException("Cannot pull: No active BibDatabaseContext.");
        }

        BibDatabaseContext localBibDatabaseContext = maybeContext.get();
        Path bibFilePath = localBibDatabaseContext.getDatabasePath().orElseThrow(() ->
                new JabRefException("Cannot pull: .bib file path missing in BibDatabaseContext.")
        );

        MergeResult result = syncService.fetchAndMerge(localBibDatabaseContext, bibFilePath);

        if (result.isSuccessful()) {
            gitStatusViewModel.updateStatusFromContext(localBibDatabaseContext);
        }

        return result;
    }
}
