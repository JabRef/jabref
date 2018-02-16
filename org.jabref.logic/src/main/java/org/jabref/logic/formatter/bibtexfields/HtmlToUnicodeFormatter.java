package org.jabref.logic.formatter.bibtexfields;

import org.jabref.architecture.ApacheCommonsLang3Allowed;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.cleanup.Formatter;

import org.apache.commons.lang3.StringEscapeUtils;

@ApacheCommonsLang3Allowed("There is no equivalent in Google's Guava")
public class HtmlToUnicodeFormatter implements LayoutFormatter, Formatter {

    @Override
    public String getName() {
        return "HTML to Unicode";
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
    public String format(String fieldText) {
        // StringEscapeUtils converts characters and regex kills tags
        return StringEscapeUtils.unescapeHtml4(fieldText).replaceAll("\\<[^>]*>","");
    }
}
