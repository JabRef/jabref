package org.jabref.logic.openoffice.engine;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.openoffice.style.CitationGroup;

import org.jspecify.annotations.NullMarked;

/// All groups are supplied for set-wide numbering and disambiguation. Containers are copied, but
/// the existing mutable groups and databases are borrowed read-only, not deep immutable snapshots.
@NullMarked
public record RenderRequest(List<CitationGroup> groupsInDocumentOrder,
                            List<BibDatabase> databases,
                            RenderOptions options) {
    public RenderRequest {
        groupsInDocumentOrder = List.copyOf(groupsInDocumentOrder);
        databases = List.copyOf(databases);
    }
}
