package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.identifier.DOI;

/**
 * Will strip any prefixes from the Doi field, in order to output only the Doi number
 */
public class DOIStrip implements LayoutFormatter {
    @Override
    public String format(String fieldText) {
        if (fieldText == null) {
            return null;
        }

        return DOI.parse(fieldText).map(DOI::getDOI).orElse(fieldText);
    }
}
