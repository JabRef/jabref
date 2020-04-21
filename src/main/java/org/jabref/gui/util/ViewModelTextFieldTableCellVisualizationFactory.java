package org.jabref.gui.util;

import java.util.Optional;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
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

            @Override
            public void startEdit() {
                super.startEdit();

                // The textfield is lazily created and not already present when a TableCell is created.
                // As 'textfield' is a private member of TextFieldTableCell we need need to adress it by the backdoor.
                lookupTextField().ifPresent(textField -> Platform.runLater(() -> {
                    textField.requestFocus();
                    textField.selectAll();
                }));
            }

            private Optional<TextField> lookupTextField() {
                if (getGraphic() instanceof TextField) {
                    return Optional.of((TextField) getGraphic());
                } else {
                    // Could be an HBox with some graphic and a TextField if a graphic is specified for the TableCell
                    if (getGraphic() instanceof HBox) {
                        HBox hbox = (HBox) getGraphic();
                        if ((hbox.getChildren().size() > 1) && hbox.getChildren().get(1) instanceof TextField) {
                            return Optional.of((TextField) hbox.getChildren().get(1));
                        }
                    }
                    return Optional.empty();
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
