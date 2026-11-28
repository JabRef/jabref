package org.jabref.model.metadata;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;

/// How far a library lets one JabFix rule go when it is saved.
@NullMarked
public enum RuleMode {
    /// The rule does not run: nothing is reported and nothing is changed. This is what a rule the
    /// library does not name does, so naming one here says it out loud.
    OFF,

    /// The rule reports what it finds and changes nothing, even where it knows the repair.
    CHECK,

    /// The rule reports what it finds and repairs it.
    FIX;

    /// How this mode is spelled in a library's formatting configuration.
    public String asKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    /// The mode spelled that way, or nothing for a spelling this JabFix does not know -- which a
    /// newer one may, so it is passed over rather than rejected.
    ///
    /// Only the lower-case spelling is read. Taking `FIX` as well would mean writing a file back
    /// differently from how it was read, for a library that did nothing wrong.
    public static Optional<RuleMode> fromKey(String key) {
        return Arrays.stream(values())
                     .filter(mode -> mode.asKey().equals(key))
                     .findFirst();
    }
}
