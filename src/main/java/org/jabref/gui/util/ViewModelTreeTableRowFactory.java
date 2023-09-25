package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

import com.tobiasdiez.easybind.Subscription;
import org.reactfx.util.TriConsumer;

public class ViewModelTreeTableRowFactory<S> implements Callback<TreeTableView<S>, TreeTableRow<S>> {
    private BiConsumer<S, ? super MouseEvent> onMouseClickedEvent;
    private BiConsumer<S, ? super MouseEvent> onMousePressedEvent;
    private Consumer<TreeTableRow<S>> toCustomInitializer;
    private Function<S, ContextMenu> contextMenuFactory;
    private TriConsumer<TreeTableRow<S>, S, ? super MouseEvent> toOnDragDetected;
    private TriConsumer<TreeTableRow<S>, S, ? super DragEvent> toOnDragDropped;
    private BiConsumer<S, ? super DragEvent> toOnDragEntered;
    private TriConsumer<TreeTableRow<S>, S, ? super DragEvent> toOnDragExited;
    private TriConsumer<TreeTableRow<S>, S, ? super DragEvent> toOnDragOver;
    private TriConsumer<TreeTableRow<S>, S, ? super MouseDragEvent> toOnMouseDragEntered;
    private final Map<EventType<? extends Event>, BiConsumer<S, ? super Event>> eventFilters = new HashMap<>();
    private final Map<PseudoClass, Callback<TreeTableRow<S>, ObservableValue<Boolean>>> pseudoClasses = new HashMap<>();

    public ViewModelTreeTableRowFactory<S> withOnMouseClickedEvent(BiConsumer<S, ? super MouseEvent> event) {
        this.onMouseClickedEvent = event;
        return this;
    }

    public ViewModelTreeTableRowFactory<S> withOnMousePressedEvent(BiConsumer<S, ? super MouseEvent> event) {
        this.onMousePressedEvent = event;
        return this;
    }

    public ViewModelTreeTableRowFactory<S> withCustomInitializer(Consumer<TreeTableRow<S>> customInitializer) {
        this.toCustomInitializer = customInitializer;
        return this;
    }

    public ViewModelTreeTableRowFactory<S> withContextMenu(Function<S, ContextMenu> contextMenuFactory) {
        this.contextMenuFactory = contextMenuFactory;
        return this;
    }

    public ViewModelTreeTableRowFactory<S> setOnDragDetected(TriConsumer<TreeTableRow<S>, S, ? super MouseEvent> toOnDragDetected) {
        this.toOnDragDetected = toOnDragDetected;
        return this;
    }

    public ViewModelTreeTableRowFactory<S> setOnDragDetected(BiConsumer<S, ? super MouseEvent> toOnDragDetected) {
        this.toOnDragDetected = (row, viewModel, event) -> toOnDragDetected.accept(viewModel, event);
        return this;
    }

    public ViewModelTreeTableRowFactory<S> setOnDragDropped(TriConsumer<TreeTableRow<S>, S, ? super DragEvent> toOnDragDropped) {
        this.toOnDragDropped = toOnDragDropped;
        return this;
    }

    public ViewModelTreeTableRowFactory<S> setOnDragDropped(BiConsumer<S, ? super DragEvent> toOnDragDropped) {
        return setOnDragDropped((row, viewModel, event) -> toOnDragDropped.accept(viewModel, event));
    }

    public ViewModelTreeTableRowFactory<S> setOnDragEntered(BiConsumer<S, ? super DragEvent> toOnDragEntered) {
        this.toOnDragEntered = toOnDragEntered;
        return this;
    }

    public ViewModelTreeTableRowFactory<S> setOnMouseDragEntered(TriConsumer<TreeTableRow<S>, S, ? super MouseDragEvent> toOnDragEntered) {
        this.toOnMouseDragEntered = toOnDragEntered;
        return this;
    }

    public ViewModelTreeTableRowFactory<S> setOnMouseDragEntered(BiConsumer<S, ? super MouseDragEvent> toOnDragEntered) {
        return setOnMouseDragEntered((row, viewModel, event) -> toOnDragEntered.accept(viewModel, event));
    }

    public ViewModelTreeTableRowFactory<S> setOnDragExited(TriConsumer<TreeTableRow<S>, S, ? super DragEvent> toOnDragExited) {
        this.toOnDragExited = toOnDragExited;
        return this;
    }

    public ViewModelTreeTableRowFactory<S> setOnDragExited(BiConsumer<S, ? super DragEvent> toOnDragExited) {
        return setOnDragExited((row, viewModel, event) -> toOnDragExited.accept(viewModel, event));
    }

    public ViewModelTreeTableRowFactory<S> setOnDragOver(TriConsumer<TreeTableRow<S>, S, ? super DragEvent> toOnDragOver) {
        this.toOnDragOver = toOnDragOver;
        return this;
    }

    public ViewModelTreeTableRowFactory<S> setOnDragOver(BiConsumer<S, ? super DragEvent> toOnDragOver) {
        return setOnDragOver((row, viewModel, event) -> toOnDragOver.accept(viewModel, event));
    }

    public ViewModelTreeTableRowFactory<S> withPseudoClass(PseudoClass pseudoClass, Callback<TreeTableRow<S>, ObservableValue<Boolean>> toCondition) {
        this.pseudoClasses.putIfAbsent(pseudoClass, toCondition);
        return this;
    }

    public ViewModelTreeTableRowFactory<S> withEventFilter(EventType<? extends Event> event, BiConsumer<S, ? super Event> toCondition) {
        this.eventFilters.putIfAbsent(event, toCondition);
        return this;
    }

    public void install(TreeTableView<S> table) {
        table.setRowFactory(this);
    }

    @Override
    public TreeTableRow<S> call(TreeTableView<S> treeTableView) {
        return new TreeTableRow<>() {
            final List<Subscription> subscriptions = new ArrayList<>();

            @Override
            protected void updateItem(S row, boolean empty) {
                super.updateItem(row, empty);

                // Remove previous subscriptions
                subscriptions.forEach(Subscription::unsubscribe);
                subscriptions.clear();

                if (contextMenuFactory != null) {
                    // We only create the context menu when really necessary
                    setOnContextMenuRequested(event -> {
                        if (!isEmpty()) {
                            setContextMenu(contextMenuFactory.apply(row));
                            getContextMenu().show(this, event.getScreenX(), event.getScreenY());
                        }
                        event.consume();
                    });

                    // Activate context menu if user presses the "context menu" key
                    treeTableView.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
                        boolean rowFocused = isEmpty() && treeTableView.getFocusModel().getFocusedIndex() == getIndex();
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

                if (!empty && (row != null)) {
                    if (onMouseClickedEvent != null) {
                        setOnMouseClicked(event -> onMouseClickedEvent.accept(getItem(), event));
                    }

                    if (onMousePressedEvent != null) {
                        setOnMousePressed(event -> onMousePressedEvent.accept(getItem(), event));
                    }

                    if (toCustomInitializer != null) {
                        toCustomInitializer.accept(this);
                    }

                    if (toOnDragDetected != null) {
                        setOnDragDetected(event -> toOnDragDetected.accept(this, getItem(), event));
                    }
                    if (toOnDragDropped != null) {
                        setOnDragDropped(event -> toOnDragDropped.accept(this, getItem(), event));
                    }
                    if (toOnDragEntered != null) {
                        setOnDragEntered(event -> toOnDragEntered.accept(getItem(), event));
                    }
                    if (toOnDragExited != null) {
                        setOnDragExited(event -> toOnDragExited.accept(this, getItem(), event));
                    }
                    if (toOnDragOver != null) {
                        setOnDragOver(event -> toOnDragOver.accept(this, getItem(), event));
                    }

                    if (toOnMouseDragEntered != null) {
                        setOnMouseDragEntered(event -> toOnMouseDragEntered.accept(this, getItem(), event));
                    }

                    for (Map.Entry<EventType<?>, BiConsumer<S, ? super Event>> eventFilter : eventFilters.entrySet()) {
                        addEventFilter(eventFilter.getKey(), event -> eventFilter.getValue().accept(getItem(), event));
                    }

                    for (Map.Entry<PseudoClass, Callback<TreeTableRow<S>, ObservableValue<Boolean>>> pseudoClassWithCondition : pseudoClasses.entrySet()) {
                        ObservableValue<Boolean> condition = pseudoClassWithCondition.getValue().call(this);
                        subscriptions.add(BindingsHelper.includePseudoClassWhen(
                                this,
                                pseudoClassWithCondition.getKey(),
                                condition));
                    }
                }
            }
        };
    }
}
