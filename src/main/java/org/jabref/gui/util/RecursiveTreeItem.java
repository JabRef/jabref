package org.jabref.gui.util;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.util.Callback;

/**
 * Taken from https://gist.github.com/lestard/011e9ed4433f9eb791a8
 */
public class RecursiveTreeItem<T> extends TreeItem<T> {

    private final Callback<T, BooleanProperty> expandedProperty;
    private Callback<T, ObservableList<T>> childrenFactory;
    private ObjectProperty<Predicate<T>> filter = new SimpleObjectProperty<>();
    private FilteredList<T> children;

    public RecursiveTreeItem(final T value, Callback<T, ObservableList<T>> func) {
        this(value, func, null, null);
    }

    public RecursiveTreeItem(final T value, Callback<T, ObservableList<T>> func, Callback<T, BooleanProperty> expandedProperty, ObservableValue<Predicate<T>> filter) {
        this(value, null, func, expandedProperty, filter);
    }

    public RecursiveTreeItem(final T value, Callback<T, ObservableList<T>> func, ObservableValue<Predicate<T>> filter) {
        this(value, null, func, null, filter);
    }

    private RecursiveTreeItem(final T value, Node graphic, Callback<T, ObservableList<T>> func, Callback<T, BooleanProperty> expandedProperty, ObservableValue<Predicate<T>> filter) {
        super(value, graphic);

        this.childrenFactory = func;
        this.expandedProperty = expandedProperty;
        if (filter != null) {
            this.filter.bind(filter);
        }

        if (value != null) {
            addChildrenListener(value);
            bindExpandedProperty(value, expandedProperty);
        }

        valueProperty().addListener((obs, oldValue, newValue)-> {
            if (newValue != null) {
                addChildrenListener(newValue);
                bindExpandedProperty(newValue, expandedProperty);
            }
        });
    }

    private void bindExpandedProperty(T value, Callback<T, BooleanProperty> expandedProperty) {
        if (expandedProperty != null) {
            expandedProperty().bindBidirectional(expandedProperty.call(value));
        }
    }

    private void addChildrenListener(T value) {
        children = new FilteredList<>(childrenFactory.call(value));
        children.predicateProperty().bind(Bindings.createObjectBinding(() -> this::showNode, filter));

        addAsChildren(children, 0);

        children.addListener((ListChangeListener<T>) change -> {
            while (change.next()) {

                if (change.wasRemoved()) {
                    change.getRemoved().forEach(t-> {
                        final List<TreeItem<T>> itemsToRemove = getChildren().stream().filter(treeItem -> treeItem.getValue().equals(t)).collect(Collectors.toList());
                        getChildren().removeAll(itemsToRemove);
                    });
                }

                if (change.wasAdded()) {
                    addAsChildren(change.getAddedSubList(), change.getFrom());
                }
            }
        });
    }

    private void addAsChildren(List<? extends T> children, int startIndex) {
        List<RecursiveTreeItem<T>> treeItems = children.stream().map(child -> new RecursiveTreeItem<>(child, getGraphic(), childrenFactory, expandedProperty, filter)).collect(Collectors.toList());
        getChildren().addAll(startIndex, treeItems);
    }

    private boolean showNode(T t) {
        if (filter.get() == null) {
            return true;
        }

        if (filter.get().test(t)) {
            // Node is directly matched -> so show it
            return true;
        }

        // Are there children (or children of children...) that are matched? If yes we also need to show this node
        return childrenFactory.call(t).stream().anyMatch(this::showNode);
    }
}
