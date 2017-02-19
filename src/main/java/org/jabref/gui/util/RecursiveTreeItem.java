package org.jabref.gui.util;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.util.Callback;

/**
 * Taken from https://gist.github.com/lestard/011e9ed4433f9eb791a8
 */
public class RecursiveTreeItem<T> extends TreeItem<T> {

    private final Callback<T, BooleanProperty> expandedProperty;
    private Callback<T, ObservableList<T>> childrenFactory;

    public RecursiveTreeItem(final T value, Callback<T, ObservableList<T>> func) {
        this(value, func, null);
    }

    public RecursiveTreeItem(final T value, Callback<T, ObservableList<T>> func, Callback<T, BooleanProperty> expandedProperty) {
        this(value, (Node) null, func, expandedProperty);
    }

    public RecursiveTreeItem(final T value, Node graphic, Callback<T, ObservableList<T>> func, Callback<T, BooleanProperty> expandedProperty) {
        super(value, graphic);

        this.childrenFactory = func;
        this.expandedProperty = expandedProperty;

        if(value != null) {
            addChildrenListener(value);
            bindExpandedProperty(value, expandedProperty);
        }

        valueProperty().addListener((obs, oldValue, newValue)->{
            if(newValue != null){
                addChildrenListener(newValue);
                bindExpandedProperty(value, expandedProperty);
            }
        });
    }

    private void bindExpandedProperty(T value, Callback<T, BooleanProperty> expandedProperty) {
        if (expandedProperty != null) {
            expandedProperty().bindBidirectional(expandedProperty.call(value));
        }
    }

    private void addChildrenListener(T value){
        final ObservableList<T> children = childrenFactory.call(value);

        children.forEach(child -> RecursiveTreeItem.this.getChildren().add(new RecursiveTreeItem<>(child, getGraphic(), childrenFactory, expandedProperty)));

        children.addListener((ListChangeListener<T>) change -> {
            while(change.next()){

                if(change.wasAdded()){
                    change.getAddedSubList().forEach(t -> RecursiveTreeItem.this.getChildren().add(new RecursiveTreeItem<>(t, getGraphic(), childrenFactory, expandedProperty)));
                }

                if(change.wasRemoved()){
                    change.getRemoved().forEach(t->{
                        final List<TreeItem<T>> itemsToRemove = RecursiveTreeItem.this.getChildren().stream().filter(treeItem -> treeItem.getValue().equals(t)).collect(Collectors.toList());

                        RecursiveTreeItem.this.getChildren().removeAll(itemsToRemove);
                    });
                }

            }
        });
    }
}
