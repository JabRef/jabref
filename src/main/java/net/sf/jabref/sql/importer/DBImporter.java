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
import java.util.*;

import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.model.entry.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.*;
import net.sf.jabref.groups.structure.*;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.sql.DBImporterExporter;
import net.sf.jabref.sql.DBStrings;
import net.sf.jabref.sql.SQLUtil;
import net.sf.jabref.logic.util.strings.StringUtil;

/**
 *
 * @author ifsteinm.
 *
 *         Jan 20th Abstract Class to provide main features to import entries from a DB. To insert a new DB it is
 *         necessary to extend this class and add the DB name the enum available at
 *         net.sf.jabref.sql.DBImporterAndExporterFactory (and to the GUI). This class and its subclasses import
 *         database, entries and related stuff from a DB to bib. Each exported database is imported as a new JabRef
 *         (bib) database, presented on a new tab
 *
 */
public abstract class DBImporter extends DBImporterExporter {

    private static final Log LOGGER = LogFactory.getLog(DBImporter.class);

    private final ArrayList<String> columnsNotConsideredForEntries = new ArrayList<>(
            Arrays.asList("cite_key", "entry_types_id", "database_id", "jabref_eid", "entries_id"));


    /**
     * Given a DBStrings it connects to the DB and returns the java.sql.Connection object
     *
     * @param dbstrings The DBStrings to use to make the connection
     * @return java.sql.Connection to the DB chosen
     * @throws Exception
     */
    protected abstract Connection connectToDB(DBStrings dbstrings) throws Exception;

    /**
     *
     * @param conn Connection object to the database
     * @return A ResultSet with column name for the entries table
     * @throws SQLException
     */
    protected abstract ResultSet readColumnNames(Connection conn) throws SQLException;

    /**
     * Worker method to perform the import from a database
     *
     * @param dbs The necessary database connection information
     * @return An ArrayList containing pairs of Objects. Each position of the ArrayList stores three Objects: a
     *         BibtexDatabase, a MetaData and a String with the bib database name stored in the DBMS
     * @throws Exception
     */
    public ArrayList<Object[]> performImport(DBStrings dbs, List<String> listOfDBs) throws Exception {
        ArrayList<Object[]> result = new ArrayList<>();
        try (Connection conn = this.connectToDB(dbs)) {

            Iterator<String> itLista = listOfDBs.iterator();
            String jabrefDBs = "(";
            while (itLista.hasNext()) {
                jabrefDBs += '\'' + itLista.next() + "',";
            }
            jabrefDBs = jabrefDBs.substring(0, jabrefDBs.length() - 1) + ')';

            try (ResultSet rsDatabase = SQLUtil.queryAllFromTable(conn,
                    "jabref_database WHERE database_name IN " + jabrefDBs)) {
                while (rsDatabase.next()) {
                    BibtexDatabase database = new BibtexDatabase();
                    // Find entry type IDs and their mappings to type names:
                    HashMap<String, EntryType> types = new HashMap<>();
                    try (ResultSet rsEntryType = SQLUtil.queryAllFromTable(conn, "entry_types")) {
                        while (rsEntryType.next()) {
                            types.put(rsEntryType.getString("entry_types_id"),
                                    EntryTypes.getType(rsEntryType.getString("label")));
                        }
                        rsEntryType.getStatement().close();
                    }
                    try (ResultSet rsColumns = this.readColumnNames(conn)) {
                        ArrayList<String> colNames = new ArrayList<>();
                        while (rsColumns.next()) {
                            if (!columnsNotConsideredForEntries.contains(rsColumns.getString(1))) {
                                colNames.add(rsColumns.getString(1));
                            }
                        }
                        rsColumns.getStatement().close();

                        String database_id = rsDatabase.getString("database_id");
                        // Read the entries and create BibtexEntry instances:
                        HashMap<String, BibtexEntry> entries = new HashMap<>();
                        try (ResultSet rsEntries = SQLUtil.queryAllFromTable(conn,
                                "entries WHERE database_id= '" + database_id + "';")) {
                            while (rsEntries.next()) {
                                String id = rsEntries.getString("entries_id");
                                BibtexEntry entry = new BibtexEntry(IdGenerator.next(),
                                        types.get(rsEntries.getString("entry_types_id")));
                                entry.setField(BibtexEntry.KEY_FIELD, rsEntries.getString("cite_key"));
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
                            rsEntries.getStatement().close();
                        }
                        // Import strings and preamble:
                        try (ResultSet rsStrings = SQLUtil.queryAllFromTable(conn,
                                "strings WHERE database_id='" + database_id + '\'')) {
                            while (rsStrings.next()) {
                                String label = rsStrings.getString("label");
                                String content = rsStrings.getString("content");
                                if (label.equals("@PREAMBLE")) {
                                    database.setPreamble(content);
                                } else {
                                    BibtexString string = new BibtexString(IdGenerator.next(), label, content);
                                    database.addString(string);
                                }
                            }
                            rsStrings.getStatement().close();
                        }
                        MetaData metaData = new MetaData();
                        metaData.initializeNewDatabase();
                        // Read the groups tree:
                        importGroupsTree(metaData, entries, conn, database_id);
                        result.add(new Object[] {database, metaData, rsDatabase.getString("database_name")});
                    }
                }
                rsDatabase.close();
            }
            conn.close();
            return result;
        }
    }

    /**
     * Look up the group type name from the type ID in the database.
     *
     * @param groupId The database's groups id
     * @param conn The database connection
     *
     * @return The name (JabRef type id) of the group type.
     * @throws SQLException
     */
    private String findGroupTypeName(String groupId, Connection conn) throws SQLException {
        return SQLUtil.processQueryWithSingleResult(conn,
                "SELECT label FROM group_types WHERE group_types_id='" + groupId + "';");
    }

    private void importGroupsTree(MetaData metaData, HashMap<String, BibtexEntry> entries, Connection conn,
            String database_id) throws SQLException {
        HashMap<String, GroupTreeNode> groups = new HashMap<>();
        LinkedHashMap<GroupTreeNode, String> parentIds = new LinkedHashMap<>();
        GroupTreeNode rootNode = new GroupTreeNode(new AllEntriesGroup());

        try (ResultSet rsGroups = SQLUtil.queryAllFromTable(conn,
                "groups WHERE database_id='" + database_id + "' ORDER BY groups_id")) {
            while (rsGroups.next()) {
                AbstractGroup group = null;
                String typeId = findGroupTypeName(rsGroups.getString("group_types_id"), conn);
                if (typeId.equals(AllEntriesGroup.ID)) {
                    // register the id of the root node:
                    groups.put(rsGroups.getString("groups_id"), rootNode);
                } else if (typeId.equals(ExplicitGroup.ID)) {
                    group = new ExplicitGroup(rsGroups.getString("label"),
                            GroupHierarchyType.getByNumber(rsGroups.getInt("hierarchical_context")));
                } else if (typeId.equals(KeywordGroup.ID)) {
                    LOGGER.debug("Keyw: " + rsGroups.getBoolean("case_sensitive"));
                    group = new KeywordGroup(rsGroups.getString("label"),
                            StringUtil.unquote(rsGroups.getString("search_field"), '\\'),
                            StringUtil.unquote(rsGroups.getString("search_expression"), '\\'),
                            rsGroups.getBoolean("case_sensitive"), rsGroups.getBoolean("reg_exp"),
                            GroupHierarchyType.getByNumber(rsGroups.getInt("hierarchical_context")));
                } else if (typeId.equals(SearchGroup.ID)) {
                    LOGGER.debug("Search: " + rsGroups.getBoolean("case_sensitive"));
                    group = new SearchGroup(rsGroups.getString("label"),
                            StringUtil.unquote(rsGroups.getString("search_expression"), '\\'),
                            rsGroups.getBoolean("case_sensitive"), rsGroups.getBoolean("reg_exp"),
                            GroupHierarchyType.getByNumber(rsGroups.getInt("hierarchical_context")));
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
                        parent.add(groupTreeNodeStringEntry.getKey());
                    }
                }

                try (ResultSet rsEntryGroup = SQLUtil.queryAllFromTable(conn, "entry_group")) {
                    while (rsEntryGroup.next()) {
                        String entryId = rsEntryGroup.getString("entries_id");
                        String groupId = rsEntryGroup.getString("groups_id");
                        GroupTreeNode node = groups.get(groupId);
                        if ((node != null) && (node.getGroup() instanceof ExplicitGroup)) {
                            ExplicitGroup expGroup = (ExplicitGroup) node.getGroup();
                            expGroup.addEntry(entries.get(entryId));
                        }
                    }
                    rsEntryGroup.getStatement().close();
                }
                metaData.setGroups(rootNode);
            }
            rsGroups.getStatement().close();
        }
    }
}
