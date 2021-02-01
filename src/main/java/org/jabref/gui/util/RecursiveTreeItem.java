package org.jabref.gui.util;

import java.util.function.Predicate;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.util.Callback;

import com.tobiasdiez.easybind.EasyBind;

/**
 * @implNote Taken from https://gist.github.com/lestard/011e9ed4433f9eb791a8
 * @implNote As CheckBoxTreeItem extends TreeItem, this class will work for both.
 */
public class RecursiveTreeItem<T> extends CheckBoxTreeItem<T> {

    private final Callback<T, BooleanProperty> expandedProperty;
    private final Callback<T, ObservableList<T>> childrenFactory;
    private final ObjectProperty<Predicate<T>> filter = new SimpleObjectProperty<>();
    private FilteredList<RecursiveTreeItem<T>> children;

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

        valueProperty().addListener((obs, oldValue, newValue) -> {
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
        children = EasyBind.mapBacked(childrenFactory.call(value), child -> new RecursiveTreeItem<>(child, getGraphic(), childrenFactory, expandedProperty, filter))
                           .filtered(Bindings.createObjectBinding(() -> this::showNode, filter));

        Bindings.bindContent(getChildren(), children);
    }

    private boolean showNode(RecursiveTreeItem<T> node) {
        if (filter.get() == null) {
            return true;
        }

        if (filter.get().test(node.getValue())) {
            // Node is directly matched -> so show it
            return true;
        }

        // Are there children (or children of children...) that are matched? If yes we also need to show this node
        return node.children.getSource().stream().anyMatch(this::showNode);
    }
}
