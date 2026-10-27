package org.jabref.logic.journals;

import java.util.Arrays;

public enum JournalMode {
    ABBREVIATE,
    UNABBREVIATE,
    NONE;

    public static JournalMode safeValueOf(String name) {
        return Arrays.stream(values())
                     .filter(mode -> mode.name().equals(name))
                     .findFirst()
                     .orElse(NONE);
    }
}
