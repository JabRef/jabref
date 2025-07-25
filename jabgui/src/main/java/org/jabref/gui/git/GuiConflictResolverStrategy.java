package org.jabref.gui.git;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.git.conflicts.GitConflictResolverStrategy;
import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.model.entry.BibEntry;

public class GuiConflictResolverStrategy implements GitConflictResolverStrategy {
    private final GitConflictResolverDialog dialog;

    public GuiConflictResolverStrategy(GitConflictResolverDialog dialog) {
        this.dialog = dialog;
    }

    @Override
    public Optional<List<BibEntry>> resolveConflicts(List<ThreeWayEntryConflict> conflicts) {
        List<BibEntry> resolved = new ArrayList<>();
        for (ThreeWayEntryConflict conflict : conflicts) {
            Optional<BibEntry> maybeConflict = dialog.resolveConflict(conflict);
            if (maybeConflict.isEmpty()) {
                return Optional.empty();
            }
            resolved.add(maybeConflict.get());
        }
        return Optional.of(resolved);
    }
}
