package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.specialfields.SpecialField;
import org.jabref.preferences.JabRefPreferences;

public class ColumnPreferences {

    private final boolean showFileColumn;
    private final boolean showUrlColumn;
    private final boolean preferDoiOverUrl;
    private final boolean showEprintColumn;
    private final List<String> normalColumns;
    private final List<SpecialField> specialFieldColumns;
    private final List<String> extraFileColumns;

    public ColumnPreferences(boolean showFileColumn, boolean showUrlColumn, boolean preferDoiOverUrl, boolean showEprintColumn, List<String> normalColumns, List<SpecialField> specialFieldColumns, List<String> extraFileColumns) {
        this.showFileColumn = showFileColumn;
        this.showUrlColumn = showUrlColumn;
        this.preferDoiOverUrl = preferDoiOverUrl;
        this.showEprintColumn = showEprintColumn;
        this.normalColumns = normalColumns;
        this.specialFieldColumns = specialFieldColumns;
        this.extraFileColumns = extraFileColumns;
    }

    public static ColumnPreferences from(JabRefPreferences preferences) {
        return new ColumnPreferences(
                preferences.getBoolean(JabRefPreferences.FILE_COLUMN),
                preferences.getBoolean(JabRefPreferences.URL_COLUMN),
                preferences.getBoolean(JabRefPreferences.PREFER_URL_DOI),
                preferences.getBoolean(JabRefPreferences.ARXIV_COLUMN),
                preferences.getStringList(JabRefPreferences.COLUMN_NAMES),
                createSpecialFieldColumns(preferences),
                createExtraFileColumns(preferences)
        );
    }

    private static List<String> createExtraFileColumns(JabRefPreferences preferences) {
        if (preferences.getBoolean(JabRefPreferences.EXTRA_FILE_COLUMNS)) {
            return preferences.getStringList(JabRefPreferences.LIST_OF_FILE_COLUMNS);
        } else {
            return Collections.emptyList();
        }
    }

    private static List<SpecialField> createSpecialFieldColumns(JabRefPreferences preferences) {
        if (preferences.getBoolean(JabRefPreferences.SPECIALFIELDSENABLED)) {
            List<SpecialField> fieldsToShow = new ArrayList<>();
            if (preferences.getBoolean(JabRefPreferences.SHOWCOLUMN_RANKING)) {
                fieldsToShow.add(SpecialField.RANKING);
            }
            if (preferences.getBoolean(JabRefPreferences.SHOWCOLUMN_RELEVANCE)) {
                fieldsToShow.add(SpecialField.RELEVANCE);
            }
            if (preferences.getBoolean(JabRefPreferences.SHOWCOLUMN_QUALITY)) {
                fieldsToShow.add(SpecialField.QUALITY);
            }

            if (preferences.getBoolean(JabRefPreferences.SHOWCOLUMN_PRIORITY)) {
                fieldsToShow.add(SpecialField.PRIORITY);
            }

            if (preferences.getBoolean(JabRefPreferences.SHOWCOLUMN_PRINTED)) {
                fieldsToShow.add(SpecialField.PRINTED);
            }

            if (preferences.getBoolean(JabRefPreferences.SHOWCOLUMN_READ)) {
                fieldsToShow.add(SpecialField.READ_STATUS);
            }
            return fieldsToShow;
        } else {
            return Collections.emptyList();
        }
    }

    public boolean showFileColumn() {
        return showFileColumn;
    }

    public boolean showUrlColumn() {
        return showUrlColumn;
    }

    public boolean preferDoiOverUrl() {
        return preferDoiOverUrl;
    }

    public boolean showEprintColumn() {
        return showEprintColumn;
    }

    public List<String> getExtraFileColumns() {
        return extraFileColumns;
    }

    public List<SpecialField> getSpecialFieldColumns() {
        return specialFieldColumns;
    }

    public List<String> getNormalColumns() {
        return normalColumns;
    }
}
