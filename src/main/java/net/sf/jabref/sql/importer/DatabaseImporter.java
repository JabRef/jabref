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

package net.sf.jabref.sql.importer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import net.sf.jabref.MetaData;
import net.sf.jabref.importer.fileformat.ParseException;
import net.sf.jabref.logic.groups.AbstractGroup;
import net.sf.jabref.logic.groups.AllEntriesGroup;
import net.sf.jabref.logic.groups.ExplicitGroup;
import net.sf.jabref.logic.groups.GroupHierarchyType;
import net.sf.jabref.logic.groups.GroupTreeNode;
import net.sf.jabref.logic.groups.KeywordGroup;
import net.sf.jabref.logic.groups.SearchGroup;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.sql.DBStrings;
import net.sf.jabref.sql.Database;
import net.sf.jabref.sql.SQLUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author ifsteinm.
 *         <p>
 *         Jan 20th Abstract Class to provide main features to import entries from a DB. To insert a new DB it is
 *         necessary to extend this class and add the DB name the enum available at
 *         net.sf.jabref.sql.DBImporterAndExporterFactory (and to the GUI). This class and its subclasses import
 *         database, entries and related stuff from a DB to bib. Each exported database is imported as a new JabRef
 *         (bib) database, presented on a new tab
 */
public class DatabaseImporter {

    private static final Log LOGGER = LogFactory.getLog(DatabaseImporter.class);

    private static final List<String> COLUMNS_NOT_CONSIDERED_FOR_ENTRIES = Arrays.asList(
            "cite_key",
            "entry_types_id",
            "database_id",
            "jabref_eid",
            "entries_id"
    );


    private final Database database;

    public DatabaseImporter(Database database) {
        this.database = database;
    }

    /**
     * @param conn Connection object to the database
     * @return A ResultSet with column name for the entries table
     * @throws SQLException
     */
    private List<String> readColumnNames(Connection conn) throws SQLException {
        String query = database.getReadColumnNamesQuery();
        try (Statement statement = conn.createStatement();
             ResultSet rsColumns = statement.executeQuery(query)) {
            List<String> colNames = new ArrayList<>();
            while (rsColumns.next()) {
                colNames.add(rsColumns.getString(1));
            }
            return colNames;
        }
    }

    /**
     * Worker method to perform the import from a database
     *
     * @param dbs  The necessary database connection information
     * @param mode
     * @return An ArrayList containing pairs of Objects. Each position of the ArrayList stores three Objects: a
     * BibDatabase, a MetaData and a String with the bib database name stored in the DBMS
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws Exception
     */
    public List<DBImporterResult> performImport(DBStrings dbs, List<String> listOfDBs, BibDatabaseMode mode)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException, SQLException
    {
        List<DBImporterResult> result = new ArrayList<>();
        try (Connection conn = this.connectToDB(dbs)) {

            Iterator<String> itLista = listOfDBs.iterator();
            StringJoiner stringJoiner = new StringJoiner(",", "(", ")");

            while (itLista.hasNext()) {
                stringJoiner.add("'" + itLista.next() + "'");
            }

            String query = SQLUtil.queryAllFromTable(
                    "jabref_database WHERE database_name IN " + stringJoiner.toString());
            try (Statement statement = conn.createStatement();
                 ResultSet rsDatabase = statement.executeQuery(query)) {
                while (rsDatabase.next()) {
                    BibDatabase database = new BibDatabase();
                    // Find entry type IDs and their mappings to type names:
                    HashMap<String, EntryType> types = new HashMap<>();
                    try (Statement entryTypes = conn.createStatement();
                            ResultSet rsEntryType = entryTypes.executeQuery(SQLUtil.queryAllFromTable("entry_types"))) {
                        while (rsEntryType.next()) {
                            Optional<EntryType> entryType = EntryTypes.getType(rsEntryType.getString("label"), mode);
                            if (entryType.isPresent()) {
                                types.put(rsEntryType.getString("entry_types_id"), entryType.get());
                            }
                        }
                    }

                    List<String> colNames = this.readColumnNames(conn).stream().filter(column -> !COLUMNS_NOT_CONSIDERED_FOR_ENTRIES.contains(column)).collect(Collectors.toList());

                    final String database_id = rsDatabase.getString("database_id");
                    // Read the entries and create BibEntry instances:
                    HashMap<String, BibEntry> entries = new HashMap<>();
                    try (Statement entryStatement = conn.createStatement();
                            ResultSet rsEntries = entryStatement.executeQuery(SQLUtil.queryAllFromTable(
                                    "entries WHERE database_id= '" + database_id + "';"))) {
                        while (rsEntries.next()) {
                            String id = rsEntries.getString("entries_id");
                            BibEntry entry = new BibEntry(IdGenerator.next(), types.get(rsEntries.getString("entry_types_id")).getName());
                            entry.setCiteKey(rsEntries.getString("cite_key"));
                            for (String col : colNames) {
                                String value = rsEntries.getString(col);
                                if (value != null) {
                                    col = col.charAt(col.length() - 1) == '_' ? col.substring(0,
                                            col.length() - 1) : col;
                                    entry.setField(col, value);
                                }
                            }
                            entries.put(id, entry);
                            database.insertEntry(entry);
                        }
                    }
                    // Import strings and preamble:
                    try (Statement stringStatement = conn.createStatement();
                            ResultSet rsStrings = stringStatement.executeQuery(SQLUtil.queryAllFromTable(
                                    "strings WHERE database_id='" + database_id + '\''))) {
                        while (rsStrings.next()) {
                            String label = rsStrings.getString("label");
                            String content = rsStrings.getString("content");
                            if ("@PREAMBLE".equals(label)) {
                                database.setPreamble(content);
                            } else {
                                BibtexString string = new BibtexString(IdGenerator.next(), label, content);
                                database.addString(string);
                            }
                        }
                    }
                    MetaData metaData = new MetaData();
                    metaData.initializeNewDatabase();
                    // Read the groups tree:
                    importGroupsTree(metaData, entries, conn, database_id);
                    result.add(new DBImporterResult(database, metaData, rsDatabase.getString("database_name")));
                }
            }
        }

        return result;
    }

    /**
     * Look up the group type name from the type ID in the database.
     *
     * @param groupId The database's groups id
     * @param conn    The database connection
     * @return The name (JabRef type id) of the group type.
     * @throws SQLException
     */
    private String findGroupTypeName(String groupId, Connection conn) throws SQLException {
        return SQLUtil.processQueryWithSingleResult(conn,
                "SELECT label FROM group_types WHERE group_types_id='" + groupId + "';");
    }

    private void importGroupsTree(MetaData metaData, Map<String, BibEntry> entries, Connection conn,
            final String database_id) throws SQLException {
        Map<String, GroupTreeNode> groups = new HashMap<>();
        LinkedHashMap<GroupTreeNode, String> parentIds = new LinkedHashMap<>();
        GroupTreeNode rootNode = new GroupTreeNode(new AllEntriesGroup());

        String query = SQLUtil.queryAllFromTable("groups WHERE database_id='" + database_id + "' ORDER BY groups_id");
        try (Statement statement = conn.createStatement();
             ResultSet rsGroups = statement.executeQuery(query)) {
            while (rsGroups.next()) {
                AbstractGroup group = null;
                String typeId = findGroupTypeName(rsGroups.getString("group_types_id"), conn);
                try {
                    switch (typeId) {
                    case AllEntriesGroup.ID:
                        // register the id of the root node:
                        groups.put(rsGroups.getString("groups_id"), rootNode);
                        break;
                    case ExplicitGroup.ID:
                        group = new ExplicitGroup(rsGroups.getString("label"),
                                GroupHierarchyType.getByNumber(rsGroups.getInt("hierarchical_context")));
                        break;
                    case KeywordGroup.ID:
                        LOGGER.debug("Keyw: " + rsGroups.getBoolean("case_sensitive"));
                        group = new KeywordGroup(rsGroups.getString("label"),
                                StringUtil.unquote(rsGroups.getString("search_field"), '\\'),
                                StringUtil.unquote(rsGroups.getString("search_expression"), '\\'),
                                rsGroups.getBoolean("case_sensitive"), rsGroups.getBoolean("reg_exp"),
                                GroupHierarchyType.getByNumber(rsGroups.getInt("hierarchical_context")));
                        break;
                    case SearchGroup.ID:
                        LOGGER.debug("Search: " + rsGroups.getBoolean("case_sensitive"));
                        group = new SearchGroup(rsGroups.getString("label"),
                                StringUtil.unquote(rsGroups.getString("search_expression"), '\\'),
                                rsGroups.getBoolean("case_sensitive"), rsGroups.getBoolean("reg_exp"),
                                GroupHierarchyType.getByNumber(rsGroups.getInt("hierarchical_context")));
                        break;
                    }
                } catch (ParseException e) {
                    LOGGER.error(e);
                }

                if (group != null) {
                    GroupTreeNode node = new GroupTreeNode(group);
                    parentIds.put(node, rsGroups.getString("parent_id"));
                    groups.put(rsGroups.getString("groups_id"), node);
                }

                // Ok, we have collected a map of all groups and their parent IDs,
                // and another map of all group IDs and their group nodes.
                // Now we need to build the groups tree:
                for (Map.Entry<GroupTreeNode, String> groupTreeNodeStringEntry : parentIds.entrySet()) {
                    String parentId = groupTreeNodeStringEntry.getValue();
                    GroupTreeNode parent = groups.get(parentId);
                    if (parent == null) {
                        // TODO: missing parent
                    } else {
                        groupTreeNodeStringEntry.getKey().moveTo(parent);
                    }
                }

                try (Statement entryGroup = conn.createStatement();
                        ResultSet rsEntryGroup = entryGroup.executeQuery(SQLUtil.queryAllFromTable("entry_group"))) {
                    while (rsEntryGroup.next()) {
                        String entryId = rsEntryGroup.getString("entries_id");
                        String groupId = rsEntryGroup.getString("groups_id");
                        GroupTreeNode node = groups.get(groupId);
                        if ((node != null) && (node.getGroup() instanceof ExplicitGroup)) {
                            ExplicitGroup expGroup = (ExplicitGroup) node.getGroup();
                            expGroup.add(entries.get(entryId));
                        }
                    }
                }
                metaData.setGroups(rootNode);
            }
        }
    }

    /**
     * Given a DBStrings it connects to the DB and returns the java.sql.Connection object
     *
     * @param dbstrings The DBStrings to use to make the connection
     * @return java.sql.Connection to the DB chosen
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public Connection connectToDB(DBStrings dbstrings)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException, SQLException {
        String url = SQLUtil.createJDBCurl(dbstrings, true);
        return database.connect(url, dbstrings.getDbPreferences().getUsername(), dbstrings.getPassword());
    }

}
