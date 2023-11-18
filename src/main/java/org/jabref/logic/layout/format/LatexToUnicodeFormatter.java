package org.jabref.logic.layout.format;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.strings.LatexToUnicodeAdapter;

/**
 * This formatter converts LaTeX character sequences their equivalent unicode characters,
 * and removes other LaTeX commands without handling them.
 */
public class LatexToUnicodeFormatter extends Formatter implements LayoutFormatter {

    @Override
    public String getName() {
        return Localization.lang("LaTeX to Unicode");
    }

    @Override
    public String getKey() {
        return "latex_to_unicode";
    }

    @Override
    public String format(String inField) {
        return LatexToUnicodeAdapter.format(inField);
    }

    @Override
    public String getDescription() {
        return Localization.lang("Converts LaTeX encoding to Unicode characters.");
    }

    @Override
    public String getExampleInput() {
        return "M{\\\"{o}}nch";
    }
}
