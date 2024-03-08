package org.jabref.gui.util;

import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

import org.jabref.model.strings.StringUtil;

import org.reactfx.util.TriConsumer;

/**
 * Constructs a {@link TreeTableCell} based on the view model of the row and a bunch of specified converter methods.
 *
 * @param <S> view model
 */
public class ViewModelTableRowFactory<S> implements Callback<TableView<S>, TableRow<S>> {

    private BiConsumer<S, ? super MouseEvent> onMouseClickedEvent;
    private Function<S, ContextMenu> contextMenuFactory;
    private TriConsumer<TableRow<S>, S, ? super MouseEvent> toOnDragDetected;
    private TriConsumer<TableRow<S>, S, ? super DragEvent> toOnDragDropped;
    private BiConsumer<S, ? super DragEvent> toOnDragEntered;
    private TriConsumer<TableRow<S>, S, ? super DragEvent> toOnDragExited;
    private TriConsumer<TableRow<S>, S, ? super DragEvent> toOnDragOver;
    private TriConsumer<TableRow<S>, S, ? super MouseDragEvent> toOnMouseDragEntered;
    private Callback<S, String> toTooltip;

    public ViewModelTableRowFactory<S> withOnMouseClickedEvent(BiConsumer<S, ? super MouseEvent> onMouseClickedEvent) {
        this.onMouseClickedEvent = onMouseClickedEvent;
        return this;
    }

    public ViewModelTableRowFactory<S> withContextMenu(Function<S, ContextMenu> contextMenuFactory) {
        this.contextMenuFactory = contextMenuFactory;
        return this;
    }

    public ViewModelTableRowFactory<S> setOnDragDetected(TriConsumer<TableRow<S>, S, ? super MouseEvent> toOnDragDetected) {
        this.toOnDragDetected = toOnDragDetected;
        return this;
    }

    public ViewModelTableRowFactory<S> setOnDragDetected(BiConsumer<S, ? super MouseEvent> toOnDragDetected) {
        this.toOnDragDetected = (row, viewModel, event) -> toOnDragDetected.accept(viewModel, event);
        return this;
    }

    public ViewModelTableRowFactory<S> setOnDragDropped(TriConsumer<TableRow<S>, S, ? super DragEvent> toOnDragDropped) {
        this.toOnDragDropped = toOnDragDropped;
        return this;
    }

    public ViewModelTableRowFactory<S> setOnDragDropped(BiConsumer<S, ? super DragEvent> toOnDragDropped) {
        return setOnDragDropped((row, viewModel, event) -> toOnDragDropped.accept(viewModel, event));
    }

    public ViewModelTableRowFactory<S> setOnDragEntered(BiConsumer<S, ? super DragEvent> toOnDragEntered) {
        this.toOnDragEntered = toOnDragEntered;
        return this;
    }

    public ViewModelTableRowFactory<S> setOnMouseDragEntered(TriConsumer<TableRow<S>, S, ? super MouseDragEvent> toOnDragEntered) {
        this.toOnMouseDragEntered = toOnDragEntered;
        return this;
    }

    public ViewModelTableRowFactory<S> setOnMouseDragEntered(BiConsumer<S, ? super MouseDragEvent> toOnDragEntered) {
        return setOnMouseDragEntered((row, viewModel, event) -> toOnDragEntered.accept(viewModel, event));
    }

    public ViewModelTableRowFactory<S> setOnDragExited(TriConsumer<TableRow<S>, S, ? super DragEvent> toOnDragExited) {
        this.toOnDragExited = toOnDragExited;
        return this;
    }

    public ViewModelTableRowFactory<S> setOnDragExited(BiConsumer<S, ? super DragEvent> toOnDragExited) {
        return setOnDragExited((row, viewModel, event) -> toOnDragExited.accept(viewModel, event));
    }

    public ViewModelTableRowFactory<S> setOnDragOver(TriConsumer<TableRow<S>, S, ? super DragEvent> toOnDragOver) {
        this.toOnDragOver = toOnDragOver;
        return this;
    }

    public ViewModelTableRowFactory<S> setOnDragOver(BiConsumer<S, ? super DragEvent> toOnDragOver) {
        return setOnDragOver((row, viewModel, event) -> toOnDragOver.accept(viewModel, event));
    }

    public ViewModelTableRowFactory<S> withTooltip(Callback<S, String> toTooltip) {
        this.toTooltip = toTooltip;
        return this;
    }

    @Override
    public TableRow<S> call(TableView<S> tableView) {
        TableRow<S> row = new TableRow<>();

        if (toTooltip != null) {
            String tooltipText = toTooltip.call(row.getItem());
            if (StringUtil.isNotBlank(tooltipText)) {
                row.setTooltip(new Tooltip(tooltipText));
            }
        }

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

            // Activate context menu if user presses the "context menu" key
            tableView.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
                boolean rowFocused = !row.isEmpty() && tableView.getFocusModel().getFocusedIndex() == row.getIndex();
                if (event.getCode() == KeyCode.CONTEXT_MENU && rowFocused) {
                    // Get center of focused cell
                    Bounds anchorBounds = row.getBoundsInParent();
                    double x = anchorBounds.getMinX() + anchorBounds.getWidth() / 2;
                    double y = anchorBounds.getMinY() + anchorBounds.getHeight() / 2;
                    Point2D screenPosition = row.getParent().localToScreen(x, y);

                    if (row.getContextMenu() == null) {
                        row.setContextMenu(contextMenuFactory.apply(row.getItem()));
                    }
                    row.getContextMenu().show(row, screenPosition.getX(), screenPosition.getY());
                }
            });
        }

        if (toOnDragDetected != null) {
            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    toOnDragDetected.accept(row, row.getItem(), event);
                }
            });
        }
        if (toOnDragDropped != null) {
            row.setOnDragDropped(event -> {
                if (!row.isEmpty()) {
                    toOnDragDropped.accept(row, row.getItem(), event);
                }
            });
        }
        if (toOnDragEntered != null) {
            row.setOnDragEntered(event -> {
                if (!row.isEmpty()) {
                    toOnDragEntered.accept(row.getItem(), event);
                }
            });
        }
        if (toOnDragExited != null) {
            row.setOnDragExited(event -> {
                if (!row.isEmpty()) {
                    toOnDragExited.accept(row, row.getItem(), event);
                }
            });
        }
        if (toOnDragOver != null) {
            row.setOnDragOver(event -> {
                if (!row.isEmpty()) {
                    toOnDragOver.accept(row, row.getItem(), event);
                }
            });
        }

        if (toOnMouseDragEntered != null) {
            row.setOnMouseDragEntered(event -> {
                if (!row.isEmpty()) {
                    toOnMouseDragEntered.accept(row, row.getItem(), event);
                }
            });
        }
        return row;
    }

    public void install(TableView<S> table) {
        table.setRowFactory(this);
    }
}
