package org.jabref.gui.git;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.collab.entrychange.EntryChangeResolver;
import org.jabref.model.git.GitContext;
import org.jabref.preferences.PreferencesService;

public class GitChangeResolverFactory {
    private final DialogService dialogService;
    private final GitContext gitContext;
    private final PreferencesService preferencesService;

    public GitChangeResolverFactory(DialogService dialogService, GitContext gitContext, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.gitContext = gitContext;
        this.preferencesService = preferencesService;
    }

    public Optional<GitChangeResolver> create(GitChange change) {
        if (change instanceof EntryChange entryChange) {
            return Optional.of(new EntryChangeResolver(entryChange, dialogService, gitContext, preferencesService));
        }

        return Optional.empty();
    }
}
