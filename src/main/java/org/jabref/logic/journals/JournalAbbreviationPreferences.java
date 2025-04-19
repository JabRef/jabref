package org.jabref.logic.journals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Class for storing and managing journal abbreviation preferences
 */
public class JournalAbbreviationPreferences {

    private final ObservableList<String> externalJournalLists;
    private final BooleanProperty useFJournalField;
    private final Map<String, Boolean> enabledExternalLists = new HashMap<>();
    private final BooleanProperty enabledListsChanged = new SimpleBooleanProperty(false);
    
    /**
     * Constructs a new JournalAbbreviationPreferences with the given external journal lists and FJournal field preference
     *
     * @param externalJournalLists List of paths to external journal abbreviation files
     * @param useFJournalField Whether to use the FJournal field
     */
    public JournalAbbreviationPreferences(List<String> externalJournalLists,
                                          boolean useFJournalField) {
        this.externalJournalLists = FXCollections.observableArrayList(externalJournalLists);
        this.useFJournalField = new SimpleBooleanProperty(useFJournalField);
    }

    /**
     * Constructs a new JournalAbbreviationPreferences with the given external journal lists, FJournal field preference,
     * and enabled states for journal abbreviation sources
     *
     * @param externalJournalLists List of paths to external journal abbreviation files
     * @param useFJournalField Whether to use the FJournal field
     * @param enabledExternalLists Map of source paths to their enabled states
     */
    public JournalAbbreviationPreferences(List<String> externalJournalLists,
                                         boolean useFJournalField,
                                         Map<String, Boolean> enabledExternalLists) {
        this(externalJournalLists, useFJournalField);
        if (enabledExternalLists != null) {
            this.enabledExternalLists.putAll(enabledExternalLists);
        }
    }

    public ObservableList<String> getExternalJournalLists() {
        return externalJournalLists;
    }

    public void setExternalJournalLists(List<String> list) {
        externalJournalLists.clear();
        externalJournalLists.addAll(list);
    }

    public boolean shouldUseFJournalField() {
        return useFJournalField.get();
    }

    public BooleanProperty useFJournalFieldProperty() {
        return useFJournalField;
    }

    public void setUseFJournalField(boolean useFJournalField) {
        this.useFJournalField.set(useFJournalField);
    }
    
    /**
     * Checks if a journal abbreviation source is enabled
     *
     * @param sourcePath Path to the abbreviation source
     * @return true if the source is enabled or has no explicit state (default is enabled)
     */
    public boolean isSourceEnabled(String sourcePath) {
        if (sourcePath == null) {
            return true;
        }
        return enabledExternalLists.getOrDefault(sourcePath, true);
    }
    
    /**
     * Sets the enabled state for a journal abbreviation source
     *
     * @param sourcePath Path to the abbreviation source
     * @param enabled Whether the source should be enabled
     */
    public void setSourceEnabled(String sourcePath, boolean enabled) {
        if (sourcePath == null) {
            return;
        }
        enabledExternalLists.put(sourcePath, enabled);
        enabledListsChanged.set(!enabledListsChanged.get());
    }
    
    /**
     * Gets all enabled/disabled states for journal abbreviation sources
     *
     * @return Map of source paths to their enabled states
     */
    public Map<String, Boolean> getEnabledExternalLists() {
        return new HashMap<>(enabledExternalLists);
    }
    
    /**
     * Sets all enabled/disabled states for journal abbreviation sources
     *
     * @param enabledLists Map of source paths to their enabled states
     */
    public void setEnabledExternalLists(Map<String, Boolean> enabledLists) {
        this.enabledExternalLists.clear();
        if (enabledLists != null) {
            this.enabledExternalLists.putAll(enabledLists);
        }
        enabledListsChanged.set(!enabledListsChanged.get());
    }
    
    /**
     * Property that changes whenever the enabled states map changes
     * Used for binding/listening to changes
     *
     * @return A boolean property that toggles when enabled states change
     */
    public BooleanProperty enabledListsChangedProperty() {
        return enabledListsChanged;
    }
    
    /**
     * Checks if a specific source has an explicit enabled/disabled setting
     *
     * @param sourcePath Path to check
     * @return True if there is an explicit setting for this source
     */
    public boolean hasExplicitEnabledSetting(String sourcePath) {
        return enabledExternalLists.containsKey(sourcePath);
    }
}
