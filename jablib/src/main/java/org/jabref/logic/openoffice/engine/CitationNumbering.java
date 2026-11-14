package org.jabref.logic.openoffice.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

/// Computes one-based numbers from stable citation keys, without reading or modifying marks.
/// The caller supplies eligible keys in document order (excluding unresolved citations when they
/// must not take a number). Key resolution, layout ordering and label rendering belong to callers.
@NullMarked
public final class CitationNumbering {

    private CitationNumbering() {
    }

    public static Map<String, Integer> firstAppearance(List<String> citationKeysInDocumentOrder) {
        return bibliographyOrder(citationKeysInDocumentOrder, List.of());
    }

    /// Bibliography order takes precedence for cited keys. Other cited keys follow in first-appearance
    /// order. Repeated keys reuse a number; uncited bibliography entries do not consume numbers.
    /// The returned map is immutable and iterates in assigned-number order.
    public static Map<String, Integer> bibliographyOrder(List<String> citationKeysInDocumentOrder, List<String> bibliographyKeysInOrder) {
        Set<String> citedKeys = Set.copyOf(citationKeysInDocumentOrder);
        Map<String, Integer> numbers = new LinkedHashMap<>();
        for (String key : bibliographyKeysInOrder) {
            if (citedKeys.contains(key)) {
                numbers.putIfAbsent(key, numbers.size() + 1);
            }
        }
        for (String key : citationKeysInDocumentOrder) {
            numbers.putIfAbsent(key, numbers.size() + 1);
        }
        return Collections.unmodifiableMap(numbers);
    }
}
