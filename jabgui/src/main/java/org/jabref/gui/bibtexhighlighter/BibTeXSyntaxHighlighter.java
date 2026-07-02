package org.jabref.gui.bibtexhighlighter;

import java.util.List;

import org.fxmisc.richtext.CodeArea;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BibTeXSyntaxHighlighter {
    List<HighlightRegion> computeHighlighting(String source);

    void applyHighlighting(List<HighlightRegion> regions, CodeArea codeArea);
}
