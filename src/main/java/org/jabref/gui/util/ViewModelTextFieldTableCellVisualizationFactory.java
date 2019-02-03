package org.jabref.gui.util;

import java.util.function.Function;

import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;

import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class ViewModelTextFieldTableCellVisualizationFactory<S, T> implements Callback<TableColumn<S, T>, TextFieldTableCell<S, T>> {

    private ControlsFxVisualizer visualizer;
    private Function<S, ValidationStatus> val;

    @Override
    public TextFieldTableCell<S, T> call(TableColumn<S, T> param) {
        return new TextFieldTableCell<S, T>() {

            @Override
            public void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (!empty && (getTableRow() != null)) {
                    Object rowItem = getTableRow().getItem();

                    if (rowItem != null) {
                        S vm = (S) rowItem;
                        visualizer.initVisualization(val.apply(vm), this);
                    }
                }
            }
        };

    }

}
