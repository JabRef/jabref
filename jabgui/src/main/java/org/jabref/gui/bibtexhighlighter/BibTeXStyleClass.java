package org.jabref.gui.bibtexhighlighter;

import org.jspecify.annotations.NullMarked;

/**
 * Maps a {@code BibTeXTokenCategory} (from the Veneer syntax highlighter) to the CSS style
 * class used to render it in the JavaFX {@code CodeArea}.
 * <p>
 * The enum constant names intentionally mirror
 * {@code io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter.BibTeXTokenCategory} so that
 * {@link BibTeXHighlighter} can resolve a category to its style class via {@link Enum#valueOf}.
 * <p>
 * The actual colors and font styles for each class (e.g. {@code .bibtex-keyword}) are defined
 * in {@code jabref-theme.css}, not here, so that they can be customized per theme without touching Java code.
 */
@NullMarked
public enum BibTeXStyleClass {
    KEYWORD("bibtex-keyword"),
    STRING("bibtex-string"),
    NUMBER("bibtex-number"),
    COMMENT("bibtex-comment"),
    CITE_KEY("bibtex-key"),
    FIELD_NAME("bibtex-field"),
    MACRO("bibtex-type"),
    DEFAULT("text");

    private final String styleClass;

    BibTeXStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    /**
     * Returns the CSS style class name to apply to a text segment of this category,
     * e.g. {@code "bibtex-keyword"}.
     *
     * @return the CSS style class name
     */
    public String getStyleClass() {
        return styleClass;
    }
}
