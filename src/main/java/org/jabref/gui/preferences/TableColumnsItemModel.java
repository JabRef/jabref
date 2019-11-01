package org.jabref.gui.preferences;

import java.util.Objects;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnModel;

public class TableColumnsItemModel {

    private final ObjectProperty<MainTableColumnModel> columnName;
    private final DoubleProperty length = new SimpleDoubleProperty(ColumnPreferences.DEFAULT_FIELD_LENGTH);

    public TableColumnsItemModel(MainTableColumnModel columnName) {
        this.columnName = new SimpleObjectProperty<>(columnName);
    }

    public TableColumnsItemModel(MainTableColumnModel columnName, double length) {
        this(columnName);
        this.length.setValue(length);
    }

    public MainTableColumnModel getColumnName() { return columnName.get(); }

    public ObservableValue<MainTableColumnModel> columnNameProperty() { return this.columnName; }

    public void setLength(double length) { this.length.set(length); }

    public double getLength() { return length.get(); }

    @Override
    public int hashCode() { return Objects.hash(columnName); }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TableColumnsItemModel) {
            return Objects.equals(this.columnName, ((TableColumnsItemModel) obj).columnName);
        } else {
            return false;
        }
    }
}
