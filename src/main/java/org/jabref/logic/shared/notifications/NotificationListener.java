package org.jabref.logic.shared.notifications;

import java.sql.SQLException;

import org.jabref.logic.shared.DBMSProcessor;
import org.jabref.logic.shared.DBMSSynchronizer;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A listener for PostgreSQL database notifications.
 */
public class NotificationListener implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationListener.class);

    private final DBMSSynchronizer dbmsSynchronizer;
    private final PGConnection pgConnection;
    private volatile boolean stop;

    public NotificationListener(DBMSSynchronizer dbmsSynchronizer, PGConnection pgConnection) {
        this.dbmsSynchronizer = dbmsSynchronizer;
        this.pgConnection = pgConnection;
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
                        if (!DBMSProcessor.PROCESSOR_ID.equals(notification.getName())) {
                            // Only process notifications that are not sent by this processor
                            notification.getParameter();
                            dbmsSynchronizer.pullChanges();
                        }
                    }
                }
            }
        } catch (SQLException exception) {
            LOGGER.error("Error while listening for updates to PostgresSQL", exception);
        }
    }

    public void stop() {
        stop = true;
    }
}
