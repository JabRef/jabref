package org.jabref.logic.journals;

/**
 * Defines the different abbreviation types that JabRef can operate with.
 * <p>
 * DEFAULT: Default abbreviation type, which is the standard behavior.
 * DOTLESS: Abbreviation type that does not include dots in the abbreviation.
 * SHORTEST_UNIQUE: Abbreviation type that generates the shortest unique abbreviation.
 * LTWA: Abbreviation type that uses the LTWA (List of Title Word Abbreviations)/ISO4 method.
 */
public enum AbbreviationType {
    DEFAULT,
    DOTLESS,
    SHORTEST_UNIQUE,
    LTWA
}
