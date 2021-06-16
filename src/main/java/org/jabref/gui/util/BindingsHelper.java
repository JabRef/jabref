package org.jabref.gui.util;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.css.PseudoClass;
import javafx.scene.Node;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.PreboundBinding;
import com.tobiasdiez.easybind.Subscription;

/**
 * Helper methods for javafx binding. Some methods are taken from https://bugs.openjdk.java.net/browse/JDK-8134679
 */
public class BindingsHelper {

    private BindingsHelper() {
    }

    public static Subscription includePseudoClassWhen(Node node, PseudoClass pseudoClass, ObservableValue<? extends Boolean> condition) {
        Consumer<Boolean> changePseudoClass = value -> node.pseudoClassStateChanged(pseudoClass, value);
        Subscription subscription = EasyBind.subscribe(condition, changePseudoClass);

        // Put the pseudo class there depending on the current value
        changePseudoClass.accept(condition.getValue());
        return subscription;
    }

    public static <T, U> ObservableList<U> map(ObservableValue<T> source, Function<T, List<U>> mapper) {
        PreboundBinding<List<U>> binding = new PreboundBinding<>(source) {

            @Override
            protected List<U> computeValue() {
                return mapper.apply(source.getValue());
            }
        };

        ObservableList<U> list = FXCollections.observableArrayList();
        binding.addListener((observable, oldValue, newValue) -> list.setAll(newValue));
        return list;
    }

    /**
     * Binds propertyA bidirectional to propertyB using the provided map functions to convert between them.
     */
    public static <A, B> void bindBidirectional(Property<A> propertyA, Property<B> propertyB, Function<A, B> mapAtoB, Function<B, A> mapBtoA) {
        Consumer<B> updateA = newValueB -> propertyA.setValue(mapBtoA.apply(newValueB));
        Consumer<A> updateB = newValueA -> propertyB.setValue(mapAtoB.apply(newValueA));
        bindBidirectional(propertyA, propertyB, updateA, updateB);
    }

    /**
     * Binds propertyA bidirectional to propertyB while using updateB to update propertyB when propertyA changed.
     */
    public static <A> void bindBidirectional(Property<A> propertyA, ObservableValue<A> propertyB, Consumer<A> updateB) {
        bindBidirectional(propertyA, propertyB, propertyA::setValue, updateB);
    }

    /**
     * Binds propertyA bidirectional to propertyB using updateB to update propertyB when propertyA changed and similar for updateA.
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

    public static <T> ObservableValue<T> constantOf(T value) {
        return new ObjectBinding<>() {

            @Override
            protected T computeValue() {
                return value;
            }
        };
    }

    public static ObservableValue<Boolean> constantOf(boolean value) {
        return new BooleanBinding() {

            @Override
            protected boolean computeValue() {
                return value;
            }
        };
    }

    public static ObservableValue<? extends String> emptyString() {
        return new StringBinding() {

            @Override
            protected String computeValue() {
                return "";
            }
        };
    }

    /**
     * Returns a wrapper around the given list that posts changes on the JavaFX thread.
     */
    public static <T> ObservableList<T> forUI(ObservableList<T> list) {
        return new UiThreadList<>(list);
    }

    public static <T> ObservableValue<T> ifThenElse(ObservableValue<Boolean> condition, T value, T other) {
        return EasyBind.map(condition, conditionValue -> {
            if (conditionValue) {
                return value;
            } else {
                return other;
            }
        });
    }

    /**
     * Invokes {@code subscriber} for the every new value of {@code observable}, but not for the current value.
     *
     * @param observable observable value to subscribe to
     * @param subscriber action to invoke for values of {@code observable}.
     * @return a subscription that can be used to stop invoking subscriber for any further {@code observable} changes.
     * @apiNote {@link EasyBind#subscribe(ObservableValue, Consumer)} is similar but also invokes the {@code subscriber} for the current value
     */
    public static <T> Subscription subscribeFuture(ObservableValue<T> observable, Consumer<? super T> subscriber) {
        ChangeListener<? super T> listener = (obs, oldValue, newValue) -> subscriber.accept(newValue);
        observable.addListener(listener);
        return () -> observable.removeListener(listener);
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
