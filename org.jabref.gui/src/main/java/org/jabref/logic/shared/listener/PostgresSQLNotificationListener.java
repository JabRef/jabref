package org.jabref.logic.shared.listener;

import org.jabref.logic.shared.DBMSProcessor;
import org.jabref.logic.shared.DBMSSynchronizer;

import com.impossibl.postgres.api.jdbc.PGNotificationListener;

/**
 * A listener for PostgreSQL database notifications.
 */
public class PostgresSQLNotificationListener implements PGNotificationListener {

    private final DBMSSynchronizer dbmsSynchronizer;


    public PostgresSQLNotificationListener(DBMSSynchronizer dbmsSynchronizer) {
        this.dbmsSynchronizer = dbmsSynchronizer;
    }

    @Override
    public void notification(int processId, String channel, String payload) {
        if (!payload.equals(DBMSProcessor.PROCESSOR_ID)) {
            dbmsSynchronizer.pullChanges();
        }
    }

}
