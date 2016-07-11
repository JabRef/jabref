package net.sf.jabref.event;

import net.sf.jabref.BibDatabaseContext;

/**
 * A new {@link RemoteConnectionLostEvent} is fired, when the connection to the remote database gets lost.
 */
public class RemoteConnectionLostEvent {

    private final BibDatabaseContext bibDatabaseContext;

    /**
     * @param bibDatabaseContext Affected {@link BibDatabaseContext}
     */
    public RemoteConnectionLostEvent(BibDatabaseContext bibDatabaseContext) {
        this.bibDatabaseContext = bibDatabaseContext;
    }

    public BibDatabaseContext getBibDatabaseContext() {
        return this.bibDatabaseContext;
    }
}
