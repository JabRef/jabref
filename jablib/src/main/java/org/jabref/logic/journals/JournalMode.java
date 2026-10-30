package org.jabref.logic.journals;

import java.util.Arrays;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum JournalMode {
    ABBREVIATE,
    NONE,
    UNABBREVIATE;

    public static JournalMode safeValueOf(String name) {
        return Arrays.stream(values())
                     .filter(mode -> mode.name().equals(name))
                     .findFirst()
                     .orElse(NONE);
    }
}
