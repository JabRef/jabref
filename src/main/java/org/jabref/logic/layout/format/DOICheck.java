package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.identifier.DOI;

/**
 * Used to fix [ 1588028 ] export HTML table DOI URL.
 *
 * Will prepend "http://doi.org/" if only DOI and not an URL is given.
 */
public class DOICheck implements LayoutFormatter {
    @Override
    public String format(String fieldText) {
        if (fieldText == null) {
            return null;
        }
        String result = fieldText;
        if (result.startsWith("/")) {
            result = result.substring(1);
        }
        return DOI.parse(result).map(DOI::getURIAsASCIIString).orElse(result);
    }
}
