package net.sf.jabref.model.database;

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
}
