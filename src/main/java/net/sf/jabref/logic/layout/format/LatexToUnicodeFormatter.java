package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.model.cleanup.Formatter;
import net.sf.jabref.model.strings.LatexToUnicode;

/**
 * This formatter converts LaTeX character sequences their equivalent unicode characters,
 * and removes other LaTeX commands without handling them.
 */
public class LatexToUnicodeFormatter implements LayoutFormatter, Formatter {

    private final LatexToUnicode formatter = new LatexToUnicode();


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
        return formatter.format(inField);
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
