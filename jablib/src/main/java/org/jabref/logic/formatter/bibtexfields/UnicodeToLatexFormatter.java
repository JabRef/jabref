package org.jabref.logic.formatter.bibtexfields;

import org.jabref.latexconv.LatexConv;
import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatter;

import org.jspecify.annotations.NonNull;

public class UnicodeToLatexFormatter extends Formatter implements LayoutFormatter {

    @Override
    public String format(@NonNull String text) {
        return LatexConv.toLatex(text);
    }

    @Override
    public String getDescription() {
        return Localization.lang("Converts Unicode characters to LaTeX encoding.");
    }

    @Override
    public String getExampleInput() {
        return "Mönch";
    }

    @Override
    public String getName() {
        return Localization.lang("Unicode to LaTeX");
    }

    @Override
    public String getKey() {
        return "unicode_to_latex";
    }
}
