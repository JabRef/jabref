package org.jabref.logic.util;

import java.util.List;
import java.util.concurrent.Callable;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Utility methods for working with JavaFX {@link Observable} objects in non-GUI logic code.
///
/// GUI code should use `BindingsHelper` from `jabgui` instead.
public final class ObservablesHelper {
    private final static Logger LOGGER = LoggerFactory.getLogger(ObservablesHelper.class);

    private ObservablesHelper() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    public static <T> ObjectBinding<T> createObjectBinding(final Callable<T> func, final List<? extends Observable> dependencies) {
        return Bindings.createObjectBinding(func, dependencies.toArray(Observable[]::new));
    }

    public static <T> void onChange(List<? extends Observable> observables, Runnable... actions) {
        observables.forEach(obs -> obs.addListener(_ -> {
            for (var action : actions) {
                action.run();
            }
        }));
    }

    /// Creates an {@link ObjectBinding} that safely manages the lifecycle of {@link AutoCloseable} resources.
    ///
    /// Standard JavaFX bindings are lazy and stateless, which can lead to resource leaks when
    /// binding to heavy or stateful objects (like file streams, external processes, or specialized
    /// internal components). This utility creates a binding that retains a reference to its
    /// previously calculated value and guarantees that {@link AutoCloseable#close()} is invoked
    /// on the old value immediately before a new value is computed.
    ///
    ///
    /// **Lifecycle & Memory Management:**
    ///
    /// - **Lazy Evaluation:** Old resources are closed *only* when a new value is explicitly requested via `.get()` after invalidation.
    /// - **Disposal:** The active resource is automatically closed when {@link ObjectBinding#dispose()} is called.
    ///
    ///
    ///
    /// *Note:* Exceptions thrown during the closing of old resources or the computation of
    /// new ones are caught and logged to prevent crashing the JavaFX application thread.
    /// If computation fails, the binding will evaluate to `null`.
    ///
    /// @param <T>          The type of the closable resource, which must implement {@link AutoCloseable}.
    /// @param func         The factory {@link Callable} used to compute the new resource.
    /// @param dependencies The observables that this binding should listen to for invalidation.
    /// @return An `ObjectBinding<T>` that automatically closes previous instances upon recalculation or disposal.
    public static <T extends AutoCloseable> ObjectBinding<T> createClosableObjectBinding(final Callable<T> func, final Observable... dependencies) {
        return new ObjectBinding<>() {
            private T currentValue;

            {
                bind(dependencies);
            }

            @Override
            protected T computeValue() {
                if (currentValue != null) {
                    try {
                        currentValue.close();
                    } catch (Exception e) {
                        LOGGER.error("Exception while closing previous binding value", e);
                    }
                }

                try {
                    currentValue = func.call();
                    return currentValue;
                } catch (Exception e) {
                    LOGGER.error("Exception while evaluating binding", e);
                    currentValue = null;
                    return null;
                }
            }

            /// Calls {@link ObjectBinding#unbind(Observable...)} and closes the active resource.
            @Override
            public void dispose() {
                super.unbind(dependencies);

                if (currentValue != null) {
                    try {
                        currentValue.close();
                    } catch (Exception e) {
                        LOGGER.error("Exception while closing binding value on dispose", e);
                    }

                    currentValue = null; // Help garbage collection
                }
            }

            /// Returns an immutable list of the dependencies of this binding.

            @Override
            public ObservableList<?> getDependencies() {
                return ((dependencies == null) || (dependencies.length == 0)) ?
                       FXCollections.emptyObservableList()
                                                                              : (dependencies.length == 1) ?
                                                                                FXCollections.singletonObservableList(dependencies[0])
                                                                                                           : FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(dependencies));
            }
        };
    }

    public static <T extends AutoCloseable> ObjectBinding<T> createClosableObjectBinding(final Callable<T> func, final List<Property<?>> dependencies) {
        return createClosableObjectBinding(func, dependencies.toArray(Observable[]::new));
    }
}
