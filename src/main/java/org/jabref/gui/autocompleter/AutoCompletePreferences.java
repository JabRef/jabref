package org.jabref.gui.autocompleter;

import java.util.Set;

import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class AutoCompletePreferences {

    private boolean shouldAutoComplete;
    private AutoCompleteFirstNameMode firstNameMode;
    private boolean onlyCompleteLastFirst;
    private boolean onlyCompleteFirstLast;
    private Set<Field> completeFields;
    private final JournalAbbreviationPreferences journalAbbreviationPreferences;

    public AutoCompletePreferences(boolean shouldAutoComplete, AutoCompleteFirstNameMode firstNameMode, boolean onlyCompleteLastFirst, boolean onlyCompleteFirstLast, Set<Field> completeFields, JournalAbbreviationPreferences journalAbbreviationPreferences) {
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
    public Set<Field> getCompleteFields() {
        return completeFields;
    }

    public void setCompleteFields(Set<Field> completeFields) {
        this.completeFields = completeFields;
    }

    public void setCompleteNames(String input) {
        setCompleteFields(FieldFactory.parseFieldList(input));
    }

    public String getCompleteNamesAsString() {
        return FieldFactory.serializeFieldsList(completeFields);
    }

    public JournalAbbreviationPreferences getJournalAbbreviationPreferences() {
        return journalAbbreviationPreferences;
    }
}
