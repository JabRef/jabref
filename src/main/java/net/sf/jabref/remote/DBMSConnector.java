/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.remote;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import net.sf.jabref.logic.l10n.Localization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used to establish connections between JabRef and database systems like MySQL, PostgreSQL and Oracle.
 */
public class DBMSConnector {

    private static final Log LOGGER = LogFactory.getLog(DBMSConnector.class);


    /**
     * Determines the suitable driver and retrieves a working SQL Connection in normal case.
     * A default port is going to be taken.
     *
     * @param dbmsType Enum entry of {@link DBMSType} which determines the driver
     * @param host Hostname, Domain or IP address
     * @param database An already existent database name.
     * @param user Username
     * @param password Password
     * @return
     * @throws ClassNotFoundException Thrown if no suitable drivers were found
     * @throws SQLException Thrown if connection has failed
     */
    public static Connection getNewConnection(DBMSType dbmsType, String host, String database, String user, String password)
            throws ClassNotFoundException, SQLException {
        return getNewConnection(dbmsType, host, getDefaultPort(dbmsType), database, user, password);
    }

    /**
     * Determines the suitable driver and retrieves a working SQL Connection in normal case.
     *
     * @param dbmsType Enum entry of {@link DBMSType} which determines the driver
     * @param host Hostname, Domain or IP address
     * @param port Port number the server is listening on
     * @param database An already existent database name.
     * @param user Username
     * @param password Password
     * @return
     * @throws ClassNotFoundException Thrown if no suitable drivers were found
     * @throws SQLException Thrown if connection has failed
     */
    public static Connection getNewConnection(DBMSType dbmsType, String host, int port, String database, String user,
            String password) throws ClassNotFoundException, SQLException {

        String url = "jdbc:";

        try {
            switch (dbmsType) {
            case MYSQL:
                url = url + "mysql://" + host + ":" + port + "/" + database;
                break;
            case ORACLE:
                url = url + "oracle:thin:@" + host + ":" + port + ":" + database;
                break;
            case POSTGRESQL:
                url = url + "postgresql://" + host + ":" + port + "/" + database;
                break;
            }
            DriverManager.setLoginTimeout(3);
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            // Some systems like PostgreSQL retrieves 0 to every exception.
            // Therefore a stable error determination is not possible.
            LOGGER.error("Could not connect to database: " +
                    e.getMessage() + " - Error code: " + e.getErrorCode());

            throw e;
        }
    }

    /**
     * Retrieves the port number dependent on the type of the database system.
     */
    public static int getDefaultPort(DBMSType dbmsType) {
        if (dbmsType == DBMSType.MYSQL) {
            return 3306;
        }
        if (dbmsType == DBMSType.POSTGRESQL) {
            return 5432;
        }
        if (dbmsType == DBMSType.ORACLE) {
            return 1521;
        }
        return -1;
    }

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
