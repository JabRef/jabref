package net.sf.jabref.model.metadata;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import net.sf.jabref.model.bibtexkeypattern.DatabaseBibtexKeyPattern;
import net.sf.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import net.sf.jabref.model.cleanup.FieldFormatterCleanups;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.database.event.ChangePropagation;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.groups.GroupTreeNode;
import net.sf.jabref.model.groups.event.GroupUpdatedEvent;
import net.sf.jabref.model.metadata.event.MetaDataChangedEvent;

import com.google.common.eventbus.EventBus;

public class MetaData {
    public static final String META_FLAG = "jabref-meta: ";
    public static final String SAVE_ORDER_CONFIG = "saveOrderConfig";
    public static final String SAVE_ACTIONS = "saveActions";
    public static final String PREFIX_KEYPATTERN = "keypattern_";
    public static final String KEYPATTERNDEFAULT = "keypatterndefault";
    public static final String DATABASE_TYPE = "databaseType";
    public static final String GROUPSTREE = "groupstree";
    public static final String FILE_DIRECTORY = FieldName.FILE + FileDirectoryPreferences.DIR_SUFFIX;
    public static final String PROTECTED_FLAG_META = "protectedFlag";

    public static final char ESCAPE_CHARACTER = '\\';
    public static final char SEPARATOR_CHARACTER = ';';
    public static final String SEPARATOR_STRING = String.valueOf(SEPARATOR_CHARACTER);

    private final EventBus eventBus = new EventBus();
    private GroupTreeNode groupsRoot;
    private Charset encoding;
    private SaveOrderConfig saveOrderConfig;
    private Map<String, String> citeKeyPatterns = new HashMap<>(); // <BibType, Pattern>
    private Map<String, String> userFileDirectory = new HashMap<>(); // <User, FilePath>
    private String defaultCiteKeyPattern;
    private FieldFormatterCleanups saveActions;
    private BibDatabaseMode mode;
    private boolean isProtected;
    private String defaultFileDirectory;

    /**
     * Constructs an empty metadata.
     */
    public MetaData() {
        // Do nothing
    }

    public Optional<SaveOrderConfig> getSaveOrderConfig() {
        return Optional.ofNullable(saveOrderConfig);
    }

    public void setSaveOrderConfig(SaveOrderConfig saveOrderConfig) {
        this.saveOrderConfig = saveOrderConfig;
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
        groupsRoot = Objects.requireNonNull(root);
        groupsRoot.subscribeToDescendantChanged(groupTreeNode -> eventBus.post(new GroupUpdatedEvent(this)));
        eventBus.post(new GroupUpdatedEvent(this));
    }

    /**
     * @return the stored label patterns
     */
    public AbstractBibtexKeyPattern getCiteKeyPattern(GlobalBibtexKeyPattern globalPattern) {
        Objects.requireNonNull(globalPattern);
        AbstractBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(globalPattern);

        // Add stored key patterns
        citeKeyPatterns.forEach(bibtexKeyPattern::addBibtexKeyPattern);
        getDefaultCiteKeyPattern().ifPresent(bibtexKeyPattern::setDefaultValue);

        return bibtexKeyPattern;
    }

    /**
     * Updates the stored key patterns to the given key patterns.
     *
     * @param bibtexKeyPattern the key patterns to update to. <br />
     *                     A reference to this object is stored internally and is returned at getCiteKeyPattern();
     */
    public void setCiteKeyPattern(AbstractBibtexKeyPattern bibtexKeyPattern) {
        Objects.requireNonNull(bibtexKeyPattern);

        List<String> defaultValue = bibtexKeyPattern.getDefaultValue();
        Map<String, List<String>> nonDefaultPatterns = bibtexKeyPattern.getPatterns();
        setCiteKeyPattern(defaultValue, nonDefaultPatterns);
    }

    public void setCiteKeyPattern(List<String> defaultValue, Map<String, List<String>> nonDefaultPatterns) {
        // Remove all patterns from metadata
        citeKeyPatterns.clear();

        // Set new value if it is not a default value
        for (Map.Entry<String, List<String>> pattern : nonDefaultPatterns.entrySet()) {
            citeKeyPatterns.put(pattern.getKey(), pattern.getValue().get(0));
        }

        // Store default pattern
        if (defaultValue.isEmpty()) {
            defaultCiteKeyPattern = null;
        } else {
            defaultCiteKeyPattern = defaultValue.get(0);
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
        this.mode = Objects.requireNonNull(mode);
        postChange();
    }

    public boolean isProtected() {
        return isProtected;
    }

    public Optional<String> getDefaultFileDirectory() {
        return Optional.ofNullable(defaultFileDirectory);
    }

    public void setDefaultFileDirectory(String path) {
        defaultFileDirectory = Objects.requireNonNull(path).trim();
        postChange();
    }

    public Optional<String> getUserFileDirectory(String user) {
        return Optional.ofNullable(userFileDirectory.get(user));
    }

    public void markAsProtected() {
        isProtected = true;
        postChange();
    }

    public void clearDefaultFileDirectory() {
        defaultFileDirectory = null;
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

    public void markAsNotProtected() {
        isProtected = false;
        postChange();
    }

    public void clearSaveActions() {
        saveActions = null;
        postChange();
    }

    public void clearSaveOrderConfig() {
        saveOrderConfig = null;
        postChange();
    }

    /**
     * Posts a new {@link MetaDataChangedEvent} on the {@link EventBus}.
     */
    private void postChange() {
        eventBus.post(new MetaDataChangedEvent(this));
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
     * This Method (with additional parameter) has been introduced to avoid event loops while saving a database.
     */
    public void setEncoding(Charset encoding, ChangePropagation postChanges) {
        this.encoding = Objects.requireNonNull(encoding);
        if (postChanges == ChangePropagation.POST_EVENT) {
            postChange();
        }
    }

    public void registerListener(Object listener) {
        this.eventBus.register(listener);
    }

    public void unregisterListener(Object listener) {
        this.eventBus.unregister(listener);
    }

    private Optional<String> getDefaultCiteKeyPattern() {
        return Optional.ofNullable(defaultCiteKeyPattern);
    }

    public boolean isEmpty() {
        return this.equals(new MetaData());
    }

    public Map<String, String> getUserFileDirectories() {
        return Collections.unmodifiableMap(userFileDirectory);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetaData metaData = (MetaData) o;
        return isProtected == metaData.isProtected && Objects.equals(groupsRoot, metaData.groupsRoot) && Objects.equals(
                encoding, metaData.encoding) && Objects.equals(saveOrderConfig, metaData.saveOrderConfig) && Objects
                .equals(citeKeyPatterns, metaData.citeKeyPatterns) && Objects.equals(userFileDirectory,
                metaData.userFileDirectory) && Objects.equals(defaultCiteKeyPattern, metaData.defaultCiteKeyPattern)
                && Objects.equals(saveActions, metaData.saveActions) && mode == metaData.mode && Objects.equals(
                defaultFileDirectory, metaData.defaultFileDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupsRoot, encoding, saveOrderConfig, citeKeyPatterns, userFileDirectory,
                defaultCiteKeyPattern, saveActions, mode, isProtected, defaultFileDirectory);
    }
}
