package org.jabref.gui.maintable;

import java.util.List;
import java.util.Map;

import javafx.scene.control.TableColumn.SortType;

import org.jabref.model.entry.field.SpecialField;

public class ColumnPreferences {

    public static final double DEFAULT_FIELD_LENGTH = 100;
    private final boolean showFileColumn;
    private final boolean showUrlColumn;
    private final boolean preferDoiOverUrl;
    private final boolean showEprintColumn;
    private final List<String> normalColumns;
    private final List<SpecialField> specialFieldColumns;
    private final boolean autoSyncSpecialFieldsToKeyWords;
    private final boolean serializeSpecialFields;
    private final List<String> extraFileColumns;
    private final Map<String, Double> columnWidths;
    private final Map<String, SortType> columnSortType;

    public ColumnPreferences(boolean showFileColumn, boolean showUrlColumn, boolean preferDoiOverUrl, boolean showEprintColumn, List<String> normalColumns, List<SpecialField> specialFieldColumns, boolean autoSyncSpecialFieldsToKeyWords, boolean serializeSpecialFields, List<String> extraFileColumns, Map<String, Double> columnWidths, Map<String, SortType> columnSortType) {
        this.showFileColumn = showFileColumn;
        this.showUrlColumn = showUrlColumn;
        this.preferDoiOverUrl = preferDoiOverUrl;
        this.showEprintColumn = showEprintColumn;
        this.normalColumns = normalColumns;
        this.specialFieldColumns = specialFieldColumns;
        this.autoSyncSpecialFieldsToKeyWords = autoSyncSpecialFieldsToKeyWords;
        this.serializeSpecialFields = serializeSpecialFields;
        this.extraFileColumns = extraFileColumns;
        this.columnWidths = columnWidths;
        this.columnSortType = columnSortType;
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

    public boolean getAutoSyncSpecialFieldsToKeyWords() {
        return autoSyncSpecialFieldsToKeyWords;
    }

    public boolean getSerializeSpecialFields() {
        return serializeSpecialFields;
    }

    public List<String> getNormalColumns() {
        return normalColumns;
    }

    public Map<String, Double> getColumnWidths() {
        return columnWidths;
    }

    public double getPrefColumnWidth(String columnName) {
        return columnWidths.getOrDefault(columnName, DEFAULT_FIELD_LENGTH);
    }

    public Map<String, SortType> getSortTypesForColumns() {
        return columnSortType;
    }
}
