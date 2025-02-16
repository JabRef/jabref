package org.jabref.logic.shared.notifications;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jabref.model.entry.event.FieldChangedEvent;
import org.postgresql.PGConnection;

/**
 * TODO: for sizes > 8000 bytes, use a table for exchange
 */
public class Notifier {

    private final PGConnection pgConnection;
    private final Gson gson = new GsonBuilder().create();

    public Notifier(PGConnection pgConnection) {
        this.pgConnection = pgConnection;
    }

    public void notifyAboutChangedField(FieldChangedEvent event) {
        // FIXME: BibEntry bibEntry = event.getBibEntry();

        // While synchronizing the local database (see synchronizeLocalDatabase() below), some EntriesEvents may be posted.
        // In this case DBSynchronizer should not try to update the bibEntry entry again (but it would not harm).
        // connection.createStatement().execute("NOTIFY jabrefLiveUpdate, '" + PROCESSOR_ID + "';");
    }
}
