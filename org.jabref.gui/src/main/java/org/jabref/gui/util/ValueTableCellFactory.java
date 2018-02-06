package org.jabref.gui.util;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

import org.jabref.model.strings.StringUtil;

/**
 * Constructs a {@link TableCell} based on the value of the cell and a bunch of specified converter methods.
 *
 * @param <S> view model
 * @param <T> cell value
 */
public class ValueTableCellFactory<S, T> implements Callback<TableColumn<S, T>, TableCell<S, T>> {

    private Callback<T, String> toText;
    private Callback<T, Node> toGraphic;
    private Callback<T, EventHandler<? super MouseEvent>> toOnMouseClickedEvent;
    private Callback<T, String> toTooltip;

    public ValueTableCellFactory<S, T> withText(Callback<T, String> toText) {
        this.toText = toText;
        return this;
    }

    public ValueTableCellFactory<S, T> withGraphic(Callback<T, Node> toGraphic) {
        this.toGraphic = toGraphic;
        return this;
    }

    public ValueTableCellFactory<S, T> withTooltip(Callback<T, String> toTooltip) {
        this.toTooltip = toTooltip;
        return this;
    }

    public ValueTableCellFactory<S, T> withOnMouseClickedEvent(
            Callback<T, EventHandler<? super MouseEvent>> toOnMouseClickedEvent) {
        this.toOnMouseClickedEvent = toOnMouseClickedEvent;
        return this;
    }

    @Override
    public TableCell<S, T> call(TableColumn<S, T> param) {

        return new TableCell<S, T>() {

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || (item == null)) {
                    setText(null);
                    setGraphic(null);
                    setOnMouseClicked(null);
                    setTooltip(null);
                } else {
                    if (toText != null) {
                        setText(toText.call(item));
                    }
                    if (toGraphic != null) {
                        setGraphic(toGraphic.call(item));
                    }
                    if (toTooltip != null) {
                        String tooltipText = toTooltip.call(item);
                        if (StringUtil.isNotBlank(tooltipText)) {
                            setTooltip(new Tooltip(tooltipText));
                        }
                    }
                    if (toOnMouseClickedEvent != null) {
                        setOnMouseClicked(toOnMouseClickedEvent.call(item));
                    }
                }
            }
        };
    }
}
