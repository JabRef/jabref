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
package net.sf.jabref.sql.exporter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import javax.swing.JOptionPane;

import net.sf.jabref.*;
import net.sf.jabref.model.database.BibDatabaseMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.groups.structure.*;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.exporter.BibDatabaseWriter;
import net.sf.jabref.exporter.SavePreferences;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.sql.DBImportExportDialog;
import net.sf.jabref.sql.DBImporterExporter;
import net.sf.jabref.sql.DBStrings;
import net.sf.jabref.sql.SQLUtil;

/**
 * @author igorsteinmacher.
 *         <p>
 *         Jan 20th Abstract Class to provide main features to export entries to a DB. To insert a new DB it is
 *         necessary to extend this class and add the DB name the enum available at
 *         net.sf.jabref.sql.DBImporterAndExporterFactory (and to the GUI). This class and its subclasses create
 *         database, entries and related stuff within a DB.
 */

public abstract class DBExporter extends DBImporterExporter {

    private final String fieldStr = SQLUtil.getFieldStr();
    protected DBStrings dbStrings;
    private final List<String> dbNames = new ArrayList<>();

    private static final Log LOGGER = LogFactory.getLog(DBExporter.class);


    /**
     * Method for the exportDatabase methods.
     *
     * @param databaseContext the database to export
     * @param entriesToExport The list of the entries to export.
     * @param out      The output (PrintStream or Connection) object to which the DML should be written.
     */
    private void performExport(BibDatabaseContext databaseContext, List<BibEntry> entriesToExport,
            Object out, String dbName) throws Exception {

        SavePreferences savePrefs = SavePreferences.loadForExportFromPreferences(Globals.prefs);
        List<BibEntry> entries = BibDatabaseWriter.getSortedEntries(databaseContext, entriesToExport, savePrefs);
        GroupTreeNode gtn = databaseContext.getMetaData().getGroups();

        final int database_id = getDatabaseIDByName(databaseContext, out, dbName);
        removeAllRecordsForAGivenDB(out, database_id);
        populateEntryTypesTable(out, databaseContext.getMode());
        populateEntriesTable(database_id, entries, out);
        populateStringTable(databaseContext.getDatabase(), out, database_id);
        populateGroupTypesTable(out);
        populateGroupsTable(gtn, 0, 1, out, database_id);
        populateEntryGroupsTable(gtn, 0, 1, out, database_id);
    }

    /**
     * Generates the DML required to populate the entries table with jabref data and writes it to the output
     * PrintStream.
     *
     * @param database_id ID of Jabref database related to the entries to be exported This information can be gathered
     *                    using getDatabaseIDByPath(metaData, out)
     * @param entries     The BibtexEntries to export
     * @param out         The output (PrintStream or Connection) object to which the DML should be written.
     */
    private void populateEntriesTable(final int database_id, List<BibEntry> entries, Object out) throws SQLException {
        StringBuilder query = new StringBuilder(75);
        String insert = "INSERT INTO entries (jabref_eid, entry_types_id, cite_key, " + fieldStr
                + ", database_id) VALUES (";
        for (BibEntry entry : entries) {
            query.append(insert).append('\'').append(entry.getId())
                    .append("', (SELECT entry_types_id FROM entry_types WHERE label='").append(entry.getType())
                    .append("'), '").append(entry.getCiteKey()).append('\'');
            for (int i = 0; i < SQLUtil.getAllFields().size(); i++) {
                query.append(", ");
                if (entry.hasField(SQLUtil.getAllFields().get(i))) {
                    String val = entry.getField(SQLUtil.getAllFields().get(i));
                    /**
                     * The condition below is there since PostgreSQL automatically escapes the backslashes, so the entry
                     * would double the number of slashes after storing/retrieving.
                     **/
                    if ((out instanceof Connection) && "MySQL".equals(dbStrings.getServerType())) {
                        val = val.replace("\\", "\\\\");
                        val = val.replace("\"", "\\\"");
                        val = val.replace("\'", "''");
                        val = val.replace("`", "\\`");
                    }
                    query.append('\'').append(val).append('\'');
                } else {
                    query.append("NULL");
                }
            }
            query.append(", '").append(database_id).append("');");
        }
        SQLUtil.processQuery(out, query.toString());
    }

    /**
     * Recursive method to include a tree of groups.
     *
     * @param cursor      The current GroupTreeNode in the GroupsTree
     * @param parentID    The integer ID associated with the cursors's parent node
     * @param currentID   The integer value to associate with the cursor
     * @param out         The output (PrintStream or Connection) object to which the DML should be written.
     * @param database_id Id of jabref database to which the group is part of
     */

    private int populateEntryGroupsTable(GroupTreeNode cursor, int parentID, int currentID, Object out,
            final int database_id) throws SQLException {
        // if this group contains entries...
        if (cursor.getGroup() instanceof ExplicitGroup) {
            ExplicitGroup grp = (ExplicitGroup) cursor.getGroup();
            for (BibEntry be : grp.getEntries()) {
                SQLUtil.processQuery(out, "INSERT INTO entry_group (entries_id, groups_id) " + "VALUES ("
                        + "(SELECT entries_id FROM entries WHERE jabref_eid=" + '\'' + be.getId()
                        + "' AND database_id = " + database_id + "), "
                        + "(SELECT groups_id FROM groups WHERE database_id=" + '\'' + database_id + "' AND parent_id="
                        + '\'' + parentID + "' AND label=" + '\'' + grp.getName() + "')" + ");");
            }
        }
        // recurse on child nodes (depth-first traversal)
        try (AutoCloseable response = SQLUtil.processQueryWithResults(out,
                "SELECT groups_id FROM groups WHERE label='" + cursor.getGroup().getName() + "' AND database_id='"
                        + database_id + "' AND parent_id='" + parentID + "';")) {
            // setting values to ID and myID to be used in case of textual SQL
            // export
            ++currentID;
            int myID = currentID;
            if (response instanceof Statement) {
                try (ResultSet rs = ((Statement) response).getResultSet()) {
                    rs.next();
                    myID = rs.getInt("groups_id");
                } finally {
                    ((Statement) response).close();
                }
            }

            for (Enumeration<GroupTreeNode> e = cursor.children(); e.hasMoreElements();) {
                currentID = populateEntryGroupsTable(e.nextElement(), myID, currentID, out, database_id);
            }
            //Unfortunatley, AutoCloseable throws only Exception
        } catch (Exception e) {
            LOGGER.warn("Cannot close resource", e);
        }
        return currentID;
    }

    /**
     * Generates the SQL required to populate the entry_types table with jabref data.
     *
     * @param out The output (PrintSream or Connection) object to which the DML should be written.
     * @param type
     */

    private void populateEntryTypesTable(Object out, BibDatabaseMode type) throws SQLException {
        List<String> fieldRequirement = new ArrayList<>();

        List<String> existentTypes = new ArrayList<>();
        if (out instanceof Connection) {
            try (Statement sm = (Statement) SQLUtil.processQueryWithResults(out, "SELECT label FROM entry_types");
                    ResultSet rs = sm.getResultSet()) {
                while (rs.next()) {
                    existentTypes.add(rs.getString(1));
                }
            }
        }
        for (EntryType val : EntryTypes.getAllValues(type)) {
            StringBuilder querySB = new StringBuilder();

            fieldRequirement.clear();
            for (int i = 0; i < SQLUtil.getAllFields().size(); i++) {
                fieldRequirement.add(i, "gen");
            }
            List<String> reqFields = val.getRequiredFieldsFlat();
            List<String> optFields = val.getOptionalFields();
            List<String> utiFields = Collections.singletonList("search");
            fieldRequirement = SQLUtil.setFieldRequirement(SQLUtil.getAllFields(), reqFields, optFields, utiFields,
                    fieldRequirement);
            if (existentTypes.contains(val.getName().toLowerCase())) {
                String[] update = fieldStr.split(",");
                querySB.append("UPDATE entry_types SET \n");
                for (int i = 0; i < fieldRequirement.size(); i++) {
                    querySB.append(update[i]).append("='").append(fieldRequirement.get(i)).append("',");
                }
                querySB.delete(querySB.lastIndexOf(","), querySB.length());
                querySB.append(" WHERE label='").append(val.getName().toLowerCase()).append("';");
            } else {
                querySB.append("INSERT INTO entry_types (label, ").append(fieldStr).append(") VALUES ('")
                        .append(val.getName().toLowerCase()).append('\'');
                for (String aFieldRequirement : fieldRequirement) {
                    querySB.append(", '").append(aFieldRequirement).append('\'');
                }
                querySB.append(");");
            }
            SQLUtil.processQuery(out, querySB.toString());
        }
    }

    /**
     * Recursive worker method for the populateGroupsTable methods.
     *
     * @param cursor      The current GroupTreeNode in the GroupsTree
     * @param parentID    The integer ID associated with the cursors's parent node
     * @param currentID   The integer value to associate with the cursor
     * @param out         The output (PrintStream or Connection) object to which the DML should be written.
     * @param database_id Id of jabref database to which the groups/entries are part of
     */
    private int populateGroupsTable(GroupTreeNode cursor, int parentID, int currentID, Object out,
            final int database_id) throws SQLException {

        AbstractGroup group = cursor.getGroup();
        String searchField = null;
        String searchExpr = null;
        String caseSens = null;
        String regExp = null;
        GroupHierarchyType hierContext = group.getHierarchicalContext();
        if (group instanceof KeywordGroup) {
            searchField = ((KeywordGroup) group).getSearchField();
            searchExpr = ((KeywordGroup) group).getSearchExpression();
            caseSens = ((KeywordGroup) group).isCaseSensitive() ? "1" : "0";
            regExp = ((KeywordGroup) group).isRegExp() ? "1" : "0";
        } else if (group instanceof SearchGroup) {
            searchExpr = ((SearchGroup) group).getSearchExpression();
            caseSens = ((SearchGroup) group).isCaseSensitive() ? "1" : "0";
            regExp = ((SearchGroup) group).isRegExp() ? "1" : "0";
        }
        // Protect all quotes in the group descriptions:
        if (searchField != null) {
            searchField = StringUtil.quote(searchField, "'", '\\');
        }
        if (searchExpr != null) {
            searchExpr = StringUtil.quote(searchExpr, "'", '\\');
        }

        SQLUtil.processQuery(out, "INSERT INTO groups (label, parent_id, group_types_id, search_field, "
                + "search_expression, case_sensitive, reg_exp, hierarchical_context, database_id) " + "VALUES ('"
                + group.getName() + "', " + parentID + ", (SELECT group_types_id FROM group_types where label='"
                + group.getTypeId() + "')" + ", " + (searchField != null ? '\'' + searchField + '\'' : "NULL") + ", "
                + (searchExpr != null ? '\'' + searchExpr + '\'' : "NULL") + ", "
                + (caseSens != null ? '\'' + caseSens + '\'' : "NULL") + ", "
                + (regExp != null ? '\'' + regExp + '\'' : "NULL") + ", " + hierContext.ordinal() + ", '" + database_id
                + "');");
        // recurse on child nodes (depth-first traversal)
        try (AutoCloseable response = SQLUtil.processQueryWithResults(out,
                "SELECT groups_id FROM groups WHERE label='" + cursor.getGroup().getName() + "' AND database_id='"
                        + database_id + "' AND parent_id='" + parentID + "';")) {
            // setting values to ID and myID to be used in case of textual SQL
            // export
            int myID = currentID;
            if (response instanceof Statement) {
                try (ResultSet rs = ((Statement) response).getResultSet()) {
                    rs.next();
                    myID = rs.getInt("groups_id");
                } finally {
                    ((Statement) response).close();
                }
            }
            for (Enumeration<GroupTreeNode> e = cursor.children(); e.hasMoreElements();) {
                ++currentID;
                currentID = populateGroupsTable(e.nextElement(), myID, currentID, out, database_id);
            }
            //Unfortunatley, AutoCloseable throws only Exception
        } catch (Exception e) {
            LOGGER.warn("Cannot close resource", e);
        }
        return currentID;
    }

    /**
     * Generates the DML required to populate the group_types table with JabRef data.
     *
     * @param out The output (PrintSream or Connection) object to which the DML should be written.
     * @throws SQLException
     */
    private static void populateGroupTypesTable(Object out) throws SQLException {
        int quantity = 0;
        if (out instanceof Connection) {
            try (Statement sm = (Statement) SQLUtil.processQueryWithResults(out,
                    "SELECT COUNT(*) AS amount FROM group_types"); ResultSet res = sm.getResultSet()) {
                res.next();
                quantity = res.getInt("amount");
            }
        }
        if (quantity == 0) {
            String[] typeNames = new String[] {AllEntriesGroup.ID, ExplicitGroup.ID, KeywordGroup.ID, SearchGroup.ID};
            for (String typeName : typeNames) {
                String insert = "INSERT INTO group_types (label) VALUES ('" + typeName + "');";
                SQLUtil.processQuery(out, insert);
            }
        }
    }

    /**
     * Generates the SQL required to populate the strings table with jabref data.
     *
     * @param database    BibDatabase object used from where the strings will be exported
     * @param out         The output (PrintStream or Connection) object to which the DML should be written.
     * @param database_id ID of Jabref database related to the entries to be exported This information can be gathered
     *                    using getDatabaseIDByPath(metaData, out)
     * @throws SQLException
     */
    private static void populateStringTable(BibDatabase database, Object out, final int database_id)
            throws SQLException {
        String insert = "INSERT INTO strings (label, content, database_id) VALUES (";

        if (database.getPreamble() != null) {
            String dml = insert + "'@PREAMBLE', " + '\'' + StringUtil.quote(database.getPreamble(), "'", '\\') + "', "
                    + '\'' + database_id + "');";
            SQLUtil.processQuery(out, dml);
        }
        for (String key : database.getStringKeySet()) {
            BibtexString string = database.getString(key);
            String dml = insert + '\'' + StringUtil.quote(string.getName(), "'", '\\') + "', " + '\''
                    + StringUtil.quote(string.getContent(), "'", '\\') + "', " + '\'' + database_id + '\'' + ");";
            SQLUtil.processQuery(out, dml);
        }
    }

    /**
     * Given a DBStrings it connects to the DB and returns the java.sql.Connection object
     *
     * @param dbstrings The DBStrings to use to make the connection
     * @return java.sql.Connection to the DB chosen
     * @throws Exception
     */
    public abstract Connection connectToDB(DBStrings dbstrings) throws Exception;

    /**
     * Generates DML code necessary to create all tables in a database, and writes it to appropriate output.
     *
     * @param out The output (PrintStream or Connection) object to which the DML should be written.
     */
    protected abstract void createTables(Object out) throws SQLException;

    /**
     * Accepts the BibDatabase and MetaData, generates the DML required to create and populate SQL database tables,
     * and writes this DML to the specified output file.
     *
     * @param databaseContext the database to export
     * @param entriesToExport   The list of the entries to export.
     * @param file     The name of the file to which the DML should be written
     * @param encoding The encoding to be used
     */
    public void exportDatabaseAsFile(final BibDatabaseContext databaseContext,
            List<BibEntry> entriesToExport, String file, Charset encoding) throws Exception {
        // open output file
        File outfile = new File(file);
        if (outfile.exists() && !outfile.delete()) {
            LOGGER.warn("Cannot delete/overwrite file.");
            return;
        }
        try (BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(outfile));
                PrintStream fout = new PrintStream(writer)) {
            performExport(databaseContext, entriesToExport, fout, "file");
        }
    }

    /**
     * Accepts the BibDatabase and MetaData, generates the DML required to create and populate SQL database tables,
     * and writes this DML to the specified SQL database.
     *
     * @param databaseContext the database to export
     * @param entriesToExport The list of the entries to export.
     * @param databaseStrings The necessary database connection information
     */
    public void exportDatabaseToDBMS(final BibDatabaseContext databaseContext,
            List<BibEntry> entriesToExport, DBStrings databaseStrings, JabRefFrame frame) throws Exception {
        String dbName;
        Connection conn = null;
        boolean redisplay = false;
        try {
            conn = this.connectToDB(databaseStrings);
            createTables(conn);
            Vector<Vector<String>> matrix = createExistentDBNamesMatrix(databaseStrings);
            DBImportExportDialog dialogo = new DBImportExportDialog(frame, matrix,
                    DBImportExportDialog.DialogType.EXPORTER);
            if (dialogo.removeAction) {
                dbName = getDBName(matrix, databaseStrings, frame, dialogo);
                removeDB(dialogo, dbName, conn, databaseContext);
                redisplay = true;
            } else if (dialogo.hasDBSelected) {
                dbName = getDBName(matrix, databaseStrings, frame, dialogo);
                performExport(databaseContext, entriesToExport, conn, dbName);
            }
            if (!conn.getAutoCommit()) {
                conn.commit();
                conn.setAutoCommit(true);
            }
            if (redisplay) {
                exportDatabaseToDBMS(databaseContext, entriesToExport, databaseStrings, frame);
            }
        } catch (SQLException ex) {
            if ((conn != null) && !conn.getAutoCommit()) {
                conn.rollback();
            }
            throw ex;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    private String getDBName(Vector<Vector<String>> matrix, DBStrings databaseStrings, JabRefFrame frame,
            DBImportExportDialog dialogo) throws Exception {
        String dbName = "";
        if (matrix.size() > 1) {
            if (dialogo.hasDBSelected) {
                dbName = dialogo.selectedDB;
                if ((dialogo.selectedInt == 0) && (!dialogo.removeAction)) {
                    dbName = JOptionPane.showInputDialog(dialogo.getDiag(),
                            Localization.lang("Please enter the desired name:"), Localization.lang("SQL Export"),
                            JOptionPane.INFORMATION_MESSAGE);
                    if (dbName == null) {
                        getDBName(matrix, databaseStrings, frame,
                                new DBImportExportDialog(frame, matrix, DBImportExportDialog.DialogType.EXPORTER));
                    } else {
                        while (!isValidDBName(dbNames, dbName)) {
                            dbName = JOptionPane.showInputDialog(dialogo.getDiag(),
                                    Localization.lang("You have entered an invalid or already existent DB name.") + '\n'
                                            + Localization.lang("Please enter the desired name:"),
                                    Localization.lang("SQL Export"), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        } else {
            dbName = JOptionPane.showInputDialog(frame, Localization.lang("Please enter the desired name:"),
                    Localization.lang("SQL Export"), JOptionPane.INFORMATION_MESSAGE);
        }
        return dbName;
    }

    private Vector<Vector<String>> createExistentDBNamesMatrix(DBStrings databaseStrings) throws Exception {
        try (Connection conn = this.connectToDB(databaseStrings);
                Statement statement = SQLUtil.queryAllFromTable(conn, "jabref_database");
                ResultSet rs = statement.getResultSet()) {

            Vector<String> v;
            Vector<Vector<String>> matrix = new Vector<>();
            dbNames.clear();
            v = new Vector<>();
            v.add(Localization.lang("< CREATE NEW DATABASE >"));
            matrix.add(v);
            while (rs.next()) {
                v = new Vector<>();
                v.add(rs.getString("database_name"));
                matrix.add(v);
                dbNames.add(rs.getString("database_name"));
            }
            return matrix;
        }
    }

    private boolean isValidDBName(List<String> databaseNames, String desiredName) {
        return (desiredName != null) && (desiredName.trim().length() > 1) && !databaseNames.contains(desiredName);
    }

    /**
     * Returns a Jabref Database ID from the database in case the DB is already exported. In case the bib was already
     * exported before, the method returns the id, otherwise it calls the method that inserts a new row and returns the
     * ID for this new database
     *
     * @param metaData The MetaData object containing the database information
     * @param out The output (PrintStream or Connection) object to which the DML should be written.
     * @return The ID of database row of the jabref database being exported
     * @throws SQLException
     */
    /*
     * public int getDatabaseIDByPath(MetaData metaData, Object out, String
     * dbName) throws SQLException {
     *
     * if (out instanceof Connection) { Object response =
     * SQLUtil.processQueryWithResults(out,
     * "SELECT database_id FROM jabref_database WHERE md5_path=md5('" +
     * metaData.getDatabaseFile().getAbsolutePath() + "');"); ResultSet rs =
     * ((Statement) response).getResultSet(); if (rs.next()) return
     * rs.getInt("database_id"); else { insertJabRefDatabase(metaData, out,
     * dbName); return getDatabaseIDByPath(metaData, out, dbName); } } // in
     * case of text export there will be only 1 bib exported else {
     * insertJabRefDatabase(metaData, out, dbName); return 1; } }
     */

}
