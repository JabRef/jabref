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

    private final ObjectProperty<MainTableColumnModel> columnModel;
    private final DoubleProperty length = new SimpleDoubleProperty(ColumnPreferences.DEFAULT_FIELD_LENGTH);

    public TableColumnsItemModel(MainTableColumnModel columnModel) {
        this.columnModel = new SimpleObjectProperty<>(columnModel);
    }

    public TableColumnsItemModel(MainTableColumnModel columnModel, double length) {
        this(columnModel);
        this.length.setValue(length);
    }

    public MainTableColumnModel getColumnModel() { return columnModel.get(); }

    public ObservableValue<MainTableColumnModel> columnModelProperty() { return this.columnModel; }

    public void setLength(double length) { this.length.set(length); }

    public double getLength() { return length.get(); }

    @Override
    public int hashCode() { return Objects.hash(columnModel); }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TableColumnsItemModel) {
            return Objects.equals(this.columnModel, ((TableColumnsItemModel) obj).columnModel);
        } else {
            return false;
        }
    }
}
