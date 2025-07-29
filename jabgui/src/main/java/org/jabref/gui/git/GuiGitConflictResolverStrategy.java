package org.jabref.gui.git;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.git.conflicts.GitConflictResolverStrategy;
import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuiGitConflictResolverStrategy implements GitConflictResolverStrategy {
    private final static Logger LOGGER = LoggerFactory.getLogger(GuiGitConflictResolverStrategy.class);
    private final GitConflictResolverDialog dialog;

    public GuiGitConflictResolverStrategy(GitConflictResolverDialog dialog) {
        this.dialog = dialog;
    }

    @Override
    public List<BibEntry> resolveConflicts(List<ThreeWayEntryConflict> conflicts) {
        List<BibEntry> resolved = new ArrayList<>();
        for (ThreeWayEntryConflict conflict : conflicts) {
            // TODO: We discussed somewhere else that Optional<List<BibEntry>> should be List<BibEntry> - and that this list is empty if it was cancelled.
            Optional<BibEntry> maybeEntry = dialog.resolveConflict(conflict);
            if (maybeEntry.isEmpty()) {
                LOGGER.debug("User cancelled conflict resolution for entry {}", conflict.local().getCitationKey().orElse("<unknown>"));
                return List.of();
            }
            resolved.add(maybeEntry.get());
        }
        return resolved;
    }
}
