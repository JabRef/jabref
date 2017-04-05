package org.jabref.logic.layout.format;

import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.cleanup.Formatter;
import org.jabref.model.strings.LatexToUnicodeAdapter;

/**
 * This formatter converts LaTeX character sequences their equivalent unicode characters,
 * and removes other LaTeX commands without handling them.
 */
public class LatexToUnicodeFormatter implements LayoutFormatter, Formatter {

    private Pattern underscoreMatcher = Pattern.compile("_(?!\\{)");

    private String replacementChar = "\uFFFD";

    private Pattern underscorePlaceholderMatcher = Pattern.compile(replacementChar);

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
        String toFormat = underscoreMatcher.matcher(inField).replaceAll(replacementChar);
        toFormat = LatexToUnicodeAdapter.format(toFormat);
        return underscorePlaceholderMatcher.matcher(toFormat).replaceAll("_");
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
