package org.jabref.logic.layout.format;

import org.jabref.logic.identifier.DOI;
import org.jabref.logic.layout.LayoutFormatter;

/**
 * Will strip any prefixes from the Doi field, in order to output only the Doi number
 */
public class DOIStrip implements LayoutFormatter {
    @Override
    public String format(String fieldText) {
        if (fieldText == null) {
            return null;
        }

        return DOI.build(fieldText).map(DOI::getDOI).orElse(fieldText);
    }
}
