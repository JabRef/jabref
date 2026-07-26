package org.jabref.model;

import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Interpreted version of JabRefClipBoardData
/// Also used in a non-clipboard case at internal transfers at JabRef
@NullMarked
public record TransferInformation(
        BibDatabaseContext bibDatabaseContext,
        TransferMode transferMode,
        List<BibEntry> sourceEntries
) {
    public TransferInformation(BibDatabaseContext bibDatabaseContext, TransferMode transferMode) {
        this(bibDatabaseContext, transferMode, List.of());
    }
}
