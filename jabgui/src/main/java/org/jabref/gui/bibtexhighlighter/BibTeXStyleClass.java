package org.jabref.gui.bibtexhighlighter;

import org.jspecify.annotations.NullMarked;

/// Maps a `BibTeXTokenCategory` (from the Veneer syntax highlighter) to the CSS style
/// class used to render it in the JavaFX `CodeArea`.
///
/// The enum constant names intentionally mirror
/// `io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter.BibTeXTokenCategory` so that
/// [BibTeXHighlighter] can resolve a category to its style class via [Enum#valueOf].
///
/// The actual colors and font styles for each class (e.g. `.bibtex-keyword`) are defined
/// in the theme stylesheets (`jabref-theme.css` on themes.jabref.org), not here, so that they can be customized per theme without touching Java code.
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

    /// Returns the CSS style class name to apply to a text segment of this category,
    /// e.g. `"bibtex-keyword"`.
    ///
    /// @return the CSS style class name
    public String getStyleClass() {
        return styleClass;
    }

    /// Returns inline CSS styles corresponding to this style class for rich text / HTML clipboard export.
    ///
    /// @return inline CSS style string, e.g. `"color: #0033b3; font-weight: bold;"`
    public String getInlineCss() {
        return switch (this) {
            case KEYWORD ->
                    "color: #0033b3; font-weight: bold;";
            case CITE_KEY ->
                    "color: #177534; font-weight: bold;";
            case FIELD_NAME ->
                    "color: #b05a00;";
            case STRING ->
                    "color: #067d17;";
            case NUMBER ->
                    "color: #1750eb;";
            case COMMENT ->
                    "color: #8c8c8c; font-style: italic;";
            case MACRO ->
                    "color: #871094;";
            case DEFAULT ->
                    "";
        };
    }

    /// Resolves a category name to a [BibTeXStyleClass], falling back to [DEFAULT] if unknown.
    ///
    /// @param name the name of the token category
    /// @return the corresponding style class or DEFAULT
    public static BibTeXStyleClass fromCategoryName(String name) {
        for (BibTeXStyleClass style : values()) {
            if (style.name().equals(name)) {
                return style;
            }
        }
        return DEFAULT;
    }
}
