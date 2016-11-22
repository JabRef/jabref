package net.sf.jabref.logic.autocompleter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.sf.jabref.logic.journals.JournalAbbreviationPreferences;
import net.sf.jabref.preferences.JabRefPreferences;

public class AutoCompletePreferences {

    private final JabRefPreferences preferences;

    private static final String AUTOCOMPLETER_SHORTEST_TO_COMPLETE = "shortestToComplete";
    private static final String AUTOCOMPLETER_FIRSTNAME_MODE = "autoCompFirstNameMode";
    private static final String AUTOCOMPLETER_LAST_FIRST = "autoCompLF";
    private static final String AUTOCOMPLETER_FIRST_LAST = "autoCompFF";
    private static final String AUTOCOMPLETER_COMPLETE_FIELDS = "autoCompleteFields";


    public static void putDefaults(Map<String, Object> defaults) {
        defaults.put(AUTOCOMPLETER_SHORTEST_TO_COMPLETE, 1);
        defaults.put(AUTOCOMPLETER_FIRSTNAME_MODE, AutoCompleteFirstNameMode.BOTH.name());
        defaults.put(AUTOCOMPLETER_FIRST_LAST, Boolean.FALSE); // "Autocomplete names in 'Firstname Lastname' format only"
        defaults.put(AUTOCOMPLETER_LAST_FIRST, Boolean.FALSE); // "Autocomplete names in 'Lastname, Firstname' format only"
        defaults.put(AUTOCOMPLETER_COMPLETE_FIELDS, "author;editor;title;journal;publisher;keywords");
    }

    public AutoCompletePreferences(JabRefPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    public int getShortestLengthToComplete() {
        return preferences.getInt(AUTOCOMPLETER_SHORTEST_TO_COMPLETE);
    }

    public void setShortestLengthToComplete(Integer value) {
        preferences.putInt(AUTOCOMPLETER_SHORTEST_TO_COMPLETE, value);
    }

    /**
     * Returns how the first names are handled.
     * For "ONLY_FULL", the auto completer returns the full name, e.g. "Smith, Bob"
     * For "ONLY_ABBREVIATED", the auto completer returns the first name abbreviated, e.g. "Smith, B."
     * For "BOTH", the auto completer returns both versions.
     */
    public AutoCompleteFirstNameMode getFirstnameMode() {
        try {
            return AutoCompleteFirstNameMode.valueOf(preferences.get(AUTOCOMPLETER_FIRSTNAME_MODE));
        } catch (IllegalArgumentException ex) {
            // Should only occur when preferences are set directly via preferences.put and not via setFirstnameMode
            return AutoCompleteFirstNameMode.BOTH;
        }
    }

    public void setFirstnameMode(AutoCompleteFirstNameMode mode) {
        preferences.put(AUTOCOMPLETER_FIRSTNAME_MODE, mode.name());
    }

    public boolean getOnlyCompleteLastFirst() {
        return preferences.getBoolean(AUTOCOMPLETER_LAST_FIRST);
    }

    public void setOnlyCompleteLastFirst(boolean value) {
        preferences.putBoolean(AUTOCOMPLETER_LAST_FIRST, value);
    }

    public boolean getOnlyCompleteFirstLast() {
        return preferences.getBoolean(AUTOCOMPLETER_FIRST_LAST);
    }

    public void setOnlyCompleteFirstLast(boolean value) {
        preferences.putBoolean(AUTOCOMPLETER_FIRST_LAST, value);
    }

    public List<String> getCompleteNames() {
        return preferences.getStringList(AUTOCOMPLETER_COMPLETE_FIELDS);
    }

    public String getCompleteNamesAsString() {
        return preferences.get(AUTOCOMPLETER_COMPLETE_FIELDS);
    }

    public void setCompleteNames(String value) {
        preferences.put(AUTOCOMPLETER_COMPLETE_FIELDS, value);
    }

    public JournalAbbreviationPreferences getJournalAbbreviationPreferences() {
        return preferences.getJournalAbbreviationPreferences();
    }
}