package net.sf.jabref;

public class HighlightMatchingGroupPreferences {

    public static final String ALL = "all";
    public static final String ANY = "any";
    public static final String DISABLED = "";

    private final JabRefPreferences preferences;

    public HighlightMatchingGroupPreferences(JabRefPreferences preferences) {
        this.preferences = preferences;
    }

    public boolean isAll() {
        return ALL.equals(preferences.get(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING));
    }

    public boolean isAny() {
        return ANY.equals(preferences.get(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING));
    }

    public boolean isDisabled() {
        return !isAll() && !isAny();
    }

    public void setToAll() {
        preferences.put(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING, ALL);
    }

    public void setToAny() {
        preferences.put(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING, ANY);
    }

    public void setToDisabled() {
        preferences.put(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING, DISABLED);
    }

}
