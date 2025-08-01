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
            Optional<BibEntry> entryOpt = dialog.resolveConflict(conflict);
            if (entryOpt.isEmpty()) {
                LOGGER.debug("User cancelled conflict resolution for entry {}", conflict.local().getCitationKey().orElse("<unknown>"));
                return List.of();
            }
            resolved.add(entryOpt.get());
        }
        return resolved;
    }
}
