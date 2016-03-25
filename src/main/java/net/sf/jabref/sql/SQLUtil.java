/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General public static License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General public static License for more details.

    You should have received a copy of the GNU General public static License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.sql;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.bibtex.InternalBibtexFields;

/**
 * @author pattonlk
 *         <p>
 *         Reestructured by ifsteinm. Jan 20th Now it is possible to export more than one jabref database. BD creation,
 *         insertions and queries where reformulated to accomodate the changes. The changes include a refactory on
 *         import/export to SQL module, creating many other classes making them more readable This class just support
 *         Exporters and Importers
 */

final public class SQLUtil {

    private static final List<String> RESERVED_DB_WORDS = new ArrayList<>(Collections.singletonList("key"));

    private static List<String> allFields;

    private static final Log LOGGER = LogFactory.getLog(SQLUtil.class);


    private SQLUtil() {
    }

    /**
     * loop through entry types to get required, optional, general and utility fields for this type.
     */
    private static void refreshFields() {
        if (SQLUtil.allFields == null) {
            SQLUtil.allFields = new ArrayList<>();
        } else {
            SQLUtil.allFields.clear();
        }
        SQLUtil.uniqueListInsert(SQLUtil.allFields, InternalBibtexFields.getAllFieldNames());
        SQLUtil.uniqueListInsert(SQLUtil.allFields, InternalBibtexFields.getAllPrivateFieldNames());
    }

    /**
     * @return All existent fields for a bibtex entry
     */
    public static List<String> getAllFields() {
        if (SQLUtil.allFields == null) {
            SQLUtil.refreshFields();
        }
        return SQLUtil.allFields;
    }

    /**
     * @return Create a common separated field names
     */
    public static String getFieldStr() {
        // create comma separated list of field names
        List<String> fieldNames = new ArrayList<>();
        for (int i = 0; i < SQLUtil.getAllFields().size(); i++) {
            StringBuilder field = new StringBuilder(SQLUtil.allFields.get(i));
            if (SQLUtil.RESERVED_DB_WORDS.contains(field.toString())) {
                field.append('_');
            }
            fieldNames.add(field.toString());
        }
        return String.join(", ", fieldNames);
    }

    /**
     * Inserts the elements of a List into another List making sure not to duplicate entries in the resulting List
     *
     * @param list1 The List containing unique entries
     * @param list2 The second List to be inserted into the first List
     * @return The updated list1 with new unique entries
     */
    private static List<String> uniqueListInsert(List<String> list1, List<String> list2) {
        if (list2 != null) {
            for (String fromList2 : list2) {
                if (!list1.contains(fromList2) && (!"#".equals(fromList2))) {
                    list1.add(fromList2);
                }
            }
        }
        return list1;
    }

    /**
     * Generates DML specifying table columns and their datatypes. The output of this routine should be used within a
     * CREATE TABLE statement.
     *
     * @param fields   Contains unique field names
     * @param datatype Specifies the SQL data type that the fields should take on.
     * @return The SQL code to be included in a CREATE TABLE statement.
     */
    public static String fieldsAsCols(List<String> fields, String datatype) {
        List<String> newFields = new ArrayList<>();
        for (String field1 : fields) {
            StringBuilder field = new StringBuilder(field1);
            if (SQLUtil.RESERVED_DB_WORDS.contains(field.toString())) {
                field.append('_');
            }
            field.append(datatype);
            newFields.add(field.toString());
        }
        return String.join(", ", newFields);
    }

    /**
     * @param allFields All existent fields for a given entry type
     * @param reqFields list containing required fields for an entry type
     * @param optFields list containing optional fields for an entry type
     * @param utiFields list containing utility fields for an entry type
     * @param origList  original list with the correct size filled with the default values for each field
     * @return origList changing the values of the fields that appear on reqFields, optFields, utiFields set to 'req',
     * 'opt' and 'uti' respectively
     */
    public static List<String> setFieldRequirement(List<String> allFields, List<String> reqFields,
            List<String> optFields, List<String> utiFields, List<String> origList) {

        String currentField;
        for (int i = 0; i < allFields.size(); i++) {
            currentField = allFields.get(i);
            if (reqFields.contains(currentField)) {
                origList.set(i, "req");
            } else if (optFields.contains(currentField)) {
                origList.set(i, "opt");
            } else if (utiFields.contains(currentField)) {
                origList.set(i, "uti");
            }
        }
        return origList;
    }

    /**
     * Return a message raised from a SQLException
     *
     * @param ex The SQLException raised
     */
    public static String getExceptionMessage(Exception ex) {
        String msg;
        if (ex.getMessage() == null) {
            msg = ex.toString();
        } else {
            msg = ex.getMessage();
        }
        return msg;
    }

    /**
     * return a Statement with the result of a "SELECT *" query for a given table
     *
     * @param conn      Connection to the database
     * @param tableName String containing the name of the table you want to get the results.
     * @return a ResultSet with the query result returned from the DB
     * @throws SQLException
     */
    public static Statement queryAllFromTable(Connection conn, String tableName) throws SQLException {
        String query = "SELECT * FROM " + tableName + ';';
        return (Statement) SQLUtil.processQueryWithResults(conn, query);
    }

    /**
     * Utility method for processing DML with proper output
     *
     * @param out The output (PrintStream or Connection) object to which the DML should be sent
     * @param dml The DML statements to be processed
     */
    public static void processQuery(Object out, String dml) throws SQLException {
        if (out instanceof PrintStream) {
            PrintStream fout = (PrintStream) out;
            fout.println(dml);
        }
        if (out instanceof Connection) {
            Connection conn = (Connection) out;
            SQLUtil.executeQuery(conn, dml);
        }
    }

    /**
     * Utility method for processing DML with proper output
     *
     * @param out   The output (PrintStream or Connection) object to which the DML should be sent
     * @param query The DML statements to be processed
     * @return the result of the statement
     */
    public static AutoCloseable processQueryWithResults(Object out, String query) throws SQLException {
        if (out instanceof PrintStream) {// TODO: how to handle the PrintStream
            // case?
            PrintStream fout = (PrintStream) out;
            fout.println(query);
            return fout;
        }
        if (out instanceof Connection) {
            Connection conn = (Connection) out;
            return SQLUtil.executeQueryWithResults(conn, query);
        }
        return null;
    }

    /**
     * This routine returns the JDBC url corresponding to the DBStrings input.
     *
     * @param dbStrings The DBStrings to use to make the connection
     * @return The JDBC url corresponding to the input DBStrings
     */
    public static String createJDBCurl(DBStrings dbStrings, boolean withDBName) {
        String url;
        url = "jdbc:" + dbStrings.getServerType().toLowerCase() + "://" + dbStrings.getServerHostname()
                + (withDBName ? '/' + dbStrings.getDatabase() : "") + dbStrings.getDbParameters();
        return url;
    }

    /**
     * Process a query and returns only the first result of a result set as a String. To be used when it is certain that
     * only one String (single cell) will be returned from the DB
     *
     * @param conn  The Connection object to which the DML should be sent
     * @param query The query statements to be processed
     * @return String with the result returned from the database
     * @throws SQLException
     */
    public static String processQueryWithSingleResult(Connection conn, String query) throws SQLException {
        try (Statement sm = SQLUtil.executeQueryWithResults(conn, query); ResultSet rs = sm.getResultSet()) {
            rs.next();
            String result = rs.getString(1);
            rs.getStatement().close();
            return result;
        }
    }

    /**
     * Utility method for executing DML
     *
     * @param conn The DML Connection object that will execute the SQL
     * @param qry  The DML statements to be executed
     */
    private static void executeQuery(Connection conn, String qry) throws SQLException {
        try (Statement stmnt = conn.createStatement()) {
            stmnt.execute(qry);
            SQLWarning warn = stmnt.getWarnings();
            if (warn != null) {
                LOGGER.warn(warn);
            }
        }
    }

    /**
     * Utility method for executing DML
     *
     * @param conn The DML Connection object that will execute the SQL
     * @param qry  The DML statements to be executed
     */
    private static Statement executeQueryWithResults(Connection conn, String qry) throws SQLException {
        Statement stmnt = null;
        try {
            stmnt = conn.createStatement();
            stmnt.executeQuery(qry);
            SQLWarning warn = stmnt.getWarnings();
            if (warn != null) {
                LOGGER.warn(warn);
            }
            return stmnt;
        } catch (SQLException rethrow) {
            // in case of exception, try to close the statement to avoid a resource leak...
            if (stmnt != null) {
                stmnt.close();
            }
            //... and rethrow the exception
            throw rethrow;
        }
    }

}