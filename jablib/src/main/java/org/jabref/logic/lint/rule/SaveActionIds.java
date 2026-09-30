package org.jabref.logic.lint.rule;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.formatter.Formatters;
import org.jabref.logic.formatter.IdentityFormatter;

import org.jspecify.annotations.NullMarked;

/// The rule id of a Save Action, and the formatter behind such an id.
///
/// The id is the formatter's key in kebab-case, which is **not** reversible: five of JabRef's keys
/// do not survive the transform -- `escapeAmpersands`, `escapeDollarSign`, `escapeUnderscores`,
/// `MSC_codes_to_descriptions` and `NORMALIZE_UNICODE` -- and [Formatters#getFormatterForKey] is an
/// exact match, so `escapeampersands` would find nothing. The way back is therefore a map built by
/// putting every registered key through the same transform, never by reversing the string.
@NullMarked
public final class SaveActionIds {

    private static final Map<String, Formatter> BY_ID = byId();

    private SaveActionIds() {
    }

    /// The id the rule of a Save Action using this formatter is known by.
    public static String idOf(Formatter formatter) {
        return asId(formatter.getKey());
    }

    /// The formatter an id names, or nothing where it names none -- which is the case for every
    /// rule JabFix ships with, since those are not formatters.
    public static Optional<Formatter> formatterFor(String ruleId) {
        return Optional.ofNullable(BY_ID.get(ruleId));
    }

    private static String asId(String formatterKey) {
        return formatterKey.replace('_', '-').toLowerCase(Locale.ROOT);
    }

    private static Map<String, Formatter> byId() {
        Map<String, Formatter> byId = new HashMap<>();
        // Not one of the registered formatters, but reachable: a Save Action naming a formatter
        // JabRef cannot resolve is read as this one.
        byId.put(asId(new IdentityFormatter().getKey()), new IdentityFormatter());
        Formatters.getAll().forEach(formatter -> byId.put(asId(formatter.getKey()), formatter));
        return Map.copyOf(byId);
    }
}
