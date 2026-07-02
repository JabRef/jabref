package org.jabref.gui.bibtexhighlighter;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum BibTeXStyleClass {
    COMMENT("bibtex-comment"),
    KEYWORD("bibtex-keyword"),
    STRING("bibtex-string"),
    NUMBER("bibtex-number"),
    FIELD("bibtex-field"),
    KEY("bibtex-key"),
    TYPE("bibtex-type"),
    DEFAULT("text");

    private final String className;

    BibTeXStyleClass(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
