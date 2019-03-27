package org.jabref.gui.util;

import java.util.function.Function;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import javafx.util.StringConverter;

import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class ViewModelTextFieldTableCellVisualizationFactory<S, T> implements Callback<TableColumn<S, T>, TableCell<S, T>> {

    private ControlsFxVisualizer visualizer;
    private Function<S, ValidationStatus> validationStatusProperty;
    private StringConverter<T> stringConverter;

    public ViewModelTextFieldTableCellVisualizationFactory<S, T> withValidation(Function<S, ValidationStatus> validationStatusProperty, ControlsFxVisualizer visualizer) {
        this.validationStatusProperty = validationStatusProperty;
        this.visualizer = visualizer;
        return this;
    }

    public void install(TableColumn<S, T> column, StringConverter<T> stringConverter) {
        column.setCellFactory(this);
        this.stringConverter = stringConverter;
    }

    @Override
    public TextFieldTableCell<S, T> call(TableColumn<S, T> param) {
        return new TextFieldTableCell<S, T>(stringConverter) {

            @Override
            public void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (!empty && (getTableRow() != null)) {
                    Object rowItem = getTableRow().getItem();

                    if (rowItem != null) {
                        S vm = (S) rowItem;
                        if ((visualizer != null) && (validationStatusProperty != null)) {
                            visualizer.initVisualization(validationStatusProperty.apply(vm), this);
                        }
                    }
                }
            }
        };

    }

}
