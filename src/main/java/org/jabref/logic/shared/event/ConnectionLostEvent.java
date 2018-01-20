package org.jabref.logic.shared.event;

import org.jabref.model.database.BibDatabaseContext;

/**
 * A new {@link ConnectionLostEvent} is fired, when the connection to the shared database gets lost.
 */
public class ConnectionLostEvent {

    private final BibDatabaseContext bibDatabaseContext;

    /**
     * @param bibDatabaseContext Affected {@link BibDatabaseContext}
     */
    public ConnectionLostEvent(BibDatabaseContext bibDatabaseContext) {
        this.bibDatabaseContext = bibDatabaseContext;
    }

    public BibDatabaseContext getBibDatabaseContext() {
        return this.bibDatabaseContext;
    }
}
