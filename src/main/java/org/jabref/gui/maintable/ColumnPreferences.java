package org.jabref.gui.maintable;

import java.util.List;
import java.util.Map;

import javafx.scene.control.TableColumn.SortType;

import org.jabref.model.entry.BibtexSingleField;
import org.jabref.model.entry.specialfields.SpecialField;

public class ColumnPreferences {

    private final boolean showFileColumn;
    private final boolean showUrlColumn;
    private final boolean preferDoiOverUrl;
    private final boolean showEprintColumn;
    private final List<String> normalColumns;
    private final List<SpecialField> specialFieldColumns;
    private final List<String> extraFileColumns;
    private final Map<String, Double> columnWidths;
    private final Map<String, SortType> columnSortType;

    public ColumnPreferences(boolean showFileColumn, boolean showUrlColumn, boolean preferDoiOverUrl, boolean showEprintColumn, List<String> normalColumns, List<SpecialField> specialFieldColumns, List<String> extraFileColumns, Map<String, Double> columnWidths, Map<String, SortType> columnSortType) {
        this.showFileColumn = showFileColumn;
        this.showUrlColumn = showUrlColumn;
        this.preferDoiOverUrl = preferDoiOverUrl;
        this.showEprintColumn = showEprintColumn;
        this.normalColumns = normalColumns;
        this.specialFieldColumns = specialFieldColumns;
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

    public List<String> getNormalColumns() {
        return normalColumns;
    }

    public double getPrefColumnWidth(String columnName) {
        return columnWidths.getOrDefault(columnName, BibtexSingleField.DEFAULT_FIELD_LENGTH);
    }

    public Map<String, SortType> getSortTypesForColumns() {
        return columnSortType;
    }
}
