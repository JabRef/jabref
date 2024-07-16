package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
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

import com.tobiasdiez.easybind.Subscription;
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
    private final Map<PseudoClass, Callback<S, ObservableValue<Boolean>>> pseudoClasses = new HashMap<>();

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

    public ViewModelTableRowFactory<S> withPseudoClass(PseudoClass pseudoClass, Callback<S, ObservableValue<Boolean>> toCondition) {
        this.pseudoClasses.putIfAbsent(pseudoClass, toCondition);
        return this;
    }

    @Override
    public TableRow<S> call(TableView<S> tableView) {
        return new TableRow<>() {
            final List<Subscription> subscriptions = new ArrayList<>();

            @Override
            protected void updateItem(S item, boolean empty) {
                super.updateItem(item, empty);

                // Remove previous subscriptions
                subscriptions.forEach(Subscription::unsubscribe);
                subscriptions.clear();

                if (contextMenuFactory != null) {
                    // We only create the context menu when really necessary
                    setOnContextMenuRequested(event -> {
                        if (!isEmpty()) {
                            setContextMenu(contextMenuFactory.apply(item));
                            getContextMenu().show(this, event.getScreenX(), event.getScreenY());
                        }
                        event.consume();
                    });

                    // Activate context menu if user presses the "context menu" key
                    tableView.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
                        boolean rowFocused = !isEmpty() && tableView.getFocusModel().getFocusedIndex() == getIndex();
                        if (event.getCode() == KeyCode.CONTEXT_MENU && rowFocused) {
                            // Get center of focused cell
                            Bounds anchorBounds = getBoundsInParent();
                            double x = anchorBounds.getMinX() + anchorBounds.getWidth() / 2;
                            double y = anchorBounds.getMinY() + anchorBounds.getHeight() / 2;
                            Point2D screenPosition = getParent().localToScreen(x, y);

                            if (getContextMenu() == null) {
                                setContextMenu(contextMenuFactory.apply(getItem()));
                            }
                            getContextMenu().show(this, screenPosition.getX(), screenPosition.getY());
                        }
                    });
                }

                if (empty || (getItem() == null)) {
                    pseudoClasses.forEach((pseudoClass, toCondition) -> pseudoClassStateChanged(pseudoClass, false));
                } else {
                    pseudoClasses.forEach((pseudoClass, toCondition) -> {
                        ObservableValue<Boolean> condition = toCondition.call(getItem());
                        subscriptions.add(BindingsHelper.includePseudoClassWhen(
                                this,
                                pseudoClass,
                                condition));
                    });
                }

                if (toTooltip != null) {
                    String tooltipText = toTooltip.call(getItem());
                    if (StringUtil.isNotBlank(tooltipText)) {
                        setTooltip(new Tooltip(tooltipText));
                    }
                }

                if (onMouseClickedEvent != null) {
                    setOnMouseClicked(event -> {
                        if (!isEmpty()) {
                            onMouseClickedEvent.accept(getItem(), event);
                        }
                    });
                }

                if (toOnDragDetected != null) {
                    setOnDragDetected(event -> {
                        if (!isEmpty()) {
                            toOnDragDetected.accept(this, getItem(), event);
                        }
                    });
                }

                if (toOnDragDropped != null) {
                    setOnDragDropped(event -> {
                        if (!isEmpty()) {
                            toOnDragDropped.accept(this, getItem(), event);
                        }
                    });
                }

                if (toOnDragEntered != null) {
                    setOnDragEntered(event -> {
                        if (!isEmpty()) {
                            toOnDragEntered.accept(getItem(), event);
                        }
                    });
                }

                if (toOnDragExited != null) {
                    setOnDragExited(event -> {
                        if (!isEmpty()) {
                            toOnDragExited.accept(this, getItem(), event);
                        }
                    });
                }

                if (toOnDragOver != null) {
                    setOnDragOver(event -> {
                        if (!isEmpty()) {
                            toOnDragOver.accept(this, getItem(), event);
                        }
                    });
                }

                if (toOnMouseDragEntered != null) {
                    setOnMouseDragEntered(event -> {
                        if (!isEmpty()) {
                            toOnMouseDragEntered.accept(this, getItem(), event);
                        }
                    });
                }
            }
        };
    }

    public void install(TableView<S> table) {
        table.setRowFactory(this);
    }
}
