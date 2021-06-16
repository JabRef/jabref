package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import de.undercouch.citeproc.LocaleProvider;
import de.undercouch.citeproc.helper.CSLUtils;

/**
 * A {@link LocaleProvider} that loads locales from a directory in the current module.
 * <p>
 * This implementation is only a slight adaption of {@link de.undercouch.citeproc.DefaultLocaleProvider}.
 */
public class JabRefLocaleProvider implements LocaleProvider {

    private static final String LOCALES_ROOT = "/csl-locales";

    private final Map<String, String> locales = new HashMap<>();

    @Override
    public String retrieveLocale(String lang) {
        return locales.computeIfAbsent(lang, locale -> {
            try {
                URL url = getClass().getResource(LOCALES_ROOT + "/locales-" + locale + ".xml");
                if (url == null) {
                    throw new IllegalArgumentException("Unable to load locale " + locale);
                }

                return CSLUtils.readURLToString(url, "UTF-8");
            } catch (IOException e) {
                throw new UncheckedIOException("failed to read locale " + locale, e);
            }
        });
    }
}
