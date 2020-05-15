package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.StringConverter;

import com.tobiasdiez.easybind.Subscription;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;

public class ViewModelTextFieldTableCellVisualizationFactory<S, T> implements Callback<TableColumn<S, T>, TableCell<S, T>> {

    private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");

    private Function<S, ValidationStatus> validationStatusProperty;
    private StringConverter<T> stringConverter;

    public ViewModelTextFieldTableCellVisualizationFactory<S, T> withValidation(Function<S, ValidationStatus> validationStatusProperty) {
        this.validationStatusProperty = validationStatusProperty;
        return this;
    }

    public void install(TableColumn<S, T> column, StringConverter<T> stringConverter) {
        column.setCellFactory(this);
        this.stringConverter = stringConverter;
    }

    @Override
    public TextFieldTableCell<S, T> call(TableColumn<S, T> param) {
        return new TextFieldTableCell<>(stringConverter) {

            final List<Subscription> subscriptions = new ArrayList<>();

            @Override
            public void startEdit() {
                super.startEdit();

                // The textfield is lazily created and not already present when a TableCell is created.
                lookupTextField().ifPresent(textField -> Platform.runLater(() -> {
                    textField.requestFocus();
                    textField.selectAll();
                }));
            }

            /**
             * As 'textfield' is a private member of TextFieldTableCell we need need to get to it through the backdoor.
             *
             * @return The TextField containing the editable content of the TableCell
             */
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

                subscriptions.forEach(Subscription::unsubscribe);
                subscriptions.clear();

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    setOnMouseClicked(null);
                    setTooltip(null);
                    pseudoClassStateChanged(INVALID_PSEUDO_CLASS, false);
                } else {
                    S viewModel = getTableRow().getItem();
                    if (validationStatusProperty != null) {
                        validationStatusProperty.apply(viewModel)
                                                .getHighestMessage()
                                                .ifPresent(message -> setTooltip(new Tooltip(message.getMessage())));

                        subscriptions.add(BindingsHelper.includePseudoClassWhen(
                                this,
                                INVALID_PSEUDO_CLASS,
                                validationStatusProperty.apply(viewModel).validProperty().not()));
                    }
                }
            }
        };
    }
}
