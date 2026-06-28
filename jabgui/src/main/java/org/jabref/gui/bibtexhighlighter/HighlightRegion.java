package org.jabref.gui.bibtexhighlighter;

public record HighlightRegion(
        int start,
        int end,
        BibTeXStyleClass style
) {
}
