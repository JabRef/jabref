package org.jabref.preferences;

import java.util.Map;
import java.util.Objects;

import org.jabref.gui.search.SearchDisplayMode;

public class SearchPreferences {

    private static final String SEARCH_GLOBAL = "searchGlobal";
    private static final String SEARCH_DISPLAY_MODE = "searchDisplayMode";
    private static final String SEARCH_CASE_SENSITIVE = "caseSensitiveSearch";
    private static final String SEARCH_REG_EXP = "regExpSearch";

    private static final String SEARCH_DIALOG_HEIGHT = "searchDialogHeight";
    private static final String SEARCH_DIALOG_WIDTH = "searchDialogWidth";
    private static final String SEARCH_DIALOG_POS_X = "searchDialogPosX";
    private static final String SEARCH_DIALOG_POS_Y = "searchDialogPosY";

    private final JabRefPreferences preferences;


    public SearchPreferences(JabRefPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    public static void putDefaults(Map<String, Object> defaults) {
        defaults.put(SEARCH_GLOBAL, Boolean.FALSE);
        defaults.put(SEARCH_DISPLAY_MODE, SearchDisplayMode.FILTER.toString());
        defaults.put(SEARCH_CASE_SENSITIVE, Boolean.FALSE);
        defaults.put(SEARCH_REG_EXP, Boolean.FALSE);

        defaults.put(SEARCH_DIALOG_WIDTH, 650);
        defaults.put(SEARCH_DIALOG_HEIGHT, 500);
        defaults.put(SEARCH_DIALOG_POS_X, 0);
        defaults.put(SEARCH_DIALOG_POS_Y, 0);
    }

    public boolean isGlobalSearch() {
        return preferences.getBoolean(SEARCH_GLOBAL);
    }

    public SearchPreferences setGlobalSearch(boolean isGlobalSearch) {
        preferences.putBoolean(SEARCH_GLOBAL, isGlobalSearch);
        return this;
    }

    public SearchDisplayMode getSearchMode() {
        try {
            return SearchDisplayMode.valueOf(preferences.get(SEARCH_DISPLAY_MODE));
        } catch (IllegalArgumentException ex) {
            // Should only occur when the searchmode is set directly via preferences.put and the enum was not used
            return SearchDisplayMode.valueOf((String) preferences.defaults.get(SEARCH_DISPLAY_MODE));
        }
    }

    public SearchPreferences setSearchMode(SearchDisplayMode searchDisplayMode) {
        preferences.put(SEARCH_DISPLAY_MODE, Objects.requireNonNull(searchDisplayMode).toString());
        return this;
    }

    public boolean isCaseSensitive() {
        return preferences.getBoolean(SEARCH_CASE_SENSITIVE);
    }

    public SearchPreferences setCaseSensitive(boolean isCaseSensitive) {
        preferences.putBoolean(SEARCH_CASE_SENSITIVE, isCaseSensitive);
        return this;
    }

    public boolean isRegularExpression() {
        return preferences.getBoolean(SEARCH_REG_EXP);
    }

    public SearchPreferences setRegularExpression(boolean isRegularExpression) {
        preferences.putBoolean(SEARCH_REG_EXP, isRegularExpression);
        return this;
    }

    public int getSeachDialogWidth() {
        return preferences.getInt(SEARCH_DIALOG_WIDTH);
    }

    public SearchPreferences setSearchDialogWidth(int width) {
        preferences.putInt(SEARCH_DIALOG_WIDTH, width);
        return this;
    }

    public int getSeachDialogHeight() {
        return preferences.getInt(SEARCH_DIALOG_HEIGHT);
    }

    public SearchPreferences setSearchDialogHeight(int height) {
        preferences.putInt(SEARCH_DIALOG_HEIGHT, height);
        return this;
    }

    public int getSearchDialogPosX() {
        return preferences.getInt(SEARCH_DIALOG_POS_X);
    }

    public SearchPreferences setSearchDialogPosX(int x) {
        preferences.putInt(SEARCH_DIALOG_POS_X, x);
        return this;
    }

    public int getSearchDialogPosY() {
        return preferences.getInt(SEARCH_DIALOG_POS_Y);
    }

    public SearchPreferences setSearchDialogPosY(int y) {
        preferences.putInt(SEARCH_DIALOG_POS_Y, y);
        return this;
    }

}
