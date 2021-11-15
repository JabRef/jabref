package org.jabref.gui.maintable;

import java.util.List;

public interface AbstractColumnPreferences {

    public static final double DEFAULT_COLUMN_WIDTH = 100;
    public static final double ICON_COLUMN_WIDTH = 16 + 12; // add some additional space to improve appearance

    public List<MainTableColumnModel> getColumns();

    public List<MainTableColumnModel> getColumnSortOrder();
}
