package org.jabref.model;

import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.NullMarked;

/// Interpreted version of JabRefClipBoardData
/// Also used in a non-clipboard case at internal transfers at JabRef
@NullMarked
public record TransferInformation(
        BibDatabaseContext bibDatabaseContext,
        TransferMode transferMode
) {
}
