package org.jabref.logic.shared.notifications;

import java.sql.SQLException;

import org.jabref.logic.shared.DBMSSynchronizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A listener for PostgreSQL database notifications.
public class NotificationListener implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationListener.class);

    private final DBMSSynchronizer dbmsSynchronizer;
    private final PGConnection pgConnection;
    private final String processorId;
    private final Gson gson = new GsonBuilder().create();
    private volatile boolean stop;

    public NotificationListener(DBMSSynchronizer dbmsSynchronizer, PGConnection pgConnection, String processorId) {
        this.dbmsSynchronizer = dbmsSynchronizer;
        this.pgConnection = pgConnection;
        this.processorId = processorId;
    }

    @Override
    public void run() {
        stop = false;
        try {
            while (!stop && !Thread.currentThread().isInterrupted()) {
                // Wait for 12 seconds for notifications. Result will be null if no notifications arrive
                PGNotification[] notifications = pgConnection.getNotifications(12_000);

                if (notifications != null) {
                    for (PGNotification notification : notifications) {
                        try {
                            if (Notifier.METADATA_CHANNEL.equals(notification.getName())) {
                                // The payload carries the changed key/value, but metadata is small:
                                // re-reading all of it is simpler and always consistent
                                dbmsSynchronizer.synchronizeLocalMetaData();
                            } else {
                                handleNotification(notification.getParameter());
                            }
                        } catch (RuntimeException e) {
                            // The listener thread has to survive a single failing notification
                            LOGGER.error("Error while handling notification", e);
                        }
                    }
                }
            }
        } catch (SQLException exception) {
            if (!stop) {
                // Stopping closes the listener connection, which aborts a pending poll - not an error
                LOGGER.error("Error while listening for updates to PostgresSQL", exception);
            }
        }
    }

    private void handleNotification(String payload) {
        FieldChange fieldChange;
        try {
            fieldChange = gson.fromJson(payload, FieldChange.class);
        } catch (JsonSyntaxException e) {
            LOGGER.warn("Could not parse notification payload, pulling changes instead: {}", payload);
            fieldChange = null;
        }
        if (fieldChange == null) {
            dbmsSynchronizer.pullChanges();
            return;
        }
        if (processorId.equals(fieldChange.sourceProcessorId())) {
            // Own notification
            return;
        }
        dbmsSynchronizer.applyRemoteFieldChange(fieldChange);
    }

    public void stop() {
        stop = true;
    }
}
