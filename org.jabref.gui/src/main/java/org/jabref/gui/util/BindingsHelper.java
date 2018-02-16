package org.jabref.gui.util;

import java.util.List;
import java.util.Map;
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
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
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
     * Creates a new list in which each element is converted using the provided mapping.
     * All changes to the underlying list are propagated to the converted list.
     *
     * In contrast to {@link org.fxmisc.easybind.EasyBind#map(ObservableList, Function)},
     * the items are converted when the are inserted (and at the initialization) instead of when they are accessed.
     * Thus the initial CPU overhead and memory consumption is higher but the access to list items is quicker.
     */
    public static <A, B> MappedList<B, A> mapBacked(ObservableList<A> source, Function<A, B> mapper) {
        return new MappedList<>(source, mapper);
    }

    /**
     * Binds propertA bidirectional to propertyB using the provided map functions to convert between them.
     */
    public static <A, B> void bindBidirectional(Property<A> propertyA, Property<B> propertyB, Function<A, B> mapAtoB, Function<B, A> mapBtoA) {
        Consumer<B> updateA = newValueB -> propertyA.setValue(mapBtoA.apply(newValueB));
        Consumer<A> updateB = newValueA -> propertyB.setValue(mapAtoB.apply(newValueA));
        bindBidirectional(propertyA, propertyB, updateA, updateB);
    }

    /**
     * Binds propertA bidirectional to propertyB while using updateB to update propertyB when propertyA changed.
     */
    public static <A> void bindBidirectional(Property<A> propertyA, ObservableValue<A> propertyB, Consumer<A> updateB) {
        bindBidirectional(propertyA, propertyB, propertyA::setValue, updateB);
    }

    /**
     * Binds propertA bidirectional to propertyB using updateB to update propertyB when propertyA changed and similar
     * for updateA.
     */
    public static <A, B> void bindBidirectional(ObservableValue<A> propertyA, ObservableValue<B> propertyB, Consumer<B> updateA, Consumer<A> updateB) {
        final BidirectionalBinding<A, B> binding = new BidirectionalBinding<>(propertyA, propertyB, updateA, updateB);

        // use updateB as initial source
        updateA.accept(propertyB.getValue());

        propertyA.addListener(binding.getChangeListenerA());
        propertyB.addListener(binding.getChangeListenerB());
    }

    public static <A, B> void bindContentBidirectional(ObservableList<A> propertyA, ListProperty<B> propertyB, Consumer<ObservableList<B>> updateA, Consumer<List<A>> updateB) {
        bindContentBidirectional(
                propertyA,
                (ObservableValue<ObservableList<B>>) propertyB,
                updateA,
                updateB);
    }

    public static <A, B> void bindContentBidirectional(ObservableList<A> propertyA, ObservableValue<B> propertyB, Consumer<B> updateA, Consumer<List<A>> updateB) {
        final BidirectionalListBinding<A, B> binding = new BidirectionalListBinding<>(propertyA, propertyB, updateA, updateB);

        // use property as initial source
        updateA.accept(propertyB.getValue());

        propertyA.addListener(binding);
        propertyB.addListener(binding);
    }

    public static <A, B> void bindContentBidirectional(ListProperty<A> listProperty, Property<B> property, Function<List<A>, B> mapToB, Function<B, List<A>> mapToList) {
        Consumer<B> updateList = newValueB -> listProperty.setAll(mapToList.apply(newValueB));
        Consumer<List<A>> updateB = newValueList -> property.setValue(mapToB.apply(newValueList));

        bindContentBidirectional(
                listProperty,
                property,
                updateList,
                updateB);
    }

    public static <A, V, B> void bindContentBidirectional(ObservableMap<A, V> propertyA, ObservableValue<B> propertyB, Consumer<B> updateA, Consumer<Map<A, V>> updateB) {
        final BidirectionalMapBinding<A, V, B> binding = new BidirectionalMapBinding<>(propertyA, propertyB, updateA, updateB);

        // use list as initial source
        updateB.accept(propertyA);

        propertyA.addListener(binding);
        propertyB.addListener(binding);
    }

    public static <A, V, B> void bindContentBidirectional(ObservableMap<A, V> propertyA, Property<B> propertyB, Consumer<B> updateA, Function<Map<A, V>, B> mapToB) {
        Consumer<Map<A, V>> updateB = newValueList -> propertyB.setValue(mapToB.apply(newValueList));
        bindContentBidirectional(
                propertyA,
                propertyB,
                updateA,
                updateB);
    }

    private static class BidirectionalBinding<A, B> {

        private final ObservableValue<A> propertyA;
        private final Consumer<B> updateA;
        private final Consumer<A> updateB;
        private boolean updating = false;

        public BidirectionalBinding(ObservableValue<A> propertyA, ObservableValue<B> propertyB, Consumer<B> updateA, Consumer<A> updateB) {
            this.propertyA = propertyA;
            this.updateA = updateA;
            this.updateB = updateB;
        }

        public ChangeListener<? super A> getChangeListenerA() {
            return this::changedA;
        }

        public ChangeListener<? super B> getChangeListenerB() {
            return this::changedB;
        }

        public void changedA(ObservableValue<? extends A> observable, A oldValue, A newValue) {
            updateLocked(updateB, oldValue, newValue);
        }

        public void changedB(ObservableValue<? extends B> observable, B oldValue, B newValue) {
            updateLocked(updateA, oldValue, newValue);
        }

        private <T> void updateLocked(Consumer<T> update, T oldValue, T newValue) {
            if (!updating) {
                try {
                    updating = true;
                    update.accept(newValue);
                } finally {
                    updating = false;
                }
            }
        }
    }

    private static class BidirectionalListBinding<A, B> implements ListChangeListener<A>, ChangeListener<B> {

        private final ObservableList<A> listProperty;
        private final ObservableValue<B> property;
        private final Consumer<B> updateA;
        private final Consumer<List<A>> updateB;
        private boolean updating = false;

        public BidirectionalListBinding(ObservableList<A> listProperty, ObservableValue<B> property, Consumer<B> updateA, Consumer<List<A>> updateB) {
            this.listProperty = listProperty;
            this.property = property;
            this.updateA = updateA;
            this.updateB = updateB;
        }

        @Override
        public void changed(ObservableValue<? extends B> observable, B oldValue, B newValue) {
            if (!updating) {
                try {
                    updating = true;
                    updateA.accept(newValue);
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
                    updateB.accept(listProperty);
                } finally {
                    updating = false;
                }
            }
        }
    }

    private static class BidirectionalMapBinding<A, V, B> implements MapChangeListener<A, V>, ChangeListener<B> {

        private final ObservableMap<A, V> mapProperty;
        private final ObservableValue<B> property;
        private final Consumer<B> updateA;
        private final Consumer<Map<A, V>> updateB;
        private boolean updating = false;

        public BidirectionalMapBinding(ObservableMap<A, V> mapProperty, ObservableValue<B> property, Consumer<B> updateA, Consumer<Map<A, V>> updateB) {
            this.mapProperty = mapProperty;
            this.property = property;
            this.updateA = updateA;
            this.updateB = updateB;
        }

        @Override
        public void changed(ObservableValue<? extends B> observable, B oldValue, B newValue) {
            if (!updating) {
                try {
                    updating = true;
                    updateA.accept(newValue);
                } finally {
                    updating = false;
                }
            }
        }

        @Override
        public void onChanged(Change<? extends A, ? extends V> c) {
            if (!updating) {
                try {
                    updating = true;
                    updateB.accept(mapProperty);
                } finally {
                    updating = false;
                }
            }
        }
    }
}
