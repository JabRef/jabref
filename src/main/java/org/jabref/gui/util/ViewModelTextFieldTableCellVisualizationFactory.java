package org.jabref.gui.util;

import java.util.function.Function;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
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
        return new TextFieldTableCell<>(stringConverter) {

            ChangeListener<? super Boolean> lostFocusListener;

            @Override
            public void startEdit() {
                super.startEdit();

                // The TextField-node is lazily created and not already present when a TableCell is created.
                // As 'textfield' is a private member of TextFieldTableCell we need need to adress it by the backdoor.
                Node node = getGraphic();
                if (node instanceof TextField) {
                    // We want to have the focus on the TextField, even if the TableView is set into edit mode by
                    // another action. As the TextField needs time to be created, we need to put that in the queue.
                    Platform.runLater(node::requestFocus);

                    // If the user clicks somewhere else, the changes made are not committed by default. The Listener is
                    // first removed, if it exists, and afterwards added, so we don't produce multiple calls, as the
                    // the list of change listeners don't check for uniqueness of listeners.
                    lostFocusListener = (observable, oldValue, newValue) -> {
                        if (!newValue) {
                            commitEdit(getConverter().fromString(((TextField) node).getText()));
                        }
                    };

                    node.focusedProperty().removeListener(lostFocusListener);
                    node.focusedProperty().addListener(lostFocusListener);
                }
            }

            @Override
            public void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (!empty && (getTableRow() != null)) {
                    S viewModel = getTableRow().getItem();

                    if (viewModel != null && visualizer != null && validationStatusProperty != null) {
                        // FixMe: Visualization icon is buggy for tablecells, we should use a pseudoclass instead and
                        //  a tooltip instead.
                        visualizer.initVisualization(validationStatusProperty.apply(viewModel), this);
                    }
                }
            }
        };
    }
}
