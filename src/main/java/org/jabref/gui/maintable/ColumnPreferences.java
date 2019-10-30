package org.jabref.gui.maintable;

import java.util.List;
import java.util.Map;

import javafx.scene.control.TableColumn.SortType;

public class ColumnPreferences {

    public static final String QUALIFIER_SEPARATOR = ":";
    public static final double DEFAULT_FIELD_LENGTH = 100;

    private final List<String> columnNames;
    private final boolean specialFieldsEnabled;
    private final boolean autoSyncSpecialFieldsToKeyWords;
    private final boolean serializeSpecialFields;
    private final boolean extraFileColumnsEnabled;
    private final Map<String, Double> columnWidths;
    private final Map<String, SortType> columnSortType;

    public ColumnPreferences(List<String> columnNames, boolean specialFieldsEnabled, boolean autoSyncSpecialFieldsToKeyWords, boolean serializeSpecialFields, boolean extraFileColumnsEnabled, Map<String, Double> columnWidths, Map<String, SortType> columnSortType) {
        this.columnNames = columnNames;
        this.specialFieldsEnabled = specialFieldsEnabled;
        this.autoSyncSpecialFieldsToKeyWords = autoSyncSpecialFieldsToKeyWords;
        this.serializeSpecialFields = serializeSpecialFields;
        this.extraFileColumnsEnabled = extraFileColumnsEnabled;
        this.columnWidths = columnWidths;
        this.columnSortType = columnSortType;
    }

    public boolean getSpecialFieldsEnabled() { return specialFieldsEnabled; }

    public boolean getAutoSyncSpecialFieldsToKeyWords() {
        return autoSyncSpecialFieldsToKeyWords;
    }

    public boolean getSerializeSpecialFields() {
        return serializeSpecialFields;
    }

    public boolean getExtraFileColumnsEnabled() { return extraFileColumnsEnabled; }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public Map<String, Double> getColumnWidths() {
        return columnWidths;
    }

    public double getColumnWidth(String columnName) {
        return columnWidths.getOrDefault(columnName, DEFAULT_FIELD_LENGTH);
    }

    public Map<String, SortType> getSortTypesForColumns() {
        return columnSortType;
    }
}
