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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import net.sf.jabref.exporter.FieldFormatterCleanups;
import net.sf.jabref.importer.fileformat.ParseException;
import net.sf.jabref.logic.config.SaveOrderConfig;
import net.sf.jabref.logic.groups.GroupTreeNode;
import net.sf.jabref.logic.groups.GroupsParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelpattern.AbstractLabelPattern;
import net.sf.jabref.logic.labelpattern.DatabaseLabelPattern;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.sql.DBStrings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MetaData implements Iterable<String> {

    private static final Log LOGGER = LogFactory.getLog(MetaData.class);
    public static final String META_FLAG = "jabref-meta: ";
    private static final String SAVE_ORDER_CONFIG = "saveOrderConfig";

    private static final String SAVE_ACTIONS = "saveActions";
    private static final String PREFIX_KEYPATTERN = "keypattern_";
    private static final String KEYPATTERNDEFAULT = "keypatterndefault";
    private static final String DATABASE_TYPE = "databaseType";

    private static final String GROUPSTREE = "groupstree";
    private static final String FILE_DIRECTORY = Globals.FILE_FIELD + Globals.DIR_SUFFIX;
    public static final String SELECTOR_META_PREFIX = "selector_";
    private static final String PROTECTED_FLAG_META = "protectedFlag";

    private final Map<String, List<String>> metaData = new HashMap<>();
    private GroupTreeNode groupsRoot;

    private AbstractLabelPattern labelPattern;

    private DBStrings dbStrings = new DBStrings();


    /**
     * The MetaData object stores all meta data sets in Vectors. To ensure that
     * the data is written correctly to string, the user of a meta data Vector
     * must simply make sure the appropriate changes are reflected in the Vector
     * it has been passed.
     */
    public MetaData(Map<String, String> inData) throws ParseException {
        Objects.requireNonNull(inData);

        for (Map.Entry<String, String> entry : inData.entrySet()) {
            StringReader data = new StringReader(entry.getValue());
            List<String> orderedData = new ArrayList<>();
            // We must allow for ; and \ in escape sequences.
            try {
                String unit;
                while ((unit = getNextUnit(data)) != null) {
                    orderedData.add(unit);
                }
            } catch (IOException ex) {
                LOGGER.error("Weird error while parsing meta data.", ex);
            }
            if (GROUPSTREE.equals(entry.getKey())) {
                putGroups(orderedData);
                // the keys "groupsversion" and "groups" were used in JabRef versions around 1.3, we will not use them here
            } else {
                putData(entry.getKey(), orderedData);
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
        metaData.put(SELECTOR_META_PREFIX + "keywords", new Vector<>());
        metaData.put(SELECTOR_META_PREFIX + "author", new Vector<>());
        metaData.put(SELECTOR_META_PREFIX + "journal", new Vector<>());
        metaData.put(SELECTOR_META_PREFIX + "publisher", new Vector<>());
        metaData.put(SELECTOR_META_PREFIX + "review", new Vector<>());
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
     */
    private void putGroups(List<String> orderedData) throws ParseException {
        try {
            groupsRoot = GroupsParser.importGroups(orderedData);
        } catch (ParseException e) {
            throw new ParseException(Localization.lang(
                    "Group tree could not be parsed. If you save the BibTeX database, all groups will be lost."), e);
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
            if (key.startsWith(PREFIX_KEYPATTERN)) {
                List<String> value = getData(key);
                String type = key.substring(PREFIX_KEYPATTERN.length());
                labelPattern.addLabelPattern(type, value.get(0));
            }
        }
        List<String> defaultPattern = getData(KEYPATTERNDEFAULT);
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
            if (key.startsWith(PREFIX_KEYPATTERN)) {
                iterator.remove();
            }
        }

        // set new value if it is not a default value
        Set<String> allKeys = labelPattern.getAllKeys();
        for (String key : allKeys) {
            String metaDataKey = PREFIX_KEYPATTERN + key;
            if (!labelPattern.isDefaultValue(key)) {
                List<String> data = new ArrayList<>();
                data.add(labelPattern.getValue(key).get(0));
                this.putData(metaDataKey, data);
            }
        }

        // store default pattern
        if (labelPattern.getDefaultValue() == null) {
            this.remove(KEYPATTERNDEFAULT);
        } else {
            List<String> data = new ArrayList<>();
            data.add(labelPattern.getDefaultValue().get(0));
            this.putData(KEYPATTERNDEFAULT, data);
        }

        this.labelPattern = labelPattern;
    }

    public Optional<FieldFormatterCleanups> getSaveActions() {
        if (this.getData(SAVE_ACTIONS) == null) {
            return Optional.empty();
        } else {
            boolean enablementStatus = "enabled".equals(this.getData(SAVE_ACTIONS).get(0));
            String formatterString = this.getData(SAVE_ACTIONS).get(1);
            return Optional.of(new FieldFormatterCleanups(enablementStatus, formatterString));
        }
    }

    public Optional<BibDatabaseMode> getMode() {
        List<String> data = getData(DATABASE_TYPE);
        if ((data == null) || data.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(BibDatabaseMode.valueOf(data.get(0).toUpperCase(Locale.ENGLISH)));
    }

    public boolean isProtected() {
        List<String> data = getData(PROTECTED_FLAG_META);
        if ((data == null) || data.isEmpty()) {
            return false;
        } else {
            return Boolean.parseBoolean(data.get(0));
        }
    }

    public List<String> getContentSelectors(String fieldName) {
        List<String> contentSelectors = getData(SELECTOR_META_PREFIX + fieldName);
        if(contentSelectors == null) {
            return Collections.emptyList();
        } else {
            return contentSelectors;
        }
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
        if ((groupsRoot != null) && (groupsRoot.getNumberOfChildren() > 0)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Globals.NEWLINE);

            for(String groupNode : groupsRoot.getTreeAsString()) {
                stringBuilder.append(StringUtil.quote(groupNode, ";", '\\'));
                stringBuilder.append(";");
                stringBuilder.append(Globals.NEWLINE);
            }
            serializedMetaData.put(GROUPSTREE, stringBuilder.toString());
        }

        return serializedMetaData;
    }

    public void setSaveActions(FieldFormatterCleanups saveActions) {
        List<String> actionsSerialized = saveActions.convertToString();
        putData(SAVE_ACTIONS, actionsSerialized);
    }

    public void setSaveOrderConfig(SaveOrderConfig saveOrderConfig) {
        List<String> serialized = saveOrderConfig.getConfigurationList();
        putData(SAVE_ORDER_CONFIG, serialized);
    }

    public void setMode(BibDatabaseMode mode) {
        putData(DATABASE_TYPE, Collections.singletonList(mode.getFormattedName().toLowerCase(Locale.ENGLISH)));
    }

    public void markAsProtected() {
        putData(PROTECTED_FLAG_META, Collections.singletonList("true"));
    }

    public void setContentSelectors(String fieldName, List<String> contentSelectors) {
        putData(SELECTOR_META_PREFIX + fieldName, contentSelectors);
    }

    public void setDefaultFileDirectory(String path) {
        putData(FILE_DIRECTORY, Collections.singletonList(path));
    }

    public void clearDefaultFileDirectory() {
        remove(FILE_DIRECTORY);
    }

    public void setUserFileDirectory(String user, String path) {
        putData(FILE_DIRECTORY + '-' + user, Collections.singletonList(path.trim()));
    }

    public void clearUserFileDirectory(String user) {
        remove(FILE_DIRECTORY + '-' + user);
    }

    public void clearContentSelectors(String fieldName) {
        remove(SELECTOR_META_PREFIX + fieldName);
    }

    public void markAsNotProtected() {
        remove(PROTECTED_FLAG_META);
    }

    public void clearSaveActions() {
        remove(SAVE_ACTIONS);
    }

    public void clearSaveOrderConfig() {
        remove(SAVE_ORDER_CONFIG);
    }
}
