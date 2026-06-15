package org.jabref.logic.util;

import java.util.function.Supplier;

/// A lazy value container. Uses a factory method [#supplier] to calculate the value only when needed (when [#get] is called),
/// and then it is stored.
public class LazyValue<T> {
    // Implementation details of the class:
    //
    // There are 3 ways to make it:
    // 1. A pair of `T value` and `boolean initialized`.
    // 2. (Chosen option) use a sentinel value.
    // 3. Use subclasses like `Empty` and `StoredValue<T>`.
    //
    // The problem boils down to null-safety and complexity.
    // The first option is the simplest and efficient one, but it has 2 types of `null` values: one for uninitialized
    // state, and the other is the return result of the factory method. However, `@Nullable T value` would mean that the
    // value could *always* be `null`. Which is false because the supplier may always return a non-null value. As a result,
    // options 2 and 3 should be used. However, option 3 is more complex, so option 2 was chosen, even if one has to use
    // an unchecked cast.

    /// Sentinel object used only to represent "not initialized".
    private static final Object UNINITIALIZED = new Object();

    private final Supplier<T> supplier;

    /// Either:
    ///
    /// - [#UNINITIALIZED] (not computed yet)
    /// - Computed value (might be `null``)
    private Object value = UNINITIALIZED;

    public LazyValue(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @SuppressWarnings("unchecked")
    public T get() {
        if (value == UNINITIALIZED) {
            value = supplier.get();
        }

        return (T) value;
    }

    public void invalidate() {
        value = UNINITIALIZED;
    }

    @Override
    public String toString() {
        if (value == UNINITIALIZED) {
            return "null";
        } else {
            return value.toString();
        }
    }
}
