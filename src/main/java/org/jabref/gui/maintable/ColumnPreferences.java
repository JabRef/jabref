package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibtexSingleField;
import org.jabref.model.entry.specialfields.SpecialField;
import org.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ColumnPreferences {

    private static final Log LOGGER = LogFactory.getLog(ColumnPreferences.class);

    private final boolean showFileColumn;
    private final boolean showUrlColumn;
    private final boolean preferDoiOverUrl;
    private final boolean showEprintColumn;
    private final List<String> normalColumns;
    private final List<SpecialField> specialFieldColumns;
    private final List<String> extraFileColumns;
    private final Map<String, Double> columnWidths;

    public ColumnPreferences(boolean showFileColumn, boolean showUrlColumn, boolean preferDoiOverUrl, boolean showEprintColumn, List<String> normalColumns, List<SpecialField> specialFieldColumns, List<String> extraFileColumns, Map<String, Double> columnWidths) {
        this.showFileColumn = showFileColumn;
        this.showUrlColumn = showUrlColumn;
        this.preferDoiOverUrl = preferDoiOverUrl;
        this.showEprintColumn = showEprintColumn;
        this.normalColumns = normalColumns;
        this.specialFieldColumns = specialFieldColumns;
        this.extraFileColumns = extraFileColumns;
        this.columnWidths = columnWidths;
    }

    public static ColumnPreferences from(JabRefPreferences preferences) {
        return new ColumnPreferences(
                preferences.getBoolean(JabRefPreferences.FILE_COLUMN),
                preferences.getBoolean(JabRefPreferences.URL_COLUMN),
                preferences.getBoolean(JabRefPreferences.PREFER_URL_DOI),
                preferences.getBoolean(JabRefPreferences.ARXIV_COLUMN),
                preferences.getStringList(JabRefPreferences.COLUMN_NAMES),
                createSpecialFieldColumns(preferences),
                createExtraFileColumns(preferences),
                createColumnWidths(preferences)
        );
    }

    private static Map<String, Double> createColumnWidths(JabRefPreferences preferences) {
        List<String> columns = preferences.getStringList(JabRefPreferences.COLUMN_NAMES);
        List<Double> widths = preferences
                .getStringList(JabRefPreferences.COLUMN_WIDTHS)
                .stream()
                .map(string -> {
                    try {
                        return Double.parseDouble(string);
                    } catch (NumberFormatException e) {
                        LOGGER.error("Exception while parsing column widths. Choosing default.", e);
                        return BibtexSingleField.DEFAULT_FIELD_LENGTH;
                    }
                })
                .collect(Collectors.toList());

        Map<String, Double> map = new TreeMap<>();
        for (int i = 0; i < columns.size(); i++) {
            map.put(columns.get(i), widths.get(i));
        }
        return map;
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

    public double getPrefColumnWidth(String columnName) {
        return columnWidths.getOrDefault(columnName, BibtexSingleField.DEFAULT_FIELD_LENGTH);
    }
}
