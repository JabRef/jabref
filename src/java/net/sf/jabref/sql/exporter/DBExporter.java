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
package net.sf.jabref.sql.exporter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.BibtexString;
import net.sf.jabref.MetaData;
import net.sf.jabref.Util;
import net.sf.jabref.export.FileActions;
import net.sf.jabref.groups.AbstractGroup;
import net.sf.jabref.groups.AllEntriesGroup;
import net.sf.jabref.groups.ExplicitGroup;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.groups.KeywordGroup;
import net.sf.jabref.groups.SearchGroup;
import net.sf.jabref.sql.DBStrings;
import net.sf.jabref.sql.SQLUtil;

/**
 * 
 * @author ifsteinm.
 * 
 *         Jan 20th Abstract Class to provide main features to export entries to
 *         a DB. To insert a new DB it is necessary to extend this class and add
 *         the DB name the enum available at
 *         net.sf.jabref.sql.DBImporterAndExporterFactory (and to the GUI). This
 *         class and its subclasses create database, entries and related stuff
 *         within a DB.
 * 
 */

public abstract class DBExporter {

	String fieldStr = SQLUtil.getFieldStr();
	DBStrings dbStrings = null;

	/**
	 * Method for the exportDatabase methods.
	 * 
	 * @param dbtype
	 *            The DBTYPE of the database
	 * @param database
	 *            The BibtexDatabase to export
	 * @param metaData
	 *            The MetaData object containing the groups information
	 * @param keySet
	 *            The set of IDs of the entries to export.
	 * @param out
	 *            The output (PrintStream or Connection) object to which the DML
	 *            should be written.
	 */
	private void performExport(final BibtexDatabase database,
			final MetaData metaData, Set<String> keySet, Object out)
			throws Exception {

		List<BibtexEntry> entries = FileActions.getSortedEntries(database,
				keySet, false);
		GroupTreeNode gtn = metaData.getGroups();

		createTables(out);
		int database_id = getDatabaseIDByPath(metaData, out);
		removeAllRecordsForAGivenDB(out, database_id);
		populateEntryTypesTable(out);
		populateEntriesTable(database_id, entries, out);
		populateStringTable(database, out, database_id);
		populateGroupTypesTable(out);
		populateGroupsTable(gtn, 0, 1, out, database_id);
		populateEntryGroupsTable(gtn, 0, 1, out, database_id);
	}

	/**
	 * Generates the DML required to populate the entries table with jabref data
	 * and writes it to the output PrintStream.
	 * 
	 * @param database_id
	 *            ID of Jabref database related to the entries to be exported
	 *            This information can be gathered using
	 *            getDatabaseIDByPath(metaData, out)
	 * @param entries
	 *            The BibtexEntries to export
	 * @param out
	 *            The output (PrintStream or Connection) object to which the DML
	 *            should be written.
	 */
	private void populateEntriesTable(int database_id,
			List<BibtexEntry> entries, Object out) throws SQLException {
		String query = "";
		String val = "";
		String insert = "INSERT INTO entries (jabref_eid, entry_types_id, cite_key, "
				+ fieldStr + ", database_id) VALUES (";
		for (BibtexEntry entry : entries) {
			query = insert + "'" + entry.getId() + "'"
					+ ", (SELECT entry_types_id FROM entry_types WHERE label='"
					+ entry.getType().getName().toLowerCase() + "'), '"
					+ entry.getCiteKey() + "'";
			for (int i = 0; i < SQLUtil.getAllFields().size(); i++) {
				query = query + ", ";
				val = entry.getField(SQLUtil.getAllFields().get(i));
				if (val != null) {
					val = val.replace("\\", "\\\\");
					val = val.replace("\"", "\\\"");
					val = val.replace("\'", "''");
					val = val.replace("`", "\\`");
					query = query + "'" + val + "'";
				} else {
					query = query + "NULL";
				}
			}
			query = query + ", '" + database_id + "');";
			SQLUtil.processQuery(out, query);
		}
	}

	/**
	 * This method creates a new row into jabref_database table enabling to
	 * export more than one .bib
	 * 
	 * @param metaData
	 *            The MetaData object containing the groups information
	 * @param out
	 *            The output (PrintStream or Connection) object to which the DML
	 *            should be written.
	 * 
	 * @throws SQLException
	 */
	private void insertJabRefDatabase(final MetaData metaData, Object out)
			throws SQLException {
		SQLUtil.processQuery(out,
				"INSERT INTO jabref_database(database_name, md5_path) VALUES ('"
						+ metaData.getFile().getName() + "', md5('"
						+ metaData.getFile().getAbsolutePath() + "'));");
	}

	/**
	 * Recursive method to include a tree of groups.
	 * 
	 * @param cursor
	 *            The current GroupTreeNode in the GroupsTree
	 * @param parentID
	 *            The integer ID associated with the cursors's parent node
	 * @param currentID
	 *            The integer value to associate with the cursor
	 * @param out
	 *            The output (PrintStream or Connection) object to which the DML
	 *            should be written.
	 * @param database_id
	 *            Id of jabref database to which the group is part of
	 */

	private int populateEntryGroupsTable(GroupTreeNode cursor, int parentID,
			int currentID, Object out, int database_id) throws SQLException {
		// if this group contains entries...
		if (cursor.getGroup() instanceof ExplicitGroup) {
			ExplicitGroup grp = (ExplicitGroup) cursor.getGroup();
			for (BibtexEntry be : grp.getEntries()) {
				SQLUtil.processQuery(
						out,
						"INSERT INTO entry_group (entries_id, groups_id) "
								+ "VALUES ("
								+ "(SELECT entries_id FROM entries WHERE jabref_eid="
								+ "'"
								+ be.getId()
								+ "' AND database_id = "
								+ database_id
								+ "), "
								+ "(SELECT groups_id FROM groups WHERE database_id="
								+ "'" + database_id + "' AND parent_id=" + "'"
								+ parentID + "' AND label=" + "'"
								+ grp.getName() + "')" + ");");
			}
		}
		// recurse on child nodes (depth-first traversal)
		Object response = SQLUtil.processQueryWithResults(out,
				"SELECT groups_id FROM groups WHERE label='"
						+ cursor.getGroup().getName() + "' AND database_id='"
						+ database_id + "' AND parent_id='" + parentID + "';");
		// setting values to ID and myID to be used in case of textual SQL
		// export
		int myID = ++currentID;
		if (response instanceof Statement) {
			ResultSet rs = ((Statement) response).getResultSet();
			rs.next();
			myID = rs.getInt("groups_id");
		}
		for (Enumeration<GroupTreeNode> e = cursor.children(); e
				.hasMoreElements();)
			currentID = populateEntryGroupsTable(e.nextElement(), myID,
					currentID, out, database_id);
		return currentID;
	}

	/**
	 * Generates the SQL required to populate the entry_types table with jabref
	 * data.
	 * 
	 * @param out
	 *            The output (PrintSream or Connection) object to which the DML
	 *            should be written.
	 */

	private void populateEntryTypesTable(Object out) throws SQLException {
		String query = "";
		ArrayList<String> fieldRequirement = new ArrayList<String>();

		ArrayList<String> existentTypes = new ArrayList<String>();
		if (out instanceof Connection) {
			ResultSet rs = ((Statement) SQLUtil.processQueryWithResults(out,
					"SELECT label FROM entry_types")).getResultSet();
			while (rs.next()) {
				existentTypes.add(rs.getString(1));
			}
		}
		for (BibtexEntryType val : BibtexEntryType.ALL_TYPES.values()) {
			fieldRequirement.clear();
			for (int i = 0; i < SQLUtil.getAllFields().size(); i++) {
				fieldRequirement.add(i, "gen");
			}
			List<String> reqFields = Arrays
					.asList(val.getRequiredFields() != null ? val
							.getRequiredFields() : new String[0]);
			List<String> optFields = Arrays
					.asList(val.getOptionalFields() != null ? val
							.getOptionalFields() : new String[0]);
			List<String> utiFields = Arrays
					.asList(val.getUtilityFields() != null ? val
							.getUtilityFields() : new String[0]);
			fieldRequirement = SQLUtil.setFieldRequirement(
					SQLUtil.getAllFields(), reqFields, optFields, utiFields,
					fieldRequirement);
			if (!existentTypes.contains(val.getName().toLowerCase())) {
				String insert = "INSERT INTO entry_types (label, " + fieldStr
						+ ") VALUES (";
				query = insert + "'" + val.getName().toLowerCase() + "'";
				for (int i = 0; i < fieldRequirement.size(); i++) {
					query = query + ", '" + fieldRequirement.get(i) + "'";
				}
				query = query + ");";
			} else {
				String[] update = fieldStr.split(",");
				query = "UPDATE entry_types SET \n";
				for (int i = 0; i < fieldRequirement.size(); i++) {
					query += update[i] + "='" + fieldRequirement.get(i) + "',";
				}
				query = query.substring(0, query.lastIndexOf(","));
				query += " WHERE label='" + val.getName().toLowerCase() + "'";
			}
			SQLUtil.processQuery(out, query);
		}
	}

	/**
	 * Recursive worker method for the populateGroupsTable methods.
	 * 
	 * @param cursor
	 *            The current GroupTreeNode in the GroupsTree
	 * @param parentID
	 *            The integer ID associated with the cursors's parent node
	 * @param ID
	 *            The integer value to associate with the cursor
	 * @param out
	 *            The output (PrintStream or Connection) object to which the DML
	 *            should be written.
	 * @param database_id
	 *            Id of jabref database to which the groups/entries are part of
	 */
	private int populateGroupsTable(GroupTreeNode cursor, int parentID,
			int currentID, Object out, int database_id) throws SQLException {

		AbstractGroup group = cursor.getGroup();
		String searchField = null, searchExpr = null, caseSens = null, reg_exp = null;
		int hierContext = group.getHierarchicalContext();
		if (group instanceof KeywordGroup) {
			searchField = ((KeywordGroup) group).getSearchField();
			searchExpr = ((KeywordGroup) group).getSearchExpression();
			caseSens = ((KeywordGroup) group).isCaseSensitive() ? "1" : "0";
			reg_exp = ((KeywordGroup) group).isRegExp() ? "1" : "0";
		} else if (group instanceof SearchGroup) {
			searchExpr = ((SearchGroup) group).getSearchExpression();
			caseSens = ((SearchGroup) group).isCaseSensitive() ? "1" : "0";
			reg_exp = ((SearchGroup) group).isRegExp() ? "1" : "0";
		}
		// Protect all quotes in the group descriptions:
		if (searchField != null)
			searchField = Util.quote(searchField, "'", '\\');
		if (searchExpr != null)
			searchExpr = Util.quote(searchExpr, "'", '\\');

		SQLUtil.processQuery(
				out,
				"INSERT INTO groups (label, parent_id, group_types_id, search_field, "
						+ "search_expression, case_sensitive, reg_exp, hierarchical_context, database_id) "
						+ "VALUES ('"
						+ group.getName()
						+ "', "
						+ parentID
						+ ", (SELECT group_types_id FROM group_types where label='"
						+ group.getTypeId()
						+ "')"
						+ ", "
						+ (searchField != null ? "'" + searchField + "'"
								: "NULL")
						+ ", "
						+ (searchExpr != null ? "'" + searchExpr + "'" : "NULL")
						+ ", "
						+ (caseSens != null ? "'" + caseSens + "'" : "NULL")
						+ ", "
						+ (reg_exp != null ? "'" + reg_exp + "'" : "NULL")
						+ ", " + hierContext + ", '" + database_id + "');");
		// recurse on child nodes (depth-first traversal)
		Object response = SQLUtil.processQueryWithResults(out,
				"SELECT groups_id FROM groups WHERE label='"
						+ cursor.getGroup().getName() + "' AND database_id='"
						+ database_id + "' AND parent_id='" + parentID + "';");
		// setting values to ID and myID to be used in case of textual SQL
		// export
		int myID = currentID;
		if (response instanceof Statement) {
			ResultSet rs = ((Statement) response).getResultSet();
			rs.next();
			myID = rs.getInt("groups_id");
		}
		for (Enumeration<GroupTreeNode> e = cursor.children(); e
				.hasMoreElements();)
			currentID = populateGroupsTable(e.nextElement(), myID, ++currentID,
					out, database_id);
		return currentID;
	}

	/**
	 * Generates the DML required to populate the group_types table with JabRef
	 * data.
	 * 
	 * @param out
	 *            The output (PrintSream or Connection) object to which the DML
	 *            should be written.
	 * 
	 * @throws SQLException
	 */
	private void populateGroupTypesTable(Object out) throws SQLException {
		int quantidade = 0;
		if (out instanceof Connection) {
			ResultSet res = ((Statement) SQLUtil.processQueryWithResults(out,
					"SELECT COUNT(*) AS amount FROM group_types"))
					.getResultSet();
			res.next();
			quantidade = res.getInt("amount");
			res.getStatement().close();
		}
		if (quantidade == 0) {
			String[] typeNames = new String[] { AllEntriesGroup.ID,
					ExplicitGroup.ID, KeywordGroup.ID, SearchGroup.ID };
			for (int i = 0; i < typeNames.length; i++) {
				String typeName = typeNames[i];
				String insert = "INSERT INTO group_types (label) VALUES ('"
						+ typeName + "');";
				SQLUtil.processQuery(out, insert);
			}
		}
	}

	/**
	 * Generates the SQL required to populate the strings table with jabref
	 * data.
	 * 
	 * @param database
	 *            BibtexDatabase object used from where the strings will be
	 *            exported
	 * @param out
	 *            The output (PrintStream or Connection) object to which the DML
	 *            should be written.
	 * @param database_id
	 *            ID of Jabref database related to the entries to be exported
	 *            This information can be gathered using
	 *            getDatabaseIDByPath(metaData, out)
	 * @throws SQLException
	 */
	private void populateStringTable(BibtexDatabase database, Object out,
			int database_id) throws SQLException {
		String insert = "INSERT INTO strings (label, content, database_id) VALUES (";

		if (database.getPreamble() != null) {
			String dml = insert + "'@PREAMBLE', " + "'"
					+ Util.quote(database.getPreamble(), "'", '\\') + "', "
					+ "'" + database_id + "');";
			SQLUtil.processQuery(out, dml);
		}
		Iterator<String> it = database.getStringKeySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			BibtexString string = database.getString(key);
			String dml = insert + "'" + Util.quote(string.getName(), "'", '\\')
					+ "', " + "'" + Util.quote(string.getContent(), "'", '\\')
					+ "', " + "'" + database_id + "'" + ");";
			SQLUtil.processQuery(out, dml);
		}
	}

	/**
	 * Given a DBStrings it connects to the DB and returns the
	 * java.sql.Connection object
	 * 
	 * @param dbstrings
	 *            The DBStrings to use to make the connection
	 * @return java.sql.Connection to the DB chosen
	 * @throws Exception
	 */
	public abstract Connection connectToDB(DBStrings dbstrings)
			throws Exception;

	/**
	 * Generates DML code necessary to create all tables in a database, and
	 * writes it to appropriate output.
	 * 
	 * @param out
	 *            The output (PrintStream or Connection) object to which the DML
	 *            should be written.
	 */
	protected abstract void createTables(Object out) throws SQLException;

	/**
	 * Accepts the BibtexDatabase and MetaData, generates the DML required to
	 * create and populate SQL database tables, and writes this DML to the
	 * specified output file.
	 * 
	 * @param database
	 *            The BibtexDatabase to export
	 * @param metaData
	 *            The MetaData object containing the groups information
	 * @param keySet
	 *            The set of IDs of the entries to export.
	 * @param file
	 *            The name of the file to which the DML should be written
	 */
	public void exportDatabaseAsFile(final BibtexDatabase database,
			final MetaData metaData, Set<String> keySet, String file)
			throws Exception {

		// open output file
		File outfile = new File(file);
		if (outfile.exists())
			outfile.delete();
		BufferedOutputStream writer = null;
		writer = new BufferedOutputStream(new FileOutputStream(outfile));
		PrintStream fout = null;
		fout = new PrintStream(writer);
		performExport(database, metaData, keySet, fout);
		fout.close();
	}

	/**
	 * Accepts the BibtexDatabase and MetaData, generates the DML required to
	 * create and populate SQL database tables, and writes this DML to the
	 * specified SQL database.
	 * 
	 * @param database
	 *            The BibtexDatabase to export
	 * @param metaData
	 *            The MetaData object containing the groups information
	 * @param keySet
	 *            The set of IDs of the entries to export.
	 * @param dbStrings
	 *            The necessary database connection information
	 */
	public void exportDatabaseToDBMS(final BibtexDatabase database,
			final MetaData metaData, Set<String> keySet, DBStrings dbStrings)
			throws Exception {

		Connection conn = null;
		try {
			conn = this.connectToDB(dbStrings);

			performExport(database, metaData, keySet, conn);

			if (!conn.getAutoCommit()) {
				conn.commit();
				conn.setAutoCommit(true);
			}
			conn.close();
		} catch (SQLException ex) {
			if (conn != null) {
				if (!conn.getAutoCommit()) {
					conn.rollback();
				}
			}
			throw ex;
		}
	}

	/**
	 * Returns a Jabref Database ID from the database in case the DB is already
	 * exported. In case the bib was already exported before, the method returns
	 * the id, otherwise it calls the method that inserts a new row and returns
	 * the ID for this new database
	 * 
	 * @param metaData
	 *            The MetaData object containing the database information
	 * @param out
	 *            The output (PrintStream or Connection) object to which the DML
	 *            should be written.
	 * @return The ID of database row of the jabref database being exported
	 * @throws SQLException
	 */
	public int getDatabaseIDByPath(MetaData metaData, Object out)
			throws SQLException {

		if (out instanceof Connection) {
			Object response = SQLUtil.processQueryWithResults(out,
					"SELECT database_id FROM jabref_database WHERE md5_path=md5('"
							+ metaData.getFile().getAbsolutePath() + "');");
			ResultSet rs = ((Statement) response).getResultSet();
			if (rs.next())
				return rs.getInt("database_id");
			else {
				insertJabRefDatabase(metaData, out);
				return getDatabaseIDByPath(metaData, out);
			}
		}
		// in case of text export there will be only 1 bib exported
		else {
			insertJabRefDatabase(metaData, out);
			return 1;
		}
	}

	/**
	 * Removes all records for the database being exported in case it was
	 * exported before.
	 * 
	 * @param out
	 *            The output (PrintStream or Connection) object to which the DML
	 *            should be written.
	 * @param database_id
	 * 			  Id of the database being exported.
	 * @throws SQLException
	 */
	public void removeAllRecordsForAGivenDB(Object out, int database_id)
			throws SQLException {
		SQLUtil.processQuery(out, "DELETE FROM entries WHERE database_id='"
				+ database_id + "';");
		SQLUtil.processQuery(out, "DELETE FROM groups WHERE database_id='"
				+ database_id + "';");
		SQLUtil.processQuery(out, "DELETE FROM strings WHERE database_id='"
				+ database_id + "';");
	}
}