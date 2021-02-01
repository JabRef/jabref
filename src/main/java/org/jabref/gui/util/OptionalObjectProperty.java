package org.jabref.gui.util;

import java.util.Optional;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;

import com.tobiasdiez.easybind.PreboundBinding;

/**
 * Similar to {@link com.tobiasdiez.easybind.monadic.MonadicObservableValue}
 */
public class OptionalObjectProperty<T> extends SimpleObjectProperty<Optional<T>> {

    private OptionalObjectProperty(Optional<T> initialValue) {
        super(initialValue);
    }

    public static <T> OptionalObjectProperty<T> empty() {
        return new OptionalObjectProperty<>(Optional.empty());
    }

    /**
     * Returns a new ObservableValue that holds the value held by this
     * ObservableValue, or {@code other} when this ObservableValue is empty.
     */
    public ObjectBinding<T> orElse(T other) {
        return new PreboundBinding<T>(this) {
            @Override
            protected T computeValue() {
                return OptionalObjectProperty.this.getValue().orElse(other);
            }
        };
    }

    public BooleanExpression isPresent() {
        return BooleanExpression.booleanExpression(new PreboundBinding<Boolean>(this) {
            @Override
            protected Boolean computeValue() {
                return OptionalObjectProperty.this.getValue().isPresent();
            }
        });
    }
}
