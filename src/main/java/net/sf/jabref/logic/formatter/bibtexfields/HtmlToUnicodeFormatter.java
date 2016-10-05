package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.model.cleanup.Formatter;

import org.apache.commons.lang3.StringEscapeUtils;

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
