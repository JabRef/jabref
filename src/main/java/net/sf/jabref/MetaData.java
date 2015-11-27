/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref;

import java.io.*;
import java.util.*;

import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.migrations.VersionHandling;
import net.sf.jabref.logic.labelPattern.AbstractLabelPattern;
import net.sf.jabref.logic.labelPattern.DatabaseLabelPattern;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.sql.DBStrings;
import net.sf.jabref.logic.util.strings.StringUtil;

public class MetaData implements Iterable<String> {

    private static final String PREFIX_KEYPATTERN = "keypattern_";
    private static final String KEYPATTERNDEFAULT = "keypatterndefault";

    private final HashMap<String, Vector<String>> metaData = new HashMap<>();
    private GroupTreeNode groupsRoot;
    private File file; // The File where this base gets saved.
    private boolean groupTreeValid = true;

    private AbstractLabelPattern labelPattern;

    private DBStrings dbStrings = new DBStrings();


    /**
     * The MetaData object stores all meta data sets in Vectors. To ensure that
     * the data is written correctly to string, the user of a meta data Vector
     * must simply make sure the appropriate changes are reflected in the Vector
     * it has been passed.
     */
    public MetaData(HashMap<String, String> inData, BibtexDatabase db) {
        boolean groupsTreePresent = false;
        Vector<String> flatGroupsData = null;
        Vector<String> treeGroupsData = null;
        // The first version (0) lacked a version specification,
        // thus this value defaults to 0.
        int groupsVersionOnDisk = 0;

        if (inData != null) {
            for (String key : inData.keySet()) {
                StringReader data = new StringReader(inData.get(key));
                String unit;
                Vector<String> orderedData = new Vector<>();
                // We must allow for ; and \ in escape sequences.
                try {
                    while ((unit = getNextUnit(data)) != null) {
                        orderedData.add(unit);
                    }
                } catch (IOException ex) {
                    System.err.println("Weird error while parsing meta data.");
                }
                if ("groupsversion".equals(key)) {
                    if (orderedData.size() >= 1) {
                        groupsVersionOnDisk = Integer.parseInt(orderedData.firstElement());
                    }
                } else if ("groupstree".equals(key)) {
                    groupsTreePresent = true;
                    treeGroupsData = orderedData; // save for later user
                    // actual import operation is handled later because "groupsversion"
                    // tag might not yet have been read
                } else if ("groups".equals(key)) {
                    flatGroupsData = orderedData;
                } else {
                    putData(key, orderedData);
                }
            }
        }

        // this possibly handles import of a previous groups version
        if (groupsTreePresent) {
            putGroups(treeGroupsData, db, groupsVersionOnDisk);
        }

        if (!groupsTreePresent && (flatGroupsData != null)) {
            try {
                groupsRoot = VersionHandling.importFlatGroups(flatGroupsData);
                groupTreeValid = true;
            } catch (IllegalArgumentException ex) {
                groupTreeValid = true;
            }
        }
    }

    /**
     * The MetaData object can be constructed with no data in it.
     */
    public MetaData() {}

    /**
     * Add default metadata for new database:
     */
    public void initializeNewDatabase() {
        metaData.put(Globals.SELECTOR_META_PREFIX + "keywords", new Vector<>());
        metaData.put(Globals.SELECTOR_META_PREFIX + "author", new Vector<>());
        metaData.put(Globals.SELECTOR_META_PREFIX + "journal", new Vector<>());
        metaData.put(Globals.SELECTOR_META_PREFIX + "publisher", new Vector<>());
        metaData.put(Globals.SELECTOR_META_PREFIX + "review", new Vector<>());
    }

    /**
     * @return Iterator on all keys stored in the metadata
     */
    @Override
    public Iterator<String> iterator() {
        return metaData.keySet().iterator();
    }

    /**
     * Retrieves the stored meta data.
     *
     * @param key the key to look up
     * @return null if no data is found
     */
    public Vector<String> getData(String key) {
        return metaData.get(key);
    }

    /**
     * Removes the given key from metadata.
     * Nothing is done if key is not found.
     *
     * @param key the key to remove
     */
    public void remove(String key) {
        metaData.remove(key);
    }

    /**
     * Stores the specified data in this object, using the specified key. For
     * certain keys (e.g. "groupstree"), the objects in orderedData are
     * reconstructed from their textual (String) representation if they are of
     * type String, and stored as an actual instance.
     */
    public void putData(String key, Vector<String> orderedData) {
        metaData.put(key, orderedData);
    }

    /**
     * Look up the directory set up for the given field type for this database.
     * If no directory is set up, return that defined in global preferences.
     * @param fieldName The field type
     * @return The default directory for this field type.
     */
    public String[] getFileDirectory(String fieldName) {
        // There can be up to three directory definitions for these files - the database's
        // metadata can specify a general directory and/or a user-specific directory, or
        // the preferences can specify one. The settings are prioritized in the following
        // order and the first defined setting is used: metadata user-specific directory,
        // metadata general directory, preferences directory.
        String key = Globals.prefs.get(JabRefPreferences.USER_FILE_DIR_INDIVIDUAL);
        List<String> dirs = new ArrayList<>();

        Vector<String> vec = getData(key);
        if (vec == null) {
            key = Globals.prefs.get(JabRefPreferences.USER_FILE_DIR);
            vec = getData(key);
        }
        if ((vec != null) && !vec.isEmpty()) {
            String dir;
            dir = vec.get(0);
            // If this directory is relative, we try to interpret it as relative to
            // the file path of this bib file:
            if (!new File(dir).isAbsolute() && (file != null)) {
                String relDir;
                if (".".equals(dir)) {
                    // if dir is only "current" directory, just use its parent (== real current directory) as path
                    relDir = file.getParent();
                } else {
                    relDir = file.getParent() + System.getProperty("file.separator") + dir;
                }
                // If this directory actually exists, it is very likely that the
                // user wants us to use it:
                if (new File(relDir).exists()) {
                    dir = relDir;
                }
            }
            dirs.add(dir);
        }
        else {
            String dir = Globals.prefs.get(fieldName + "Directory");
            if (dir != null) {
                dirs.add(dir);
            }
        }

        // Check if the bib file location should be included, and if so, if it is set:
        if (Globals.prefs.getBoolean(JabRefPreferences.BIB_LOCATION_AS_FILE_DIR) && (getFile() != null)) {
            // Check if we should add it as primary file dir (first in the list) or not:
            if (Globals.prefs.getBoolean(JabRefPreferences.BIB_LOC_AS_PRIMARY_DIR)) {
                dirs.add(0, getFile().getParent());
            } else {
                dirs.add(getFile().getParent());
            }
        }

        return dirs.toArray(new String[dirs.size()]);
    }

    /**
     * Parse the groups metadata string
     * @param orderedData The vector of metadata strings
     * @param db The BibtexDatabase this metadata belongs to
     * @param version The group tree version
     * @return true if parsing was successful, false otherwise
     */
    private void putGroups(Vector<String> orderedData, BibtexDatabase db, int version) {
        try {
            groupsRoot = VersionHandling.importGroups(orderedData, db,
                    version);
            groupTreeValid = true;
        } catch (Exception e) {
            // we cannot really do anything about this here
            System.err.println(e);
            groupTreeValid = false;
        }
    }

    public GroupTreeNode getGroups() {
        return groupsRoot;
    }

    /**
     * Sets a new group root node. <b>WARNING </b>: This invalidates everything
     * returned by getGroups() so far!!!
     */
    public void setGroups(GroupTreeNode root) {
        groupsRoot = root;
        groupTreeValid = true;
    }

    /**
     * Writes all data to the specified writer, using each object's toString()
     * method.
     */
    public void writeMetaData(Writer out) throws IOException {
        // write all meta data except groups
        SortedSet<String> sortedKeys = new TreeSet<>(metaData.keySet());
        for (String key : sortedKeys) {
            StringBuffer sb = new StringBuffer();
            Vector<String> orderedData = metaData.get(key);
            sb.append("@comment{").append(GUIGlobals.META_FLAG).append(key).append(":");
            for (int j = 0; j < orderedData.size(); j++) {
                sb.append(StringUtil.quote(orderedData.elementAt(j), ";", '\\')).append(";");
            }
            sb.append("}");
            sb.append(Globals.NEWLINE);
            sb.append(Globals.NEWLINE);

            out.write(sb.toString());
        }
        // write groups if present. skip this if only the root node exists
        // (which is always the AllEntriesGroup).
        if ((groupsRoot != null) && (groupsRoot.getChildCount() > 0)) {
            StringBuffer sb = new StringBuffer();
            // write version first
            sb.append("@comment{").append(GUIGlobals.META_FLAG).append("groupsversion:");
            sb.append("" + VersionHandling.CURRENT_VERSION + ";");
            sb.append("}");
            sb.append(Globals.NEWLINE);
            sb.append(Globals.NEWLINE);
            out.write(sb.toString());

            // now write actual groups
            sb = new StringBuffer();
            sb.append("@comment{").append(GUIGlobals.META_FLAG).append("groupstree:");
            sb.append(Globals.NEWLINE);
            // GroupsTreeNode.toString() uses "\n" for separation
            StringTokenizer tok = new StringTokenizer(groupsRoot.getTreeAsString(), Globals.NEWLINE);
            while (tok.hasMoreTokens()) {
                StringBuffer s =
                        new StringBuffer(StringUtil.quote(tok.nextToken(), ";", '\\') + ";");
                sb.append(s);
                sb.append(Globals.NEWLINE);
            }
            sb.append("}");
            sb.append(Globals.NEWLINE);
            sb.append(Globals.NEWLINE);
            out.write(sb.toString());
        }
    }

    /**
     * Reads the next unit. Units are delimited by ';'.
     */
    private static String getNextUnit(Reader reader) throws IOException {
        int c;
        boolean escape = false;
        StringBuilder res = new StringBuilder();
        while ((c = reader.read()) != -1) {
            if (escape) {
                res.append((char) c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == ';') {
                break;
            } else {
                res.append((char) c);
            }
        }
        if (res.length() > 0) {
            return res.toString();
        }
        return null;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public DBStrings getDBStrings() {
        return dbStrings;
    }

    public void setDBStrings(DBStrings dbStrings) {
        this.dbStrings = dbStrings;
    }

    public boolean isGroupTreeValid() {
        return groupTreeValid;
    }

    /**
     * @return the stored label patterns
     */
    public AbstractLabelPattern getLabelPattern() {
        if (labelPattern != null) {
            return labelPattern;
        }

        labelPattern = new DatabaseLabelPattern();

        // read the data from the metadata and store it into the labelPattern
        for (String key : this) {
            if (key.startsWith(MetaData.PREFIX_KEYPATTERN)) {
                Vector<String> value = getData(key);
                String type = key.substring(MetaData.PREFIX_KEYPATTERN.length());
                labelPattern.addLabelPattern(type, value.get(0));
            }
        }
        Vector<String> defaultPattern = getData(MetaData.KEYPATTERNDEFAULT);
        if (defaultPattern != null) {
            labelPattern.setDefaultValue(defaultPattern.get(0));
        }

        return labelPattern;
    }

    /**
     * Updates the stored key patterns to the given key patterns.
     *
     * @param labelPattern the key patterns to update to. <br />
     * A reference to this object is stored internally and is returned at getLabelPattern();
     */
    public void setLabelPattern(DatabaseLabelPattern labelPattern) {
        // remove all keypatterns from metadata
        Iterator<String> iterator = this.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (key.startsWith(MetaData.PREFIX_KEYPATTERN)) {
                iterator.remove();
            }
        }

        // set new value if it is not a default value
        Enumeration<String> allKeys = labelPattern.getAllKeys();
        while (allKeys.hasMoreElements()) {
            String key = allKeys.nextElement();
            String metaDataKey = MetaData.PREFIX_KEYPATTERN + key;
            if (!labelPattern.isDefaultValue(key)) {
                ArrayList<String> value = labelPattern.getValue(key);
                Vector<String> data = new Vector<>();
                data.add(value.get(0));
                this.putData(metaDataKey, data);
            }
        }

        // store default pattern
        if (labelPattern.getDefaultValue() == null) {
            this.remove(MetaData.KEYPATTERNDEFAULT);
        } else {
            Vector<String> data = new Vector<>();
            data.add(labelPattern.getDefaultValue().get(0));
            this.putData(MetaData.KEYPATTERNDEFAULT, data);
        }

        this.labelPattern = labelPattern;
    }

}
