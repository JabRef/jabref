package org.jabref.gui.util;

import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.scene.control.ContextMenu;
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
    private Function<S, ContextMenu> contextMenuFactory;

    public ViewModelTableRowFactory<S> withOnMouseClickedEvent(BiConsumer<S, ? super MouseEvent> onMouseClickedEvent) {
        this.onMouseClickedEvent = onMouseClickedEvent;
        return this;
    }

    public ViewModelTableRowFactory<S> withContextMenu(Function<S, ContextMenu> contextMenuFactory) {
        this.contextMenuFactory = contextMenuFactory;
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

        if (contextMenuFactory != null) {
            // We only create the context menu when really necessary
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    row.setContextMenu(contextMenuFactory.apply(row.getItem()));
                    row.getContextMenu().show(row, event.getScreenX(), event.getScreenY());
                }
                event.consume();
            });
        }

        return row;
    }
}
