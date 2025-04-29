package org.jabref.model.database;

import java.util.Locale;

/**
 * An enum which contains the possible {@link BibDatabase} Modes.
 * Possible are BibTeX and biblatex.
 */
public enum BibDatabaseMode {
    BIBTEX,
    BIBLATEX;

    /**
     * @return the name of the current mode as String
     */
    public String getFormattedName() {
        if (this == BIBTEX) {
            return "BibTeX";
        } else {
            return "biblatex";
        }
    }

    /**
     * Returns the opposite mode of the current mode as {@link BibDatabaseMode}.
     *
     * @return biblatex if the current mode is BIBTEX, BibTeX else
     */
    public BibDatabaseMode getOppositeMode() {
        if (this == BIBTEX) {
            return BIBLATEX;
        } else {
            return BIBTEX;
        }
    }

    /**
     * Returns the {@link BibDatabaseMode} that equals the given string. The use of capital and small letters
     * in the string doesn't matter.If neither "bibtex" nor "biblatex" is the given string, then an
     * {@link IllegalArgumentException} will be thrown.
     *
     * @return  BIBTEX, if the string is bibtex<br>
     *          BIBLATEX, if the string is biblatex<br>
     */
    public static BibDatabaseMode parse(String data) {
        return BibDatabaseMode.valueOf(data.toUpperCase(Locale.ENGLISH));
    }

    /**
     * @return The current mode as String in lowercase
     */
    public String getAsString() {
        return getFormattedName().toLowerCase(Locale.ENGLISH);
    }
}
