package org.jabref.logic.layout.format;

import java.net.URI;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.model.entry.identifier.DOI;

/**
 * Used to fix [ 1588028 ] export HTML table DOI URL.
 * <p>
 * Will prepend "<a href="http://doi.org/">http://doi.org/</a>" or the DOI url with a custom base URL defined in the {@link DOIPreferences}
 * if only DOI and not an URL is given.
 */
public class DOICheck implements LayoutFormatter {

    private final DOIPreferences doiPreferences;

    public DOICheck(DOIPreferences doiPreferences) {
        this.doiPreferences = doiPreferences;
    }

    @Override
    public String format(String fieldText) {
        if (fieldText == null) {
            return null;
        }
        String result = fieldText;
        if (result.startsWith("/")) {
            result = result.substring(1);
        }

        if (doiPreferences.isUseCustom()) {
            var base = URI.create(doiPreferences.getDefaultBaseURI());
            return DOI.parse(result).flatMap(doi -> doi.getExternalURIFromBase(base))
                      .map(URI::toASCIIString)
                      .orElse(result);
        }

        return DOI.parse(result).map(DOI::getURIAsASCIIString).orElse(result);
    }
}
