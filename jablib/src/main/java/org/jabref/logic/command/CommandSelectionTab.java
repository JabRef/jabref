package org.jabref.logic.command;

import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public interface CommandSelectionTab {

    void clearAndSelect(final List<BibEntry> bibEntries);

    BibDatabaseContext getBibDatabaseContext();
}
