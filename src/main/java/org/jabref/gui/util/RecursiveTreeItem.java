package org.jabref.gui.util;

import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.util.Callback;

/**
 * Taken from https://gist.github.com/lestard/011e9ed4433f9eb791a8
 */
public class RecursiveTreeItem<T> extends TreeItem<T> {

    private Callback<T, ObservableList<T>> childrenFactory;

    public RecursiveTreeItem(Callback<T, ObservableList<T>> func){
        this(null, func);
    }

    public RecursiveTreeItem(final T value, Callback<T, ObservableList<T>> func){
        this(value, (Node) null, func);
    }

    public RecursiveTreeItem(final T value, Node graphic, Callback<T, ObservableList<T>> func){
        super(value, graphic);

        this.childrenFactory = func;

        if(value != null) {
            addChildrenListener(value);
        }

        valueProperty().addListener((obs, oldValue, newValue)->{
            if(newValue != null){
                addChildrenListener(newValue);
            }
        });
    }

    private void addChildrenListener(T value){
        final ObservableList<T> children = childrenFactory.call(value);

        children.forEach(child ->  RecursiveTreeItem.this.getChildren().add(new RecursiveTreeItem<>(child, getGraphic(), childrenFactory)));

        children.addListener((ListChangeListener<T>) change -> {
            while(change.next()){

                if(change.wasAdded()){
                    change.getAddedSubList().forEach(t-> RecursiveTreeItem.this.getChildren().add(new RecursiveTreeItem<>(t, getGraphic(), childrenFactory)));
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
