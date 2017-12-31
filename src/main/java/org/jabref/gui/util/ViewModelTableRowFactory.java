package org.jabref.gui.util;

import java.util.function.BiConsumer;

import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableCell;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

/**
 * Constructs a {@link TreeTableCell} based on the view model of the row and a bunch of specified converter methods.
 *
 * @param <S> view model
 */
public class ViewModelTableRowFactory<S> implements Callback<TableView<S>, TableRow<S>> {

    private BiConsumer<S, ? super MouseEvent> onMouseClickedEvent;

    public ViewModelTableRowFactory<S> withOnMouseClickedEvent(BiConsumer<S, ? super MouseEvent> onMouseClickedEvent) {
        this.onMouseClickedEvent = onMouseClickedEvent;
        return this;
    }

    @Override
    public TableRow<S> call(TableView<S> tableView) {
        TableRow<S> row = new TableRow<>();

        if (onMouseClickedEvent != null) {
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    onMouseClickedEvent.accept(row.getItem(), event);
                }
            });
        }

        return row;
    }
}
