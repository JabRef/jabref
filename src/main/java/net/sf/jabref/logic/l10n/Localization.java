package net.sf.jabref.logic.l10n;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Localization {
    private static final Log LOGGER = LogFactory.getLog(Localization.class);

    protected static final String RESOURCE_PREFIX = "l10n/JabRef";
    protected static final String MENU_RESOURCE_PREFIX = "l10n/Menu";

    private static ResourceBundle messages;
    private static ResourceBundle menuTitles;

    public static void setLanguage(String language) {
        Optional<Locale> knownLanguage = Languages.convertToSupportedLocale(language);
        if(!knownLanguage.isPresent()) {
            LOGGER.warn("Language " + language + " is not supported by JabRef (Default:" + Locale.getDefault()+ ")");
            setLanguage("en");
            return;
        }

        Locale locale = knownLanguage.get();
        Locale.setDefault(locale);
        javax.swing.JComponent.setDefaultLocale(locale);

        try {
            createResourceBundles(locale);
        } catch (MissingResourceException ex) {
            // should not happen as we have scripts to enforce this
            LOGGER.warn("Could not find bundles for language " + locale + ", switching to full english language", ex);
            setLanguage("en");
        }
    }

    private static void createResourceBundles(Locale locale) {
        messages = ResourceBundle.getBundle(RESOURCE_PREFIX, locale, new EncodingControl(StandardCharsets.UTF_8));
        menuTitles = ResourceBundle.getBundle(MENU_RESOURCE_PREFIX, locale, new EncodingControl(StandardCharsets.UTF_8));
    }

    /**
     * In the translation, %0, ..., %9 is replaced by the respective params given
     *
     * @param resBundle         the ResourceBundle to use
     * @param idForErrorMessage output when translation is not found
     * @param key               the key to lookup in resBundle
     * @param params            a list of Strings to replace %0, %1, ...
     * @return
     */
    private static String translate(ResourceBundle resBundle, String idForErrorMessage, String key, String... params) {
        Objects.requireNonNull(resBundle);

        String translation = null;
        try {
            String propertiesKey = new LocalizationKey(key).getPropertiesKeyUnescaped();
            translation = resBundle.getString(propertiesKey);
        } catch (MissingResourceException ex) {
            LOGGER.warn("Warning: could not get " + idForErrorMessage + " translation for \"" + key + "\" for locale "
                    + Locale.getDefault());
        }
        if ((translation == null) || translation.isEmpty()) {
            LOGGER.warn("Warning: no " + idForErrorMessage + " translation for \"" + key + "\" for locale "
                    + Locale.getDefault());

            translation = key;
        }

        return new LocalizationKeyParams(translation, params).replacePlaceholders();
    }

    public static String lang(String key, String... params) {
        if(messages == null) {
            setLanguage("en");
        }
        return translate(messages, "message", key, params);
    }

    public static String menuTitle(String key, String... params) {
        if(menuTitles == null) {
            setLanguage("en");
        }
        return translate(menuTitles, "menu item", key, params);
    }

}

