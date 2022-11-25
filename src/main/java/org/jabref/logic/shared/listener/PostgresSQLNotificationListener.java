package org.jabref.logic.shared.listener;

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
public class PostgresSQLNotificationListener implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresSQLNotificationListener.class);

    private final DBMSSynchronizer dbmsSynchronizer;
    private final PGConnection pgConnection;
    private volatile boolean stop;

    public PostgresSQLNotificationListener(DBMSSynchronizer dbmsSynchronizer, PGConnection pgConnection) {
        this.dbmsSynchronizer = dbmsSynchronizer;
        this.pgConnection = pgConnection;
    }

    @Override
    public void run() {
        stop = false;
        try {
            // noinspection InfiniteLoopStatement
            while (!stop) {
                PGNotification notifications[] = pgConnection.getNotifications();

                if (notifications != null) {
                    for (PGNotification notification : notifications) {
                        if (!notification.getName().equals(DBMSProcessor.PROCESSOR_ID)) {
                            dbmsSynchronizer.pullChanges();
                        }
                    }
                }

                // Wait a while before checking again for new notifications
                Thread.sleep(500);
            }
        } catch (SQLException | InterruptedException exception) {
            LOGGER.error("Error while listening for updates to PostgresSQL", exception);
        }
    }

    public void stop() {
        stop = true;
    }
}
