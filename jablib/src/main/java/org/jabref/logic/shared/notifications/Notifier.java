package org.jabref.logic.shared.notifications;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jabref.logic.shared.DBMSProcessor;
import org.jabref.model.entry.event.FieldChangedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Notifies the other clients connected to the same database about changes.
///
/// TODO: Send the change itself (see {@link FieldChange}) so receivers can apply it without pulling.
///       For sizes > 8000 bytes, use a table for exchange.
public class Notifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(Notifier.class);

    private final Connection connection;

    public Notifier(Connection connection) {
        this.connection = connection;
    }

    public void notifyAboutChangedField(FieldChangedEvent event) {
        // The payload identifies the sender, so receivers can skip their own notifications
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_notify('jabrefLiveUpdate', ?)")) {
            statement.setString(1, DBMSProcessor.PROCESSOR_ID);
            statement.execute();
        } catch (SQLException e) {
            LOGGER.error("Could not notify clients about changed field", e);
        }
    }
}
