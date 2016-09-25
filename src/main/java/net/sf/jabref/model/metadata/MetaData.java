package net.sf.jabref.model.metadata;

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

import net.sf.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import net.sf.jabref.model.bibtexkeypattern.DatabaseBibtexKeyPattern;
import net.sf.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.groups.GroupTreeNode;
import net.sf.jabref.model.groups.event.GroupUpdatedEvent;
import net.sf.jabref.model.metadata.event.MetaDataChangedEvent;

import com.google.common.eventbus.EventBus;

public class MetaData implements Iterable<String> {
    public static final String META_FLAG = "jabref-meta: ";
    private static final String SAVE_ORDER_CONFIG = "saveOrderConfig";

    public static final String SAVE_ACTIONS = "saveActions";
    private static final String PREFIX_KEYPATTERN = "keypattern_";
    private static final String KEYPATTERNDEFAULT = "keypatterndefault";
    private static final String DATABASE_TYPE = "databaseType";

    public static final String GROUPSTREE = "groupstree";
    private static final String FILE_DIRECTORY = FieldName.FILE + FileDirectoryPreferences.DIR_SUFFIX;
    public static final String SELECTOR_META_PREFIX = "selector_";
    private static final String PROTECTED_FLAG_META = "protectedFlag";

    public static final char ESCAPE_CHARACTER = '\\';
    public static final char SEPARATOR_CHARACTER = ';';
    public static final String SEPARATOR_STRING = String.valueOf(SEPARATOR_CHARACTER);

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
    public MetaData(Map<String, List<String>> parsedData) {
        Objects.requireNonNull(parsedData);
        clearMetaData();
        metaData.putAll(parsedData);
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


    public void setParsedData(Map<String, List<String>> parsedMetaData) {
        clearMetaData();
        metaData.putAll(parsedMetaData);
    }


    public Optional<SaveOrderConfig> getSaveOrderConfig() {
        List<String> storedSaveOrderConfig = getData(SAVE_ORDER_CONFIG);
        if (storedSaveOrderConfig != null) {
            return Optional.of(SaveOrderConfig.parse(storedSaveOrderConfig));
        }
        return Optional.empty();
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


    public Optional<GroupTreeNode> getGroups() {
        return Optional.ofNullable(groupsRoot);
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
     * @return the stored label patterns
     */
    public AbstractBibtexKeyPattern getBibtexKeyPattern(GlobalBibtexKeyPattern globalPattern) {
        if (bibtexKeyPattern != null) {
            return bibtexKeyPattern;
        }

        bibtexKeyPattern = new DatabaseBibtexKeyPattern(globalPattern);

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
            if (!bibtexKeyPattern.isDefaultValue(key)) {
                List<String> data = new ArrayList<>();
                data.add(bibtexKeyPattern.getValue(key).get(0));
                String metaDataKey = PREFIX_KEYPATTERN + key;
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

    public Optional<List<String>> getSaveActions() {
        return Optional.ofNullable(getData(SAVE_ACTIONS));
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

    public Map<String, List<String>> getMetaData() {
        return new HashMap<>(metaData);
    }

    public void setSaveActions(List<String> actionsSerialized) {
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

    public void postGroupChange() {
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
