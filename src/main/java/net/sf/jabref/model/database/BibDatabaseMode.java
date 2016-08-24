package net.sf.jabref.model.database;

import java.util.Locale;

public enum BibDatabaseMode {
    BIBTEX,
    BIBLATEX;

    public String getFormattedName() {
        if(this == BIBTEX) {
            return "BibTeX";
        } else {
            return "BibLaTeX";
        }
    }

    public BibDatabaseMode getOppositeMode() {
        if(this == BIBTEX) {
            return BIBLATEX;
        } else {
            return BIBTEX;
        }
    }

    public static BibDatabaseMode fromPreference(boolean isBibLatex) {
        return isBibLatex ? BIBLATEX : BIBTEX;
    }

    public static BibDatabaseMode parse(String data) {
        return BibDatabaseMode.valueOf(data.toUpperCase(Locale.ENGLISH));
    }

    public String getAsString() {
        return getFormattedName().toLowerCase(Locale.ENGLISH);
    }
}
