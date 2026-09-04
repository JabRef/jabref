package org.jabref.logic.shared.notifications;

import java.sql.Connection;
import java.sql.SQLException;

import org.jabref.logic.shared.DBMSSynchronizer;
import org.jabref.logic.shared.DatabaseConnection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A listener for PostgreSQL database notifications.
///
/// Opens - and on failures reopens - its own database connection, because polling for
/// notifications blocks the connection it runs on. Reconnecting is retried for as long as the
/// library is open: every successful reconnect is followed by a full pull, so an outage costs
/// nothing but delay.
@NullMarked
public class NotificationListener implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationListener.class);

    private static final long MAX_RECONNECT_DELAY_MILLIS = 30_000L;

    private final DBMSSynchronizer dbmsSynchronizer;
    private final DatabaseConnection dbmsConnection;
    private final String processorId;
    private final Gson gson = new GsonBuilder().create();

    private volatile boolean stop;
    private volatile @Nullable Connection connection;
    private volatile @Nullable PGConnection pgConnection;

    public NotificationListener(DBMSSynchronizer dbmsSynchronizer, DatabaseConnection dbmsConnection, String processorId) {
        this.dbmsSynchronizer = dbmsSynchronizer;
        this.dbmsConnection = dbmsConnection;
        this.processorId = processorId;
    }

    /// Registers the notification subscription. Called before the polling starts, so that no
    /// notification sent right after this method returns can be missed.
    public void start() throws SQLException {
        pgConnection = connect();
    }

    @Override
    public void run() {
        int consecutiveFailures = 0;
        while (!stop && !Thread.currentThread().isInterrupted()) {
            try {
                PGConnection currentPgConnection = pgConnection;
                if (currentPgConnection == null) {
                    currentPgConnection = connect();
                    pgConnection = currentPgConnection;
                    // Notifications sent while the listener was down are gone - pull the
                    // full state once so no remote change is lost over the downtime
                    dbmsSynchronizer.handleRemoteDatabaseChange();
                    dbmsSynchronizer.handleRemoteMetaDataChange();
                }
                while (!stop && !Thread.currentThread().isInterrupted()) {
                    // Wait for 12 seconds for notifications. Result will be null if no notifications arrive
                    PGNotification @Nullable [] notifications = currentPgConnection.getNotifications(12_000);
                    consecutiveFailures = 0;
                    if (notifications != null) {
                        for (PGNotification notification : notifications) {
                            try {
                                handleNotification(notification);
                            } catch (RuntimeException e) {
                                // The listener thread has to survive a single failing notification
                                LOGGER.error("Error while handling notification", e);
                            }
                        }
                    }
                }
            } catch (SQLException exception) {
                if (stop) {
                    // Stopping closes the listener connection, which aborts a pending poll - not an error
                    return;
                }
                closeConnection();
                consecutiveFailures++;
                // Exponential backoff: 1, 2, 4, ... seconds up to the cap
                long delayMillis = Math.min(MAX_RECONNECT_DELAY_MILLIS, 1000L << Math.min(consecutiveFailures - 1, 10));
                if (consecutiveFailures == 1) {
                    LOGGER.warn("Error while listening for shared database updates - reconnecting", exception);
                } else {
                    LOGGER.debug("Reconnecting the shared database listener in {} ms (attempt {})", delayMillis, consecutiveFailures, exception);
                }
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private PGConnection connect() throws SQLException {
        Connection newConnection = dbmsConnection.openNewConnection();
        this.connection = newConnection;
        if (stop) {
            // stop() raced with connecting - do not leak the fresh connection
            closeConnection();
            throw new SQLException("Listener already stopped");
        }
        newConnection.createStatement().execute("LISTEN \"" + Notifier.CHANNEL + "\"");
        newConnection.createStatement().execute("LISTEN \"" + Notifier.METADATA_CHANNEL + "\"");
        return newConnection.unwrap(PGConnection.class);
    }

    // [impl->req~shared-database.live-propagation~1]
    private void handleNotification(PGNotification notification) {
        if (Notifier.METADATA_CHANNEL.equals(notification.getName())) {
            // The payload names the changed key, but metadata is small:
            // re-reading all of it is simpler and always consistent
            dbmsSynchronizer.handleRemoteMetaDataChange();
            return;
        }
        String payload = notification.getParameter();
        @Nullable FieldChange fieldChange;
        try {
            fieldChange = gson.fromJson(payload, FieldChange.class);
        } catch (JsonSyntaxException e) {
            LOGGER.warn("Could not parse notification payload, pulling changes instead: {}", payload, e);
            fieldChange = null;
        }
        if (fieldChange == null) {
            dbmsSynchronizer.handleRemoteDatabaseChange();
            return;
        }
        if (processorId.equals(fieldChange.sourceProcessorId())) {
            // Own notification
            return;
        }
        dbmsSynchronizer.handleRemoteFieldChange(fieldChange);
    }

    public void stop() {
        stop = true;
        // Also aborts a pending poll
        closeConnection();
    }

    private void closeConnection() {
        Connection current = connection;
        connection = null;
        pgConnection = null;
        if (current != null) {
            try {
                current.close();
            } catch (SQLException e) {
                LOGGER.debug("Could not close listener connection", e);
            }
        }
    }
}
