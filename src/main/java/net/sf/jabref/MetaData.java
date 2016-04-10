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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.exporter.FieldFormatterCleanups;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.logic.config.SaveOrderConfig;
import net.sf.jabref.logic.labelpattern.AbstractLabelPattern;
import net.sf.jabref.logic.labelpattern.DatabaseLabelPattern;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.migrations.VersionHandling;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.sql.DBStrings;

public class MetaData implements Iterable<String> {
    private static final Log LOGGER = LogFactory.getLog(MetaData.class);

    public static final String META_FLAG = "jabref-meta: ";
    public static final String SAVE_ORDER_CONFIG = "saveOrderConfig";
    public static final String SAVE_ACTIONS = "saveActions";
    private static final String PREFIX_KEYPATTERN = "keypattern_";
    private static final String KEYPATTERNDEFAULT = "keypatterndefault";
    private static final String DATABASE_TYPE = "databaseType";
    private static final String GROUPSVERSION = "groupsversion";
    private static final String GROUPSTREE = "groupstree";
    private static final String GROUPS = "groups";
    private static final String FILE_DIRECTORY = Globals.FILE_FIELD + Globals.DIR_SUFFIX;

    private final Map<String, List<String>> metaData = new HashMap<>();
    private GroupTreeNode groupsRoot;

    private boolean groupTreeValid = true;

    private AbstractLabelPattern labelPattern;

    private DBStrings dbStrings = new DBStrings();


    /**
     * The MetaData object stores all meta data sets in Vectors. To ensure that
     * the data is written correctly to string, the user of a meta data Vector
     * must simply make sure the appropriate changes are reflected in the Vector
     * it has been passed.
     */
    public MetaData(Map<String, String> inData, BibDatabase db) {
        Objects.requireNonNull(inData);
        boolean groupsTreePresent = false;
        List<String> flatGroupsData = null;
        List<String> treeGroupsData = null;
        // The first version (0) lacked a version specification,
        // thus this value defaults to 0.
        int groupsVersionOnDisk = 0;

        for (Map.Entry<String, String> entry : inData.entrySet()) {
            StringReader data = new StringReader(entry.getValue());
            String unit;
            List<String> orderedData = new ArrayList<>();
            // We must allow for ; and \ in escape sequences.
            try {
                while ((unit = getNextUnit(data)) != null) {
                    orderedData.add(unit);
                }
            } catch (IOException ex) {
                LOGGER.error("Weird error while parsing meta data.", ex);
            }
            if (GROUPSVERSION.equals(entry.getKey())) {
                if (!orderedData.isEmpty()) {
                    groupsVersionOnDisk = Integer.parseInt(orderedData.get(0));
                }
            } else if (GROUPSTREE.equals(entry.getKey())) {
                groupsTreePresent = true;
                treeGroupsData = orderedData; // save for later user
                // actual import operation is handled later because "groupsversion"
                // tag might not yet have been read
            } else if (GROUPS.equals(entry.getKey())) {
                flatGroupsData = orderedData;
            } else {
                putData(entry.getKey(), orderedData);
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
    public MetaData() {
        // No data
    }

    public Optional<SaveOrderConfig> getSaveOrderConfig() {
        List<String> storedSaveOrderConfig = getData(SAVE_ORDER_CONFIG);
        if(storedSaveOrderConfig != null) {
            return Optional.of(new SaveOrderConfig(storedSaveOrderConfig));
        }
        return Optional.empty();
    }

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
    public List<String> getData(String key) {
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
    public void putData(String key, List<String> orderedData) {
        metaData.put(key, orderedData);
    }

    /**
     * Parse the groups metadata string
     *
     * @param orderedData The vector of metadata strings
     * @param db          The BibDatabase this metadata belongs to
     * @param version     The group tree version
     */
    private void putGroups(List<String> orderedData, BibDatabase db, int version) {
        try {
            groupsRoot = VersionHandling.importGroups(orderedData, db,
                    version);
            groupTreeValid = true;
        } catch (Exception e) {
            // we cannot really do anything about this here
            LOGGER.error("Problem parsing groups from MetaData", e);
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
                List<String> value = getData(key);
                String type = key.substring(MetaData.PREFIX_KEYPATTERN.length());
                labelPattern.addLabelPattern(type, value.get(0));
            }
        }
        List<String> defaultPattern = getData(MetaData.KEYPATTERNDEFAULT);
        if (defaultPattern != null) {
            labelPattern.setDefaultValue(defaultPattern.get(0));
        }

        return labelPattern;
    }

    /**
     * Updates the stored key patterns to the given key patterns.
     *
     * @param labelPattern the key patterns to update to. <br />
     *                     A reference to this object is stored internally and is returned at getLabelPattern();
     */
    public void setLabelPattern(AbstractLabelPattern labelPattern) {
        // remove all keypatterns from metadata
        Iterator<String> iterator = this.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (key.startsWith(MetaData.PREFIX_KEYPATTERN)) {
                iterator.remove();
            }
        }

        // set new value if it is not a default value
        Set<String> allKeys = labelPattern.getAllKeys();
        for (String key : allKeys) {
            String metaDataKey = MetaData.PREFIX_KEYPATTERN + key;
            if (!labelPattern.isDefaultValue(key)) {
                List<String> data = new ArrayList<>();
                data.add(labelPattern.getValue(key).get(0));
                this.putData(metaDataKey, data);
            }
        }

        // store default pattern
        if (labelPattern.getDefaultValue() == null) {
            this.remove(MetaData.KEYPATTERNDEFAULT);
        } else {
            List<String> data = new ArrayList<>();
            data.add(labelPattern.getDefaultValue().get(0));
            this.putData(MetaData.KEYPATTERNDEFAULT, data);
        }

        this.labelPattern = labelPattern;
    }

    public FieldFormatterCleanups getSaveActions() {
        if (this.getData(SAVE_ACTIONS) == null) {
            return new FieldFormatterCleanups(false, new ArrayList<>());
        } else {
            boolean enablementStatus = "enabled".equals(this.getData(SAVE_ACTIONS).get(0));
            String formatterString = this.getData(SAVE_ACTIONS).get(1);
            return new FieldFormatterCleanups(enablementStatus, formatterString);
        }
    }

    public Optional<BibDatabaseMode> getMode() {
        List<String> data = getData(MetaData.DATABASE_TYPE);
        if ((data == null) || data.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(BibDatabaseMode.valueOf(data.get(0).toUpperCase(Locale.ENGLISH)));
    }

    public boolean isProtected() {
        List<String> data = getData(Globals.PROTECTED_FLAG_META);
        if ((data == null) || data.isEmpty()) {
            return false;
        } else {
            return Boolean.parseBoolean(data.get(0));
        }
    }

    public List<String> getContentSelectors(String fieldName) {
        return getData(Globals.SELECTOR_META_PREFIX + fieldName);
    }

    public Optional<String> getDefaultFileDirectory() {
        List<String> fileDirectory = getData(FILE_DIRECTORY);
        if ((fileDirectory == null) || fileDirectory.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(fileDirectory.get(0).trim());
        }
    }

    public Optional<String> getUserFileDirectory(String user) {
        List<String> fileDirectory = getData(FILE_DIRECTORY + '-' + user);
        if ((fileDirectory == null) || fileDirectory.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(fileDirectory.get(0).trim());
        }
    }

    /**
     * Writes all data in the format <key, serialized data>.
     */
    public Map<String, String> serialize() {

        Map<String, String> serializedMetaData = new TreeMap<>();

        // first write all meta data except groups
        for (Map.Entry<String, List<String>> metaItem : metaData.entrySet()) {

            StringBuilder stringBuilder = new StringBuilder();
            for (String dataItem : metaItem.getValue()) {
                stringBuilder.append(StringUtil.quote(dataItem, ";", '\\')).append(";");

                //in case of save actions, add an additional newline after the enabled flag
                if (metaItem.getKey().equals(SAVE_ACTIONS) && "enabled".equals(dataItem)) {
                    stringBuilder.append(Globals.NEWLINE);
                }
            }

            String serializedItem = stringBuilder.toString();
            // Only add non-empty values
            if (!serializedItem.isEmpty() && !";".equals(serializedItem)) {
                serializedMetaData.put(metaItem.getKey(), serializedItem);
            }
        }

        // write groups if present. skip this if only the root node exists
        // (which is always the AllEntriesGroup).
        if ((groupsRoot != null) && (groupsRoot.getChildCount() > 0)) {

            // write version first
            serializedMetaData.put(MetaData.GROUPSVERSION, Integer.toString(VersionHandling.CURRENT_VERSION) + ";");

            // now write actual groups
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Globals.NEWLINE);
            // GroupsTreeNode.toString() uses "\n" for separation
            StringTokenizer tok = new StringTokenizer(groupsRoot.getTreeAsString(), Globals.NEWLINE);
            while (tok.hasMoreTokens()) {
                stringBuilder.append(StringUtil.quote(tok.nextToken(), ";", '\\'));
                stringBuilder.append(";");
                stringBuilder.append(Globals.NEWLINE);
            }
            serializedMetaData.put(MetaData.GROUPSTREE, stringBuilder.toString());
        }

        return serializedMetaData;
    }

    public void setSaveActions(FieldFormatterCleanups saveActions) {
        List<String> actionsSerialized = saveActions.convertToString();
        putData(SAVE_ACTIONS, actionsSerialized);
    }

    public void setSaveOrderConfig(SaveOrderConfig saveOrderConfig) {
        List<String> serialized = saveOrderConfig.getConfigurationList();
        putData(MetaData.SAVE_ORDER_CONFIG, serialized);
    }

    public void setMode(BibDatabaseMode mode) {
        putData(MetaData.DATABASE_TYPE, Collections.singletonList(mode.getFormattedName().toLowerCase(Locale.ENGLISH)));
    }

    public void markAsProtected() {
        putData(Globals.PROTECTED_FLAG_META, Collections.singletonList("true"));
    }

    public void setContentSelectors(String fieldName, List<String> contentSelectors) {
        putData(Globals.SELECTOR_META_PREFIX + fieldName, contentSelectors);
    }

    public void setDefaultFileDirectory(String path) {
        putData(FILE_DIRECTORY, Collections.singletonList(path));
    }

    public void clearDefaultFileDirectory() {
        remove(FILE_DIRECTORY);
    }

    public void setUserFileDirectory(String user, String path) {
        putData(FILE_DIRECTORY + '-' + user, Collections.singletonList(path));
    }
}
