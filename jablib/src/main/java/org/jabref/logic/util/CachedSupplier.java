package org.jabref.logic.util;

import java.util.function.Supplier;

/// Utility methods for creating cached suppliers.
public class CachedSupplier {

    private static final Object UNINITIALIZED = new Object();

    private CachedSupplier() {
    }

    /// Returns a supplier that computes the value once and returns the same value for all subsequent calls.
    public static <T> Supplier<T> memoize(Supplier<T> delegate) {
        return new Supplier<>() {
            private Object value = UNINITIALIZED;

            @Override
            @SuppressWarnings("unchecked")
            public synchronized T get() {
                if (value == UNINITIALIZED) {
                    value = delegate.get();
                }

                return (T) value;
            }
        };
    }
}
