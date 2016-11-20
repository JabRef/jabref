package net.sf.jabref.shared;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.shared.exception.InvalidDBMSConnectionPropertiesException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DBMSConnection {

    private static final Log LOGGER = LogFactory.getLog(DBMSConnection.class);

    private final Connection connection;
    private final DBMSConnectionProperties properties;


    public DBMSConnection(DBMSConnectionProperties properties) throws SQLException, InvalidDBMSConnectionPropertiesException {

        if (!properties.isValid()) {
            throw new InvalidDBMSConnectionPropertiesException();
        }
        this.properties = properties;

        try {
            DriverManager.setLoginTimeout(3);
            // ensure that all SQL drivers are loaded - source: http://stackoverflow.com/a/22384826/873282
            // we use the side effect of getAvailableDBMSTypes() - it loads all available drivers
            DBMSConnection.getAvailableDBMSTypes();

            this.connection = DriverManager.getConnection(
                    properties.getType().getUrl(properties.getHost(), properties.getPort(), properties.getDatabase()),
                    properties.getUser(), properties.getPassword());
        } catch (SQLException e) {
            // Some systems like PostgreSQL retrieves 0 to every exception.
            // Therefore a stable error determination is not possible.
            LOGGER.error("Could not connect to database: " + e.getMessage() + " - Error code: " + e.getErrorCode());

            throw e;
        }
    }

    public Connection getConnection() {
        return this.connection;
    }

    public DBMSConnectionProperties getProperties() {
        return this.properties;
    }

    /**
     * Returns a Set of {@link DBMSType} which is supported by available drivers.
     */
    public static Set<DBMSType> getAvailableDBMSTypes() {
        Set<DBMSType> dbmsTypes = new HashSet<>();

        for (DBMSType dbms : DBMSType.values()) {
            try {
                Class.forName(dbms.getDriverClassPath());
                dbmsTypes.add(dbms);
            } catch (ClassNotFoundException e) {
                // In case that the driver is not available do not perform tests for this system.
                LOGGER.info(Localization.lang("%0 driver not available.", dbms.toString()));
            }
        }
        return dbmsTypes;
    }
}
