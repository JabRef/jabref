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

    public void notifyAboutChangedField(FieldChangedEvent event) {}
}
