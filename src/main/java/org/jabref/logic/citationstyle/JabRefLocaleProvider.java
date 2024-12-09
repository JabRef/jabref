package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import de.undercouch.citeproc.LocaleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link LocaleProvider} that loads locales from a directory in the current module.
 * <p>
 * This implementation is only a slight adaption of {@link de.undercouch.citeproc.DefaultLocaleProvider}.
 */
public class JabRefLocaleProvider implements LocaleProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefLocaleProvider.class);

    private static final String LOCALES_ROOT = "/csl-locales";

    private final Map<String, String> locales = new HashMap<>();

    @Override
    public String retrieveLocale(String lang) {
        return locales.computeIfAbsent(lang, locale -> {
            try (InputStream inputStream = getClass().getResourceAsStream(LOCALES_ROOT + "/locales-" + locale + ".xml")) {
                if (inputStream == null) {
                    throw new IllegalArgumentException("Unable to load locale " + locale);
                }
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.error("failed to read locale {}", locale, e);
                throw new UncheckedIOException("failed to read locale " + locale, e);
            }
        });
    }
}
