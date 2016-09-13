package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.logic.util.DOI;

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
