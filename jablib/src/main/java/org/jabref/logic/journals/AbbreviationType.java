package org.jabref.logic.journals;

import java.util.Arrays;

/// Defines the different abbreviation types that JabRef can operate with.
///
/// DEFAULT: Default abbreviation type, which is the standard behavior.
/// DOTLESS: Abbreviation type that does not include dots in the abbreviation.
/// SHORTEST_UNIQUE: Abbreviation type that generates the shortest unique abbreviation.
/// LTWA: Abbreviation type that uses the LTWA (List of Title Word Abbreviations)/ISO4 method.
public enum AbbreviationType {
    DEFAULT,
    DOTLESS,
    SHORTEST_UNIQUE,
    LTWA;

    public static AbbreviationType safeValueOf(String name) {
        return Arrays.stream(values())
                     .filter(type -> type.name().equals(name))
                     .findFirst()
                     .orElse(DEFAULT);
    }
}
