package org.jabref.logic.layout.format;

import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.util.strings.NormalizeUtils;

/// Formatter that returns the first page from the "pages" field, if set.
///
/// For instance, if the pages field is set to "345-360" or "345--360",
/// this formatter will return "345".
/// Leading and trailing whitespace is ignored, so " 345-360" also
/// returns "345".
public class FirstPage implements LayoutFormatter {

    private final NormalizePagesFormatter formatter = new NormalizePagesFormatter();

    @Override
    public String format(String s) {
        if (s == null) {
            return "";
        }
        String normalizedPages = NormalizeUtils.toNFKC(s).orElse("");
        String formattedPages = formatter.format(normalizedPages);
        String[] pageParts = formattedPages.split("[ \\-\u2013\u2014]+");
        if (pageParts.length == 2) {
            return pageParts[0];
        } else {
            return formattedPages;
        }
    }
}
