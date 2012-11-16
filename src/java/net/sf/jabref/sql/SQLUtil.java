/*  Copyright (C) 2003-2011 JabRef contributors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import net.sf.jabref.BibtexFields;
import net.sf.jabref.Globals;

/**
 * 
 * @author pattonlk
 * 
 *         Reestructured by ifsteinm. Jan 20th Now it is possible to export more
 *         than one jabref database. BD creation, insertions and queries where
 *         reformulated to accomodate the changes. The changes include a
 *         refactory on import/export to SQL module, creating many other classes
 *         making them more readable This class just support Exporters and
 *         Importers
 */

public class SQLUtil {

	private static final ArrayList<String> reservedDBWords = new ArrayList<String>(
			Arrays.asList("key"));

	private static ArrayList<String> allFields = null;

	private SQLUtil() {
	}

	/**
	 * loop through entry types to get required, optional, general and utility
	 * fields for this type.
	 */
	public static void refreshFields() {
		if (allFields == null) {
			allFields = new ArrayList<String>();
		} else {
			allFields.clear();
		}
		uniqueInsert(allFields, BibtexFields.getAllFieldNames());
	}

	/**
	 * 
	 * @return All existent fields for a bibtex entry
	 */
	public static ArrayList<String> getAllFields() {
		if (allFields == null)
			refreshFields();
		return allFields;
	}

	/**
	 * 
	 * @return Create a common separated field names
	 */
	public static String getFieldStr() {
		// create comma separated list of field names
		String fieldstr = "";
		String field = "";
		for (int i = 0; i < getAllFields().size(); i++) {
			field = allFields.get(i);
			if (i > 0)
				fieldstr = fieldstr + ", ";
			if (reservedDBWords.contains(field))
				field += "_";
			fieldstr = fieldstr + field;
		}
		return fieldstr;
	}

	/**
	 * Inserts the elements of a String array into an ArrayList making sure not
	 * to duplicate entries in the ArrayList
	 * 
	 * @param list
	 *            The ArrayList containing unique entries
	 * @param array
	 *            The String array to be inserted into the ArrayList
	 * @return The updated ArrayList with new unique entries
	 */
	private static ArrayList<String> uniqueInsert(ArrayList<String> list,
			String[] array) {
		if (array != null) {
			for (int i = 0; i < array.length; i++) {
				if (!list.contains(array[i]))
					list.add(array[i]);
			}
		}
		return list;
	}

	/**
	 * Generates DML specifying table columns and their datatypes. The output of
	 * this routine should be used within a CREATE TABLE statement.
	 * 
	 * @param fields
	 *            Contains unique field names
	 * @param datatype
	 *            Specifies the SQL data type that the fields should take on.
	 * @return The SQL code to be included in a CREATE TABLE statement.
	 */
	public static String fieldsAsCols(ArrayList<String> fields, String datatype) {
		String str = "";
		String field = "";
		ListIterator<String> li = fields.listIterator();
		while (li.hasNext()) {
			field = li.next();
			if (reservedDBWords.contains(field))
				field = field + "_";
			str = str + field + datatype;
			if (li.hasNext())
				str = str + ", ";
		}
		return str;
	}
	
	/**
	 * 
	 * @param allFields
	 *            All existent fields for a given entry type
	 * @param reqFields
	 *            list containing required fields for an entry type
	 * @param optFields
	 *            list containing optional fields for an entry type
	 * @param utiFields
	 *            list containing utility fields for an entry type
	 * @param origList
	 *            original list with the correct size filled with the default
	 *            values for each field
	 * @return origList changing the values of the fields that appear on
	 *         reqFields, optFields, utiFields set to 'req', 'opt' and 'uti'
	 *         respectively
	 */
	public static ArrayList<String> setFieldRequirement(
			ArrayList<String> allFields, List<String> reqFields,
			List<String> optFields, List<String> utiFields,
			ArrayList<String> origList) {

		String currentField = null;
		for (int i = 0; i < allFields.size(); i++) {
			currentField = allFields.get(i);
			if (reqFields.contains(currentField))
				origList.set(i, "req");
			else if (optFields.contains(currentField))
				origList.set(i, "opt");
			else if (utiFields.contains(currentField))
				origList.set(i, "uti");
		}
		return origList;
	}

	/**
	 * Return a message raised from a SQLException
	 * 
	 * @param ex
	 *            The SQLException raised
	 */
	public static String getExceptionMessage(Exception ex) {
		String msg = null;
		if (ex.getMessage() == null) {
			msg = ex.toString();
		} else {
			msg = ex.getMessage();
		}
		return msg;
	}

	/**
	 * return a ResultSet with the result of a "SELECT *" query for a given
	 * table
	 * 
	 * @param conn
	 *            Connection to the database
	 * @param tableName
	 *            String containing the name of the table you want to get the
	 *            results.
	 * @return a ResultSet with the query result returned from the DB
	 * @throws SQLException
	 */
	public static ResultSet queryAllFromTable(Connection conn, String tableName)
			throws SQLException {
		String query = "SELECT * FROM " + tableName + ";";
		Statement res = (Statement) processQueryWithResults(conn, query);
		return res.getResultSet();
	}

	/**
	 * Utility method for processing DML with proper output
	 * 
	 * @param out
	 *            The output (PrintStream or Connection) object to which the DML
	 *            should be sent
	 * @param dml
	 *            The DML statements to be processed
	 */
	public static void processQuery(Object out, String dml) throws SQLException {
		if (out instanceof PrintStream) {
			PrintStream fout = (PrintStream) out;
			fout.println(dml);
		}
		if (out instanceof Connection) {
			Connection conn = (Connection) out;
			executeQuery(conn, dml);
		}
	}

	/**
	 * Utility method for processing DML with proper output
	 * 
	 * @param out
	 *            The output (PrintStream or Connection) object to which the DML
	 *            should be sent
	 * @param query
	 *            The DML statements to be processed
	 * @return the result of the statement
	 */
	public static Object processQueryWithResults(Object out, String query)
			throws SQLException {
		if (out instanceof PrintStream) {// TODO: how to handle the PrintStream
											// case?
			PrintStream fout = (PrintStream) out;
			fout.println(query);
			return fout;
		}
		if (out instanceof Connection) {
			Connection conn = (Connection) out;
			return executeQueryWithResults(conn, query);
		}
		return null;
	}

	/**
	 * This routine returns the JDBC url corresponding to the DBStrings input.
	 * 
	 * @param dbStrings
	 *            The DBStrings to use to make the connection
	 * @return The JDBC url corresponding to the input DBStrings
	 */
	public static String createJDBCurl(DBStrings dbStrings, boolean withDBName) {
		String url = "";
		url = "jdbc:" + dbStrings.getServerType().toLowerCase() + "://"
				+ dbStrings.getServerHostname()
				+ (withDBName ? "/" + dbStrings.getDatabase() : "");
		return url;
	}

	/**
	 * Process a query and returns only the first result of a result set as a
	 * String. To be used when it is certain that only one String (single cell)
	 * will be returned from the DB
	 * 
	 * @param out
	 *            The output (PrintStream or Connection) object to which the DML
	 *            should be sent
	 * @param query
	 *            The query statements to be processed
	 * @return String with the result returned from the database
	 * @throws SQLException
	 */
	public static String processQueryWithSingleResult(Connection conn,
			String query) throws SQLException {
		ResultSet rs = ((Statement) executeQueryWithResults(conn, query))
				.getResultSet();
		rs.next();
		String result = rs.getString(1);
		rs.getStatement().close();
		return result;
	}

	/**
	 * Utility method for executing DML
	 * 
	 * @param conn
	 *            The DML Connection object that will execute the SQL
	 * @param qry
	 *            The DML statements to be executed
	 */
	public static void executeQuery(Connection conn, String qry)
			throws SQLException {
		Statement stmnt = conn.createStatement();
		stmnt.execute(qry);
		SQLWarning warn = stmnt.getWarnings();
		if (warn != null) {
			System.err.println(warn.toString());
		}
		stmnt.close();
	}

	/**
	 * Utility method for executing DML
	 * 
	 * @param conn
	 *            The DML Connection object that will execute the SQL
	 * @param qry
	 *            The DML statements to be executed
	 */
	public static Statement executeQueryWithResults(Connection conn, String qry)
			throws SQLException {
		Statement stmnt = conn.createStatement();
		stmnt.executeQuery(qry);
		SQLWarning warn = stmnt.getWarnings();
		if (warn != null) {

			System.err.println(warn.toString());
		}
		return stmnt;
	}
	
	
}