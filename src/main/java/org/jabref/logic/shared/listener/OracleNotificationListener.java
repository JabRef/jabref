package org.jabref.logic.shared.listener;

import org.jabref.logic.shared.DBMSSynchronizer;

import oracle.jdbc.dcn.DatabaseChangeEvent;
import oracle.jdbc.dcn.DatabaseChangeListener;

/**
 * A listener for Oracle database notifications.
 */
public class OracleNotificationListener implements DatabaseChangeListener {

    private final DBMSSynchronizer dbmsSynchronizer;

    public OracleNotificationListener(DBMSSynchronizer dbmsSynchronizer) {
        this.dbmsSynchronizer = dbmsSynchronizer;
    }

    @Override
    public void onDatabaseChangeNotification(DatabaseChangeEvent event) {
        dbmsSynchronizer.pullChanges();
    }
}
