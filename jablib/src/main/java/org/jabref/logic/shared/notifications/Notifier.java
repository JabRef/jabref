package org.jabref.logic.shared.notifications;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Notifies the other clients connected to the same database about changes.
public class Notifier {

    /// Used as string argument to `pg_notify`, which - unlike the `LISTEN` command - does not
    /// case-fold its channel argument. Both sides therefore use this constant verbatim
    /// (the listener quotes it in `LISTEN`).
    public static final String CHANNEL = "jabrefLiveUpdate";

    /// Channel on which the database-side `upsert_metadata` function announces changed metadata values
    public static final String METADATA_CHANNEL = "metadata_update";

    private static final Logger LOGGER = LoggerFactory.getLogger(Notifier.class);

    // Keep safely below PostgreSQL's 8000 byte NOTIFY payload limit
    private static final int MAX_PAYLOAD_BYTES = 7000;

    private final Connection connection;
    private final String processorId;
    private final Gson gson = new GsonBuilder().create();

    public Notifier(Connection connection, String processorId) {
        this.connection = connection;
        this.processorId = processorId;
    }

    public void notifyAboutChangedField(FieldChangedEvent event) {
        send(createPayload(event));
    }

    /// Asks all other clients to pull. Used when changes reached the database without a single
    /// field-change event describing them - e.g. flushed micro-edits, which are written as a
    /// whole entry.
    public void notifyClientsToPull() {
        send(withoutContent());
    }

    private void send(FieldChange payload) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_notify('" + CHANNEL + "', ?)")) {
            statement.setString(1, gson.toJson(payload));
            statement.execute();
        } catch (SQLException e) {
            LOGGER.error("Could not notify clients", e);
        }
    }

    private FieldChange createPayload(FieldChangedEvent event) {
        BibEntry bibEntry = event.getBibEntry();
        // Content is only sent if the entry's current state still matches the event: a change that
        // did not reach the database (e.g. a refused update overwritten by the following pull, or a
        // type change, which is not readable via getField) must not be propagated as content.
        if (!bibEntry.getField(event.getField()).equals(Optional.ofNullable(event.getNewValue()))) {
            return withoutContent();
        }
        FieldChange payload = new FieldChange(
                processorId,
                bibEntry.getSharedBibEntryData().getSharedIdAsString(),
                event.getField().getName(),
                event.getOldValue(),
                event.getNewValue(),
                bibEntry.getSharedBibEntryData().getVersion());
        if (gson.toJson(payload).getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
            // TODO: use a table for exchanging oversized values
            return withoutContent();
        }
        return payload;
    }

    /// A payload without content makes receivers pull the changes from the database instead
    private FieldChange withoutContent() {
        return new FieldChange(processorId, null, null, null, null, 0);
    }
}
