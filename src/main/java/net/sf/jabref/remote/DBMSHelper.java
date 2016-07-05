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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Contains some helping methods related to the external SQL database.
 */
public class DBMSHelper {

    private static final Log LOGGER = LogFactory.getLog(DBMSConnector.class);

    private final Connection connection;


    public DBMSHelper(Connection connection) {
        this.connection = connection;
    }

    /**
     * Retrieves all present columns of the given relational table.
     * @param table Name of the table
     * @return Set of column names.
     */
    public Set<String> getColumnNames(String table) {
        Set<String> columnNames = new HashSet<>();
        try (ResultSet resultSet = query("SELECT * FROM " + table)) {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int count = resultSetMetaData.getColumnCount();

            for (int i = 0; i < count; i++) {
                columnNames.add(resultSetMetaData.getColumnName(i + 1));
            }


        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
        return columnNames;
    }

    /**
     * Executes the given query and retrieves the {@link ResultSet}
     * @param query SQL Query
     * @return Instance of {@link ResultSet}
     */
    public ResultSet query(String query) throws SQLException {
        return connection.createStatement().executeQuery(query);
    }

    /**
     * Executes the given query and retrieves the {@link ResultSet}
     * @param query SQL Query
     * @param resultSetType
     * @param resultSetConcurrency
     * @return Instance of {@link ResultSet}
     */
    public ResultSet query(String query, int resultSetType, int resultSetConcurrency) throws SQLException {
        return connection.createStatement(resultSetType, resultSetConcurrency).executeQuery(query);
    }

    /**
     * Executes the given query as SQL update
     * @param query SQL Query
     */
    public void executeUpdate(String query) {
        try {
            connection.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    /**
     * @return {@link DatabaseMetaData} of the current {@link Connection}
     */
    public DatabaseMetaData getMetaData() throws SQLException {
        return connection.getMetaData();
    }

    /**
     * @param query SQL query
     * @param columnNames Column names which should be returned
     * @return Instance of {@link PreparedStatement}
     */
    public PreparedStatement prepareStatement(String query, String... columnNames) throws SQLException {
        return connection.prepareStatement(query, columnNames);
    }

    /**
     *  Converts even String value to uppercase representation.
     *  Useful to harmonize character case for different database systems (see {@link DBMSType}).
     */
    public Set<String> allToUpperCase(Set<String> stringSet) {
        return stringSet.stream().map(n -> n.toUpperCase(Locale.ENGLISH)).collect(Collectors.toSet());
    }

    /**
     * Deletes all data from the given tables.
     */
    public void clearTables(String... tables) {
        try {
            for (String table : tables) {
                connection.createStatement().executeUpdate("TRUNCATE TABLE " + table);
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

}
