package org.jabref.gui.bibtexhighlighter;

import org.fxmisc.richtext.CodeArea;

public interface BibTeXSyntaxHighlighter {
    void applyHighlighting(String source, CodeArea codeArea);
}
