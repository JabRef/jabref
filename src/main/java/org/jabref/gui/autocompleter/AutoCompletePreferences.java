package org.jabref.gui.autocompleter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.journals.JournalAbbreviationPreferences;

public class AutoCompletePreferences {

    private static final String DELIMITER = ";";
    private boolean shouldAutoComplete;
    private AutoCompleteFirstNameMode firstNameMode;
    private boolean onlyCompleteLastFirst;
    private boolean onlyCompleteFirstLast;
    private List<String> completeFields;
    private final JournalAbbreviationPreferences journalAbbreviationPreferences;

    public AutoCompletePreferences(boolean shouldAutoComplete, AutoCompleteFirstNameMode firstNameMode, boolean onlyCompleteLastFirst, boolean onlyCompleteFirstLast, List<String> completeFields, JournalAbbreviationPreferences journalAbbreviationPreferences) {
        this.shouldAutoComplete = shouldAutoComplete;
        this.firstNameMode = firstNameMode;
        this.onlyCompleteLastFirst = onlyCompleteLastFirst;
        this.onlyCompleteFirstLast = onlyCompleteFirstLast;
        this.completeFields = completeFields;
        this.journalAbbreviationPreferences = journalAbbreviationPreferences;
    }

    public void setShouldAutoComplete(boolean shouldAutoComplete) {
        this.shouldAutoComplete = shouldAutoComplete;
    }

    public boolean shouldAutoComplete() {
        return shouldAutoComplete;
    }

    /**
     * Returns how the first names are handled.
     */
    public AutoCompleteFirstNameMode getFirstNameMode() {
        return firstNameMode;
    }

    public void setFirstNameMode(AutoCompleteFirstNameMode firstNameMode) {
        this.firstNameMode = firstNameMode;
    }

    public boolean getOnlyCompleteLastFirst() {
        return onlyCompleteLastFirst;
    }

    public void setOnlyCompleteLastFirst(boolean onlyCompleteLastFirst) {
        this.onlyCompleteLastFirst = onlyCompleteLastFirst;
    }

    public boolean getOnlyCompleteFirstLast() {
        return onlyCompleteFirstLast;
    }

    public void setOnlyCompleteFirstLast(boolean onlyCompleteFirstLast) {
        this.onlyCompleteFirstLast = onlyCompleteFirstLast;
    }

    /**
     * Returns the list of fields for which autocomplete is enabled
     * @return List of field names
     */
    public List<String> getCompleteFields() {
        return completeFields;
    }

    public void setCompleteFields(List<String> completeFields) {
        this.completeFields = completeFields;
    }

    public void setCompleteNames(String input) {
        setCompleteFields(Arrays.asList(input.split(DELIMITER)));
    }

    public String getCompleteNamesAsString() {
        return completeFields.stream().collect(Collectors.joining(DELIMITER));
    }

    public JournalAbbreviationPreferences getJournalAbbreviationPreferences() {
        return journalAbbreviationPreferences;
    }
}
