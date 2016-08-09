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
package net.sf.jabref.shared;

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
    public static Connection getNewConnection(DBMSConnectionProperties properties)
            throws ClassNotFoundException, SQLException {

        try {
            DriverManager.setLoginTimeout(3);
            return DriverManager.getConnection(
                    properties.getType().getUrl(properties.getHost(), properties.getPort(), properties.getDatabase()),
                    properties.getUser(), properties.getPassword());
        } catch (SQLException e) {
            // Some systems like PostgreSQL retrieves 0 to every exception.
            // Therefore a stable error determination is not possible.
            LOGGER.error("Could not connect to database: " + e.getMessage() + " - Error code: " + e.getErrorCode());

            throw e;
        }
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
