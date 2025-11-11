package org.jabref.logic.formatter.bibtexfields;

import java.util.regex.Pattern;

import org.jabref.architecture.AllowedToUseApacheCommonsLang3;
import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatter;

import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.NonNull;

@AllowedToUseApacheCommonsLang3("There is no equivalent in Google's Guava")
public class HtmlToUnicodeFormatter extends Formatter implements LayoutFormatter {

    private static final Pattern HTML_TAGS_PATTERN = Pattern.compile("<[^>]*>");

    @Override
    public String getName() {
        return Localization.lang("HTML to Unicode");
    }

    @Override
    public String getKey() {
        return "html_to_unicode";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Converts HTML code to Unicode.");
    }

    @Override
    public String getExampleInput() {
        return "<b>bread</b> &amp; butter";
    }

    @Override
    public String format(@NonNull String fieldText) {
        // StringEscapeUtils converts characters and regex kills tags
        String plainText = StringEscapeUtils.unescapeHtml4(fieldText);
        return HTML_TAGS_PATTERN.matcher(plainText).replaceAll("");
    }
}
