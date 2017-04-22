package org.jabref.gui.util;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ListProperty;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;

/**
 * Helper methods for javafx binding.
 * Some methods are taken from https://bugs.openjdk.java.net/browse/JDK-8134679
 */
public class BindingsHelper {

    private BindingsHelper() {
    }

    public static <T> BooleanBinding any(ObservableList<T> source, Predicate<T> predicate) {
        return Bindings.createBooleanBinding(() -> source.stream().anyMatch(predicate), source);
    }

    public static <T> BooleanBinding all(ObservableList<T> source, Predicate<T> predicate) {
        // Stream.allMatch() (in contrast to Stream.anyMatch() returns 'true' for empty streams, so this has to be checked explicitly.
        return Bindings.createBooleanBinding(() -> !source.isEmpty() && source.stream().allMatch(predicate), source);
    }

    public static void includePseudoClassWhen(Node node, PseudoClass pseudoClass, ObservableValue<? extends Boolean> condition) {
        BooleanProperty pseudoClassState = new BooleanPropertyBase(false) {
            @Override
            protected void invalidated() {
                node.pseudoClassStateChanged(pseudoClass, get());
            }

            @Override
            public Object getBean() {
                return node;
            }

            @Override
            public String getName() {
                return pseudoClass.getPseudoClassName();
            }
        };
        pseudoClassState.bind(condition);
    }

    /**
     * Binds propertA bidirectional to propertyB while using updateB to update propertyB when propertyA changed.
     */
    public static <A> void bindBidirectional(Property<A> propertyA, ObservableValue<A> propertyB, Consumer<A> updateB) {
        final BidirectionalBinding<A> binding = new BidirectionalBinding<>(propertyA, propertyB, updateB);

        // use updateB as initial source
        propertyA.setValue(propertyB.getValue());

        propertyA.addListener(binding);
        propertyB.addListener(binding);
    }

    public static <A, B> void bindContentBidirectional(ListProperty<A> listProperty, Property<B> property, Function<List<A>, B> mapToB, Function<B, List<A>> mapToList) {
        final BidirectionalListBinding<A, B> binding = new BidirectionalListBinding<>(listProperty, property, mapToB, mapToList);

        // use property as initial source
        listProperty.setAll(mapToList.apply(property.getValue()));

        listProperty.addListener(binding);
        property.addListener(binding);
    }

    private static class BidirectionalBinding<A> implements ChangeListener<A> {

        private final Property<A> propertyA;
        private final Consumer<A> updateB;
        private boolean updating = false;

        public BidirectionalBinding(Property<A> propertyA, ObservableValue<A> propertyB, Consumer<A> updateB) {
            this.propertyA = propertyA;
            this.updateB = updateB;
        }

        @Override
        public void changed(ObservableValue<? extends A> observable, A oldValue, A newValue) {
            if (!updating) {
                try {
                    updating = true;
                    if (observable == propertyA) {
                        updateB.accept(newValue);
                    } else {
                        propertyA.setValue(newValue);
                    }
                } finally {
                    updating = false;
                }
            }
        }
    }

    private static class BidirectionalListBinding<A, B> implements ListChangeListener<A>, ChangeListener<B> {

        private final ListProperty<A> listProperty;
        private final Property<B> property;
        private final Function<List<A>, B> mapToB;
        private final Function<B, List<A>> mapToList;
        private boolean updating = false;

        public BidirectionalListBinding(ListProperty<A> listProperty, Property<B> property, Function<List<A>, B> mapToB, Function<B, List<A>> mapToList) {
            this.listProperty = listProperty;
            this.property = property;
            this.mapToB = mapToB;
            this.mapToList = mapToList;
        }

        @Override
        public void changed(ObservableValue<? extends B> observable, B oldValue, B newValue) {
            if (!updating) {
                try {
                    updating = true;
                    listProperty.setAll(mapToList.apply(newValue));
                } finally {
                    updating = false;
                }
            }
        }

        @Override
        public void onChanged(Change<? extends A> c) {
            if (!updating) {
                try {
                    updating = true;
                    property.setValue(mapToB.apply(listProperty.getValue()));
                } finally {
                    updating = false;
                }
            }
        }
    }
}
