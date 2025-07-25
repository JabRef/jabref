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
            BibEntry entry = dialog.resolveConflict(conflict)
                                   .orElseThrow(() -> new IllegalStateException("Conflict resolution was cancelled"));
            resolved.add(entry);
        }
        return Optional.of(resolved);
    }
}
