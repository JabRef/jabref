package net.sf.jabref.model.database;

import java.util.Locale;

/**
 * An enum which contains the possible {@link BibDatabase} Modes.
 * Possible are BibTeX and BibLaTeX.
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
            return "BibLaTeX";
        }
    }

    /**
     * Returns the opposite mode of the current mode as {@link BibDatabaseMode}.
     *
     * @return BibLaTeX if the current mode is BIBTEX, BibTeX else
     */
    public BibDatabaseMode getOppositeMode() {
        if (this == BIBTEX) {
            return BIBLATEX;
        } else {
            return BIBTEX;
        }
    }

    /**
     * Returns the {@link BibDatabaseMode} from a given boolean.
     *
     * @param isBibLatex
     * @return BIBLATEX if isBibLatex is true, else BIBTEX
     */
    public static BibDatabaseMode fromPreference(boolean isBibLatex) {
        if (isBibLatex) {
            return BIBLATEX;
        } else {
            return BIBTEX;
        }
    }

    /**
     * Returns the {@link BibDatabaseMode} that equals the given string.
     * The use of capital and small letters in the string doesn't matter.
     * If neither "bibtex" nor "biblatex" is the given string, then this will
     * invoke {@link valueOf(String)}.
     *
     * @param   data
     * @return  BIBTEX, if the string is bibtex
     *          BIBLATEX, if the string is biblatex
     */
    public static BibDatabaseMode parse(String data) {
        return BibDatabaseMode.valueOf(data.toUpperCase(Locale.ENGLISH));
    }

    /**
     * @return the mode as a string in lowercase
     */
    public String getAsString() {
        return getFormattedName().toLowerCase(Locale.ENGLISH);
    }
}
