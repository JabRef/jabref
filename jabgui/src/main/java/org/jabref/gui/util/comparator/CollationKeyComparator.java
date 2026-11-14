package org.jabref.gui.util.comparator;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Compares strings in the same order as the given [Collator], but computes the collation key of each string only once.
///
/// [Collator#compare(String, String)] normalizes both strings on every call.
/// Sorting a table with n rows calls it about n log n times, which made sorting a large library take seconds.
///
/// The keys are cached as byte arrays: a [java.text.CollationKey] references its source string and would therefore keep the weak cache key alive.
/// A cache entry is dropped as soon as its string is no longer used, e.g., after the field value changed.
///
/// Not thread-safe, intended for use on the JavaFX thread only.
/// `null` is sorted before any string, as in [javafx.scene.control.TableColumn#DEFAULT_COMPARATOR].
@NullMarked
public class CollationKeyComparator implements Comparator<@Nullable String> {

    private final Collator collator;
    private final Map<String, byte[]> collationKeys = new WeakHashMap<>();

    public CollationKeyComparator(Collator collator) {
        this.collator = collator;
    }

    @Override
    public int compare(@Nullable String value1, @Nullable String value2) {
        if (value1 == null) {
            return value2 == null ? 0 : -1;
        }
        if (value2 == null) {
            return 1;
        }
        return Arrays.compareUnsigned(getCollationKey(value1), getCollationKey(value2));
    }

    private byte[] getCollationKey(String value) {
        return collationKeys.computeIfAbsent(value, key -> collator.getCollationKey(key).toByteArray());
    }
}
