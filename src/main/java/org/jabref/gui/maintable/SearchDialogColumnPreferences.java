package org.jabref.gui.maintable;

import java.util.List;

public class SearchDialogColumnPreferences implements AbstractColumnPreferences {

    private final List<MainTableColumnModel> columns;
    private final List<MainTableColumnModel> columnSortOrder;

    public SearchDialogColumnPreferences(List<MainTableColumnModel> columns, List<MainTableColumnModel> columnSortOrder) {
        this.columns = columns;
        this.columnSortOrder = columnSortOrder;
    }

    @Override
    public List<MainTableColumnModel> getColumns() {
        return columns;
    }

    @Override
    public List<MainTableColumnModel> getColumnSortOrder() {
        return columnSortOrder;
    }
}
