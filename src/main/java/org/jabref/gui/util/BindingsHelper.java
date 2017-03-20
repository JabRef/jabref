package org.jabref.gui.util;

import java.util.function.Predicate;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.value.ObservableValue;
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
}
