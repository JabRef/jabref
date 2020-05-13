package org.jabref.logic.shared.event;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

/**
 * A new {@link UpdateRefusedEvent} is fired, when the user tries to push changes of an obsolete {@link BibEntry} to the server.
 */
public class UpdateRefusedEvent {

    private final BibDatabaseContext bibDatabaseContext;
    private final BibEntry localBibEntry;
    private final BibEntry sharedBibEntry;

    /**
     * @param bibDatabaseContext Affected {@link BibDatabaseContext}
     * @param localBibEntry      Affected {@link BibEntry}
     */
    public UpdateRefusedEvent(BibDatabaseContext bibDatabaseContext, BibEntry localBibEntry, BibEntry sharedBibEntry) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.localBibEntry = localBibEntry;
        this.sharedBibEntry = sharedBibEntry;
    }

    public BibDatabaseContext getBibDatabaseContext() {
        return this.bibDatabaseContext;
    }

    public BibEntry getLocalBibEntry() {
        return localBibEntry;
    }

    public BibEntry getSharedBibEntry() {
        return sharedBibEntry;
    }
}
