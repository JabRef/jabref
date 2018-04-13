package org.jabref.logic.l10n;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides handling for messages and menu entries in the preferred language of the user.
 * <p>
 * Notes: All messages and menu-entries in JabRef are stored in escaped form like "This_is_a_message". This message
 * serves as key inside the {@link l10n} properties files that hold the translation for many languages. When a message
 * is accessed, it needs to be unescaped and possible parameters that can appear in a message need to be filled with
 * values.
 * <p>
 * This implementation loads the appropriate language by importing all keys/values from the correct bundle and stores
 * them in unescaped form inside a {@link LocalizationBundle} which provides fast access because it caches the key-value
 * pairs.
 * <p>
 * The access to this is given by the functions {@link Localization#lang(String, String...)} and {@link
 * Localization#menuTitle(String, String...)} that developers should use whenever they use strings for the e.g. GUI that
 * need to be translatable.
 */
public class Localization {
    public static final String BIBTEX = "BibTeX";
    static final String RESOURCE_PREFIX = "l10n/JabRef";
    static final String MENU_RESOURCE_PREFIX = "l10n/Menu";

    private static final Logger LOGGER = LoggerFactory.getLogger(Localization.class);

    private static Locale locale;
    private static LocalizationBundle localizedMessages;
    private static LocalizationBundle localizedMenuTitles;

    private Localization() {
    }

    /**
     * Public access to all messages that are not menu-entries
     *
     * @param key    The key of the message in unescaped form like "All fields"
     * @param params Replacement strings for parameters %0, %1, etc.
     * @return The message with replaced parameters
     */
    public static String lang(String key, String... params) {
        if (localizedMessages == null) {
            // I'm logging this because it should never happen
            LOGGER.error("Messages are not initialized before accessing " + key);
            setLanguage("en");
        }
        return lookup(localizedMessages, "message", key, params);
    }

    /**
     * Public access to menu entry messages
     *
     * @param key    The key of the message in unescaped form like "Save all"
     * @param params Replacement strings for parameters %0, %1, etc.
     * @return The message with replaced parameters
     */
    public static String menuTitle(String key, String... params) {
        if (localizedMenuTitles == null) {
            // I'm logging this because it should never happen
            LOGGER.error("Menu entries are not initialized");
            setLanguage("en");
        }
        return lookup(localizedMenuTitles, "menu item", key, params);
    }

    /**
     * Return the translated string for usage in JavaFX menus.
     *
     * @implNote This is only a temporary workaround. In the long term, the & sign should be removed from the language
     * files.
     */
    public static String menuTitleFX(String key, String... params) {
        // Remove & sign, which is not used by JavaFX to signify the shortcut
        return menuTitle(key, params).replace("&", "");
    }

    /**
     * Sets the language and loads the appropriate translations. Note, that this function should be called before any
     * other function of this class.
     *
     * @param language Language identifier like "en", "de", etc.
     */
    public static void setLanguage(String language) {
        Optional<Locale> knownLanguage = Languages.convertToSupportedLocale(language);
        final Locale defaultLocale = Locale.getDefault();
        if (!knownLanguage.isPresent()) {
            LOGGER.warn("Language " + language + " is not supported by JabRef (Default:" + defaultLocale + ")");
            setLanguage("en");
            return;
        }
        // avoid reinitialization of the language bundles
        final Locale langLocale = knownLanguage.get();
        if ((locale != null) && locale.equals(langLocale) && locale.equals(defaultLocale)) {
            return;
        }
        locale = langLocale;
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

    /**
     * Public access to the messages bundle for classes like AbstractView.
     *
     * @return The internally cashed bundle.
     */
    public static LocalizationBundle getMessages() {
        // avoid situations where this function is called before any language was set
        if (locale == null) {
            setLanguage("en");
        }
        return localizedMessages;
    }

    /**
     * Creates and caches the language bundles used in JabRef for a particular language. This function first loads
     * correct version of the "escaped" bundles that are given in {@link l10n}. After that, it stores the unescaped
     * version in a cached {@link LocalizationBundle} for fast access.
     *
     * @param locale Localization to use.
     */
    private static void createResourceBundles(Locale locale) {
        ResourceBundle messages = ResourceBundle.getBundle(RESOURCE_PREFIX, locale, new EncodingControl(StandardCharsets.UTF_8));
        ResourceBundle menuTitles = ResourceBundle.getBundle(MENU_RESOURCE_PREFIX, locale, new EncodingControl(StandardCharsets.UTF_8));
        Objects.requireNonNull(messages, "Could not load " + RESOURCE_PREFIX + " resource.");
        Objects.requireNonNull(menuTitles, "Could not load " + MENU_RESOURCE_PREFIX + " resource.");
        localizedMenuTitles = new LocalizationBundle(createLookupMap(menuTitles));
        localizedMessages = new LocalizationBundle(createLookupMap(messages, localizedMenuTitles));
    }

    /**
     * Helper function to create a HashMap from the key/value pairs of a bundle.
     *
     * @param baseBundle JabRef language bundle with keys and values for translations.
     * @return Lookup map for the baseBundle.
     */
    private static HashMap<String, String> createLookupMap(ResourceBundle baseBundle) {
        final ArrayList<String> baseKeys = Collections.list(baseBundle.getKeys());
        return new HashMap<>(baseKeys.stream().collect(
                Collectors.toMap(
                        key -> new LocalizationKey(key).getTranslationValue(),
                        key -> new LocalizationKey(baseBundle.getString(key)).getTranslationValue())
        ));
    }

    /**
     * Helper function to create a HashMap from the key/value pairs of a bundle and existing localized menu titles.
     * Currently, JabRef has two translations for the same string: One for the menu and one for other parts of the
     * application. The menu might contain an ampersand (&), which causes issues when used outside the menu.
     * With this fix, the ampersand is removed
     */
    private static HashMap<String,String> createLookupMap(ResourceBundle baseBundle, LocalizationBundle localizedMenuTitles) {
        final ArrayList<String> baseKeys = Collections.list(baseBundle.getKeys());
        return new HashMap<>(baseKeys.stream().collect(
                Collectors.toMap(
                        key -> new LocalizationKey(key).getTranslationValue(),
                        key -> {
                            String menuTranslationValue = localizedMenuTitles.lookup.get(key);
                            String plainTranslationValue = new LocalizationKey(baseBundle.getString(key)).getTranslationValue();
                            String translationValue;
                            if (plainTranslationValue.contains("&") && plainTranslationValue.equals(menuTranslationValue)) {
                                translationValue = plainTranslationValue.replace("&", "");
                            } else {
                                translationValue = plainTranslationValue;
                            }
                            return translationValue;
                        })
        ));
    }

    /**
     * This looks up a key in the bundle and replaces parameters %0, ..., %9 with the respective params given. Note that
     * the keys are the "unescaped" strings from the bundle property files.
     *
     * @param bundle            The {@link LocalizationBundle} which means either {@link Localization#localizedMenuTitles}
     *                          or {@link Localization#localizedMessages}.
     * @param idForErrorMessage Identifier-string when the translation is not found.
     * @param key               The lookup key.
     * @param params            The parameters that should be inserted into the message
     * @return The final message with replaced parameters.
     */
    private static String lookup(LocalizationBundle bundle, String idForErrorMessage, String key, String... params) {
        Objects.requireNonNull(key);

        String translation = bundle.containsKey(key) ? bundle.getString(key) : "";
        if (translation.isEmpty()) {
            LOGGER.warn("Warning: could not get " + idForErrorMessage + " translation for \"" + key + "\" for locale "
                    + Locale.getDefault());
            translation = key;
        }
        return new LocalizationKeyParams(translation, params).replacePlaceholders();
    }

    /**
     * A bundle for caching localized strings. Needed to support JavaFX inline binding.
     */
    private static class LocalizationBundle extends ResourceBundle {

        private final HashMap<String, String> lookup;

        LocalizationBundle(HashMap<String, String> lookupMap) {
            lookup = lookupMap;
        }

        @Override
        public final Object handleGetObject(String key) {
            Objects.requireNonNull(key);
            return Optional.ofNullable(lookup.get(key))
                           .orElse(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(lookup.keySet());
        }

        @Override
        protected Set<String> handleKeySet() {
            return lookup.keySet();
        }

        @Override
        public boolean containsKey(String key) {
            // Pretend we have every key
            return true;
        }
    }
}

