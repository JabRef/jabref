package net.sf.jabref;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import net.sf.jabref.event.GroupUpdatedEvent;
import net.sf.jabref.event.MetaDataChangedEvent;
import net.sf.jabref.logic.config.SaveOrderConfig;
import net.sf.jabref.logic.exporter.FieldFormatterCleanups;
import net.sf.jabref.logic.groups.GroupTreeNode;
import net.sf.jabref.logic.importer.util.ParseException;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.layout.format.FileLinkPreferences;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import net.sf.jabref.model.bibtexkeypattern.DatabaseBibtexKeyPattern;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.FieldName;

import com.google.common.eventbus.EventBus;
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
    private static final String FILE_DIRECTORY = FieldName.FILE + FileLinkPreferences.DIR_SUFFIX;
    public static final String SELECTOR_META_PREFIX = "selector_";
    private static final String PROTECTED_FLAG_META = "protectedFlag";

    private final Map<String, List<String>> metaData = new HashMap<>();
    private GroupTreeNode groupsRoot;
    private final EventBus eventBus = new EventBus();

    private AbstractBibtexKeyPattern bibtexKeyPattern;

    private Charset encoding;

    /**
     * The MetaData object stores all meta data sets in Vectors. To ensure that
     * the data is written correctly to string, the user of a meta data Vector
     * must simply make sure the appropriate changes are reflected in the Vector
     * it has been passed.
     */
    private MetaData(Map<String, String> inData) throws ParseException {
        Objects.requireNonNull(inData);
        setData(inData);
    }
    private MetaData(Map<String, String> inData, Charset encoding) throws ParseException {
        this(inData);
        this.encoding = Objects.requireNonNull(encoding);
    }

    /**
     * The MetaData object can be constructed with no data in it.
     */
    public MetaData() {
        // Do nothing
    }

    public MetaData(Charset encoding) {
        this.encoding = encoding;
    }

    public static MetaData parse(Map<String, String> data) throws ParseException {
        return new MetaData(data);
    }

    public static MetaData parse(Map<String, String> data, Charset encoding) throws ParseException {
        return new MetaData(data, encoding);
    }

    public void setData(Map<String, String> inData) throws ParseException {
        clearMetaData();
        for (Map.Entry<String, String> entry : inData.entrySet()) {
            StringReader data = new StringReader(entry.getValue());
            List<String> orderedData = new ArrayList<>();
            // We must allow for ; and \ in escape sequences.
            try {
                Optional<String> unit;
                while ((unit = getNextUnit(data)).isPresent()) {
                    orderedData.add(unit.get());
                }
            } catch (IOException ex) {
                LOGGER.error("Weird error while parsing meta data.", ex);
            }
            if (GROUPSTREE.equals(entry.getKey())) {
                putGroups(orderedData);
                // the keys "groupsversion" and "groups" were used in JabRef versions around 1.3, we will not support them anymore
                eventBus.post(new GroupUpdatedEvent(this));
            } else if (SAVE_ACTIONS.equals(entry.getKey())) {
                metaData.put(SAVE_ACTIONS, FieldFormatterCleanups.parse(orderedData).getAsStringList()); // Without MetaDataChangedEvent
            } else {
                metaData.put(entry.getKey(), orderedData);
            }
        }
    }

    public Optional<SaveOrderConfig> getSaveOrderConfig() {
        List<String> storedSaveOrderConfig = getData(SAVE_ORDER_CONFIG);
        if (storedSaveOrderConfig != null) {
            return Optional.of(SaveOrderConfig.parse(storedSaveOrderConfig));
        }
        return Optional.empty();
    }

    /**
     * Add default metadata for new database:
     */
    public void initializeNewDatabase() {
        metaData.put(SELECTOR_META_PREFIX + FieldName.KEYWORDS, new Vector<>());
        metaData.put(SELECTOR_META_PREFIX + FieldName.AUTHOR, new Vector<>());
        metaData.put(SELECTOR_META_PREFIX + FieldName.JOURNAL, new Vector<>());
        metaData.put(SELECTOR_META_PREFIX + FieldName.PUBLISHER, new Vector<>());
        metaData.put(SELECTOR_META_PREFIX + FieldName.REVIEW, new Vector<>());
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
        if (metaData.containsKey(key)) { //otherwise redundant and disturbing events are going to be posted
            metaData.remove(key);
            postChange();
        }
    }

    /**
     * Stores the specified data in this object, using the specified key. For
     * certain keys (e.g. "groupstree"), the objects in orderedData are
     * reconstructed from their textual (String) representation if they are of
     * type String, and stored as an actual instance.
     */
    public void putData(String key, List<String> orderedData) {
        metaData.put(key, orderedData);
        postChange();
    }

    /**
     * Parse the groups metadata string
     *
     * @param orderedData The vector of metadata strings
     */
    private void putGroups(List<String> orderedData) throws ParseException {
        try {
            groupsRoot = GroupTreeNode.parse(orderedData, Globals.prefs);
            eventBus.post(new GroupUpdatedEvent(this));
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
        eventBus.post(new GroupUpdatedEvent(this));
    }

    /**
     * Reads the next unit. Units are delimited by ';'.
     */
    private static Optional<String> getNextUnit(Reader reader) throws IOException {
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
            return Optional.of(res.toString());
        }
        return Optional.empty();
    }

    /**
     * @return the stored label patterns
     */
    public AbstractBibtexKeyPattern getBibtexKeyPattern() {
        if (bibtexKeyPattern != null) {
            return bibtexKeyPattern;
        }

        bibtexKeyPattern = new DatabaseBibtexKeyPattern(Globals.prefs);

        // read the data from the metadata and store it into the bibtexKeyPattern
        for (String key : this) {
            if (key.startsWith(PREFIX_KEYPATTERN)) {
                List<String> value = getData(key);
                String type = key.substring(PREFIX_KEYPATTERN.length());
                bibtexKeyPattern.addBibtexKeyPattern(type, value.get(0));
            }
        }
        List<String> defaultPattern = getData(KEYPATTERNDEFAULT);
        if (defaultPattern != null) {
            bibtexKeyPattern.setDefaultValue(defaultPattern.get(0));
        }

        return bibtexKeyPattern;
    }

    /**
     * Updates the stored key patterns to the given key patterns.
     *
     * @param bibtexKeyPattern the key patterns to update to. <br />
     *                     A reference to this object is stored internally and is returned at getBibtexKeyPattern();
     */
    public void setBibtexKeyPattern(AbstractBibtexKeyPattern bibtexKeyPattern) {
        // remove all keypatterns from metadata
        Iterator<String> iterator = this.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (key.startsWith(PREFIX_KEYPATTERN)) {
                iterator.remove();
            }
        }

        // set new value if it is not a default value
        Set<String> allKeys = bibtexKeyPattern.getAllKeys();
        for (String key : allKeys) {
            String metaDataKey = PREFIX_KEYPATTERN + key;
            if (!bibtexKeyPattern.isDefaultValue(key)) {
                List<String> data = new ArrayList<>();
                data.add(bibtexKeyPattern.getValue(key).get(0));
                this.putData(metaDataKey, data);
            }
        }

        // store default pattern
        if (bibtexKeyPattern.getDefaultValue() == null) {
            this.remove(KEYPATTERNDEFAULT);
        } else {
            List<String> data = new ArrayList<>();
            data.add(bibtexKeyPattern.getDefaultValue().get(0));
            this.putData(KEYPATTERNDEFAULT, data);
        }

        this.bibtexKeyPattern = bibtexKeyPattern;
    }

    public Optional<FieldFormatterCleanups> getSaveActions() {
        if (this.getData(SAVE_ACTIONS) == null) {
            return Optional.empty();
        } else {
            return Optional.of(FieldFormatterCleanups.parse(getData(SAVE_ACTIONS)));
        }
    }

    public Optional<BibDatabaseMode> getMode() {
        List<String> data = getData(DATABASE_TYPE);
        if ((data == null) || data.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(BibDatabaseMode.parse(data.get(0)));
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
        if (contentSelectors == null) {
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
    public Map<String, String> getAsStringMap() {

        Map<String, String> serializedMetaData = new TreeMap<>();

        // first write all meta data except groups
        for (Map.Entry<String, List<String>> metaItem : metaData.entrySet()) {

            StringBuilder stringBuilder = new StringBuilder();
            for (String dataItem : metaItem.getValue()) {
                stringBuilder.append(StringUtil.quote(dataItem, ";", '\\')).append(";");

                //in case of save actions, add an additional newline after the enabled flag
                if (metaItem.getKey().equals(SAVE_ACTIONS) && ("enabled".equals(dataItem) || "disabled".equals(dataItem))) {
                    stringBuilder.append(OS.NEWLINE);
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
            stringBuilder.append(OS.NEWLINE);

            for (String groupNode : groupsRoot.getTreeAsString()) {
                stringBuilder.append(StringUtil.quote(groupNode, ";", '\\'));
                stringBuilder.append(";");
                stringBuilder.append(OS.NEWLINE);
            }
            serializedMetaData.put(GROUPSTREE, stringBuilder.toString());
        }
        return serializedMetaData;
    }

    public void setSaveActions(FieldFormatterCleanups saveActions) {
        List<String> actionsSerialized = saveActions.getAsStringList();
        putData(SAVE_ACTIONS, actionsSerialized);
    }

    public void setSaveOrderConfig(SaveOrderConfig saveOrderConfig) {
        List<String> serialized = saveOrderConfig.getAsStringList();
        putData(SAVE_ORDER_CONFIG, serialized);
    }

    public void setMode(BibDatabaseMode mode) {
        putData(DATABASE_TYPE, Collections.singletonList(mode.getAsString()));
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

    /**
     * Posts a new {@link MetaDataChangedEvent} on the {@link EventBus}.
     */
    public void postChange() {
        eventBus.post(new MetaDataChangedEvent(this));
    }

    /**
     * Returns the encoding used during parsing.
     */
    public Optional<Charset> getEncoding() {
        return Optional.ofNullable(encoding);
    }

    public void setEncoding(Charset encoding) {
        this.encoding = Objects.requireNonNull(encoding);
    }

    public void clearMetaData() {
        metaData.clear();
    }

    public void registerListener(Object listener) {
        this.eventBus.register(listener);
    }

    public void unregisterListener(Object listener) {
        this.eventBus.unregister(listener);
    }
}
