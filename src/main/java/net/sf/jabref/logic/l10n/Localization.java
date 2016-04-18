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

    public static final String RESOURCE_PREFIX = "l10n/JabRef";
    public static final String MENU_RESOURCE_PREFIX = "l10n/Menu";

    private static ResourceBundle messages;
    private static ResourceBundle menuTitles;

    public static void setLanguage(String language) {
        Optional<String> knownLanguage = Languages.convertToKnownLocale(language);
        if(!knownLanguage.isPresent()) {
            LOGGER.warn("Language " + language + " is not supported by JabRef (Default:" + Locale.getDefault()+ ")");
            setLanguage("en");
            return;
        }

        String[] languageParts = knownLanguage.get().split("_");
        Locale locale;
        if (languageParts.length == 1) {
            locale = new Locale(languageParts[0]);
        } else if (languageParts.length == 2) {
            locale = new Locale(languageParts[0], languageParts[1]);
        } else {
            locale = Locale.ENGLISH;
        }

        Locale.setDefault(locale);
        javax.swing.JComponent.setDefaultLocale(locale);

        try {
            createResourceBundles(locale);
        } catch (MissingResourceException e) {
            // SHOULD NOT HAPPEN AS WE HAVE SCRIPTS TO COVER FOR THIS
            LOGGER.warn("Could not find bundles for language " + locale + ", switching to full english language", e);
            setLanguage("en");
        }
    }

    private static void createResourceBundles(Locale locale) {
        messages = ResourceBundle.getBundle(RESOURCE_PREFIX, locale, new EncodingControl(StandardCharsets.UTF_8));
        menuTitles = ResourceBundle.getBundle(MENU_RESOURCE_PREFIX, locale,
                new EncodingControl(StandardCharsets.UTF_8));
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

