package org.jabref.model.metadata;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.citationkeypattern.AbstractCitationKeyPatterns;
import org.jabref.logic.citationkeypattern.CitationKeyPattern;
import org.jabref.logic.citationkeypattern.DatabaseCitationKeyPatterns;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.util.Version;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.database.event.ChangePropagation;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.event.GroupUpdatedEvent;
import org.jabref.model.metadata.event.MetaDataChangedEvent;

import com.google.common.eventbus.EventBus;
import com.tobiasdiez.easybind.optional.OptionalBinding;
import com.tobiasdiez.easybind.optional.OptionalWrapper;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllowedToUseLogic("because it needs access to citation pattern and cleanups")
public class MetaData {

    public static final String META_FLAG = "jabref-meta: ";
    public static final String ENTRYTYPE_FLAG = "jabref-entrytype: ";
    public static final String SAVE_ORDER_CONFIG = "saveOrderConfig"; // ToDo: Rename in next major version to saveOrder, adapt testbibs
    public static final String SAVE_ACTIONS = "saveActions";
    public static final String PREFIX_KEYPATTERN = "keypattern_";
    public static final String KEYPATTERNDEFAULT = "keypatterndefault";
    public static final String DATABASE_TYPE = "databaseType";
    public static final String VERSION_DB_STRUCT = "VersionDBStructure";
    public static final String GROUPSTREE = "grouping";
    public static final String GROUPSTREE_LEGACY = "groupstree";
    public static final String GROUPS_SEARCH_SYNTAX_VERSION = "groups-search-syntax-version";
    public static final String FILE_DIRECTORY = "fileDirectory";
    public static final String FILE_DIRECTORY_LATEX = "fileDirectoryLatex";
    public static final String PROTECTED_FLAG_META = "protectedFlag";
    public static final String SELECTOR_META_PREFIX = "selector_";
    public static final String BIBDESK_STATIC_FLAG = "BibDesk Static Groups";

    public static final char ESCAPE_CHARACTER = '\\';
    public static final char SEPARATOR_CHARACTER = ';';
    public static final String SEPARATOR_STRING = String.valueOf(SEPARATOR_CHARACTER);
    public static final String BLG_FILE_PATH = "blgFilePath";
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaData.class);
    private final EventBus eventBus = new EventBus();
    private final Map<EntryType, String> citeKeyPatterns = new HashMap<>(); // <BibType, Pattern>
    private final Map<String, String> userFileDirectory = new HashMap<>(); // <User, FilePath>
    private final Map<String, Path> laTexFileDirectory = new HashMap<>(); // <User, FilePath>

    private final ObjectProperty<GroupTreeNode> groupsRoot = new SimpleObjectProperty<>(null);
    private final OptionalBinding<GroupTreeNode> groupsRootBinding = new OptionalWrapper<>(groupsRoot);
    private final Map<String, Path> blgFilePathMap = new HashMap<>();
    private Optional<Version> groupSearchSyntaxVersion = Optional.empty();

    private Charset encoding;
    private SaveOrder saveOrder;
    private String defaultCiteKeyPattern;
    private FieldFormatterCleanups saveActions;
    private BibDatabaseMode mode;
    private boolean isProtected;
    private String librarySpecificFileDirectory;
    private final ContentSelectors contentSelectors = new ContentSelectors();
    private final Map<String, List<String>> unknownMetaData = new HashMap<>();
    private boolean isEventPropagationEnabled = true;
    private boolean encodingExplicitlySupplied;
    private String versionDBStructure;

    /**
     * Constructs an empty metadata.
     */
    public MetaData() {
        // Do nothing
    }

    public Optional<SaveOrder> getSaveOrder() {
        return Optional.ofNullable(saveOrder);
    }

    public void setSaveOrder(SaveOrder saveOrder) {
        this.saveOrder = saveOrder;
        postChange();
    }

    public Optional<GroupTreeNode> getGroups() {
        return groupsRootBinding.getValue();
    }

    public OptionalBinding<GroupTreeNode> groupsBinding() {
        return groupsRootBinding;
    }

    /**
     * Sets a new group root node. <b>WARNING </b>: This invalidates everything returned by getGroups() so far!!!
     */
    public void setGroups(GroupTreeNode root) {
        Objects.requireNonNull(root);
        groupsRoot.setValue(root);
        root.subscribeToDescendantChanged(groupTreeNode -> groupsRootBinding.invalidate());
        root.subscribeToDescendantChanged(groupTreeNode -> eventBus.post(new GroupUpdatedEvent(this)));
        eventBus.post(new GroupUpdatedEvent(this));
        postChange();
    }

    public void setGroupSearchSyntaxVersion(Version version) {
        groupSearchSyntaxVersion = Optional.of(version);
        postChange();
    }

    public Optional<Version> getGroupSearchSyntaxVersion() {
        return this.groupSearchSyntaxVersion;
    }

    /**
     * @return the stored label patterns
     */
    public AbstractCitationKeyPatterns getCiteKeyPatterns(GlobalCitationKeyPatterns globalPatterns) {
        Objects.requireNonNull(globalPatterns);
        AbstractCitationKeyPatterns bibtexKeyPatterns = new DatabaseCitationKeyPatterns(globalPatterns);

        // Add stored key patterns
        citeKeyPatterns.forEach(bibtexKeyPatterns::addCitationKeyPattern);
        getDefaultCiteKeyPattern().ifPresent(bibtexKeyPatterns::setDefaultValue);

        return bibtexKeyPatterns;
    }

    /**
     * Updates the stored key patterns to the given key patterns.
     *
     * @param bibtexKeyPatterns the key patterns to update to. <br /> A reference to this object is stored internally and is returned at getCiteKeyPattern();
     */
    public void setCiteKeyPattern(AbstractCitationKeyPatterns bibtexKeyPatterns) {
        Objects.requireNonNull(bibtexKeyPatterns);

        CitationKeyPattern defaultValue = bibtexKeyPatterns.getDefaultValue();
        Map<EntryType, CitationKeyPattern> nonDefaultPatterns = bibtexKeyPatterns.getPatterns();
        setCiteKeyPattern(defaultValue, nonDefaultPatterns);
    }

    public void setCiteKeyPattern(CitationKeyPattern defaultValue, Map<EntryType, CitationKeyPattern> nonDefaultPatterns) {
        // Remove all patterns from metadata
        citeKeyPatterns.clear();

        // Set new value if it is not a default value
        for (Map.Entry<EntryType, CitationKeyPattern> pattern : nonDefaultPatterns.entrySet()) {
            citeKeyPatterns.put(pattern.getKey(), pattern.getValue().stringRepresentation());
        }

        // Store default pattern
        if (defaultValue.equals(CitationKeyPattern.NULL_CITATION_KEY_PATTERN)) {
            defaultCiteKeyPattern = null;
        } else {
            defaultCiteKeyPattern = defaultValue.stringRepresentation();
        }

        postChange();
    }

    public Optional<FieldFormatterCleanups> getSaveActions() {
        return Optional.ofNullable(saveActions);
    }

    public void setSaveActions(FieldFormatterCleanups saveActions) {
        this.saveActions = Objects.requireNonNull(saveActions);
        postChange();
    }

    public Optional<BibDatabaseMode> getMode() {
        return Optional.ofNullable(mode);
    }

    public void setMode(BibDatabaseMode mode) {
        if (mode == this.mode) {
            return;
        }

        this.mode = Objects.requireNonNull(mode);
        postChange();
    }

    public boolean isProtected() {
        return isProtected;
    }

    public ContentSelectors getContentSelectors() {
        return contentSelectors;
    }

    public SortedSet<ContentSelector> getContentSelectorsSorted() {
        return contentSelectors.getContentSelectors();
    }

    public void addContentSelector(ContentSelector contentSelector) {
        this.contentSelectors.addContentSelector(contentSelector);
        postChange();
    }

    public void clearContentSelectors(Field field) {
        contentSelectors.removeSelector(field);
        postChange();
    }

    public List<String> getContentSelectorValuesForField(Field field) {
        return contentSelectors.getSelectorValuesForField(field);
    }

    public Optional<String> getLibrarySpecificFileDirectory() {
        return Optional.ofNullable(librarySpecificFileDirectory);
    }

    public void setLibrarySpecificFileDirectory(String path) {
        librarySpecificFileDirectory = Objects.requireNonNull(path).trim();
        postChange();
    }

    public Optional<String> getVersionDBStructure() {
        return Optional.ofNullable(versionDBStructure);
    }

    public void setVersionDBStructure(String version) {
        versionDBStructure = Objects.requireNonNull(version).trim();
        postChange();
    }

    public Optional<String> getUserFileDirectory(String user) {
        return Optional.ofNullable(userFileDirectory.get(user));
    }

    public void markAsProtected() {
        isProtected = true;
        postChange();
    }

    public void clearLibrarySpecificFileDirectory() {
        librarySpecificFileDirectory = null;
        postChange();
    }

    public void setUserFileDirectory(String user, String path) {
        userFileDirectory.put(Objects.requireNonNull(user), Objects.requireNonNull(path));
        postChange();
    }

    public void clearUserFileDirectory(String user) {
        userFileDirectory.remove(user);
        postChange();
    }

    public Optional<Path> getLatexFileDirectory(String user) {
        return Optional.ofNullable(laTexFileDirectory.get(user));
    }

    public void setLatexFileDirectory(String user, Path path) {
        laTexFileDirectory.put(Objects.requireNonNull(user), Objects.requireNonNull(path));
        postChange();
    }

    public void clearLatexFileDirectory(String user) {
        laTexFileDirectory.remove(user);
        postChange();
    }

    public void markAsNotProtected() {
        isProtected = false;
        postChange();
    }

    public void clearSaveActions() {
        saveActions = null;
        postChange();
    }

    public void clearSaveOrder() {
        saveOrder = null;
        postChange();
    }

    /**
     * Posts a new {@link MetaDataChangedEvent} on the {@link EventBus}.
     */
    private void postChange() {
        if (isEventPropagationEnabled) {
            eventBus.post(new MetaDataChangedEvent(this));
        }
    }

    /**
     * Returns the encoding used during parsing.
     */
    public Optional<Charset> getEncoding() {
        return Optional.ofNullable(encoding);
    }

    public void setEncoding(Charset encoding) {
        setEncoding(encoding, ChangePropagation.POST_EVENT);
    }

    /**
     * This method (with additional parameter) has been introduced to avoid event loops while saving a database.
     */
    public void setEncoding(Charset encoding, ChangePropagation postChanges) {
        this.encoding = Objects.requireNonNull(encoding);
        if (postChanges == ChangePropagation.POST_EVENT) {
            postChange();
        }
    }

    public boolean getEncodingExplicitlySupplied() {
        return encodingExplicitlySupplied;
    }

    /**
     * Sets the indication whether the encoding was set using "% Encoding: ..." or whether it was detected "magically"
     */
    public void setEncodingExplicitlySupplied(boolean encodingExplicitlySupplied) {
        this.encodingExplicitlySupplied = encodingExplicitlySupplied;
    }

    /**
     * If disabled {@link MetaDataChangedEvent} will not be posted.
     */
    public void setEventPropagation(boolean enabled) {
        this.isEventPropagationEnabled = enabled;
    }

    public void registerListener(Object listener) {
        this.eventBus.register(listener);
    }

    public void unregisterListener(Object listener) {
        try {
            this.eventBus.unregister(listener);
        } catch (IllegalArgumentException e) {
            // occurs if the event source has not been registered, should not prevent shutdown
        }
    }

    public Optional<String> getDefaultCiteKeyPattern() {
        return Optional.ofNullable(defaultCiteKeyPattern);
    }

    public boolean isEmpty() {
        return this.equals(new MetaData());
    }

    public Map<String, String> getUserFileDirectories() {
        return Collections.unmodifiableMap(userFileDirectory);
    }

    public Map<String, Path> getLatexFileDirectories() {
        return Collections.unmodifiableMap(laTexFileDirectory);
    }

    public Map<String, List<String>> getUnknownMetaData() {
        return Collections.unmodifiableMap(unknownMetaData);
    }

    public void putUnknownMetaDataItem(String key, List<String> value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        unknownMetaData.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MetaData that)) {
            return false;
        }
        return (isProtected == that.isProtected)
                && Objects.equals(groupsRoot.getValue(), that.groupsRoot.getValue())
                && Objects.equals(encoding, that.encoding)
                && Objects.equals(encodingExplicitlySupplied, that.encodingExplicitlySupplied)
                && Objects.equals(saveOrder, that.saveOrder)
                && Objects.equals(citeKeyPatterns, that.citeKeyPatterns)
                && Objects.equals(userFileDirectory, that.userFileDirectory)
                && Objects.equals(laTexFileDirectory, that.laTexFileDirectory)
                && Objects.equals(defaultCiteKeyPattern, that.defaultCiteKeyPattern)
                && Objects.equals(saveActions, that.saveActions)
                && (mode == that.mode)
                && Objects.equals(librarySpecificFileDirectory, that.librarySpecificFileDirectory)
                && Objects.equals(contentSelectors, that.contentSelectors)
                && Objects.equals(versionDBStructure, that.versionDBStructure);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isProtected, groupsRoot.getValue(), encoding, encodingExplicitlySupplied, saveOrder, citeKeyPatterns, userFileDirectory,
                laTexFileDirectory, defaultCiteKeyPattern, saveActions, mode, librarySpecificFileDirectory, contentSelectors, versionDBStructure);
    }

    @Override
    public String toString() {
        return "MetaData [citeKeyPatterns=" + citeKeyPatterns + ", userFileDirectory=" + userFileDirectory + ", laTexFileDirectory=" + laTexFileDirectory + ", groupsRoot=" + groupsRoot + ", encoding=" + encoding + ", saveOrderConfig=" + saveOrder + ", defaultCiteKeyPattern=" + defaultCiteKeyPattern + ", saveActions=" + saveActions + ", mode=" + mode + ", isProtected=" + isProtected + ", librarySpecificFileDirectory=" + librarySpecificFileDirectory + ", contentSelectors=" + contentSelectors + ", encodingExplicitlySupplied=" + encodingExplicitlySupplied + ", VersionDBStructure=" + versionDBStructure + "]";
    }

    public Optional<Path> getBlgFilePath(String user) {
        return Optional.ofNullable(blgFilePathMap.get(user));
    }

    public void setBlgFilePath(@NonNull String user, @NonNull Path path) {
        blgFilePathMap.put(user, path);
        postChange();
    }

    public void clearBlgFilePath(String user) {
        blgFilePathMap.remove(user);
        postChange();
    }

    public Map<String, Path> getBlgFilePaths() {
        return blgFilePathMap;
    }
}
