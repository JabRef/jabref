package net.sf.jabref.logic.l10n;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.charset.Charset;
import java.util.*;

public class Localization {
    private static final Log LOGGER = LogFactory.getLog(Localization.class);

    private static final String RESOURCE_PREFIX = "l10n/JabRef";
    private static final String MENU_RESOURCE_PREFIX = "l10n/Menu";
    private static final String INTEGRITY_RESOURCE_PREFIX = "l10n/IntegrityMessage";

    private static ResourceBundle messages;
    private static ResourceBundle menuTitles;
    private static ResourceBundle intMessages;

    public static void setLanguage(String language, String country) {
        Locale locale = new Locale(language, country);

        messages = ResourceBundle.getBundle(RESOURCE_PREFIX, locale, new EncodingControl("UTF-8"));
        menuTitles = ResourceBundle.getBundle(MENU_RESOURCE_PREFIX, locale, new EncodingControl("UTF-8"));
        intMessages = ResourceBundle.getBundle(INTEGRITY_RESOURCE_PREFIX, locale, new EncodingControl("UTF-8"));

        // these checks are required as when the requested resource bundle is NOT found, the default locale is used as a fallback silently.
        if(!messages.getLocale().equals(locale)) {
            LOGGER.warn("tried loading <" + RESOURCE_PREFIX + "> for locale <" + locale + "> but had to fall back on default locale <" + Locale.getDefault() + ">");
        }

        if(!menuTitles.getLocale().equals(locale)) {
            LOGGER.warn("tried loading <" + MENU_RESOURCE_PREFIX + "> for locale <" + locale + "> but had to fall back on default locale <" + Locale.getDefault() + ">");
        }

        if(!intMessages.getLocale().equals(locale)) {
            LOGGER.warn("tried loading <" + INTEGRITY_RESOURCE_PREFIX + "> for locale <" + locale + "> but had to fall back on default locale <" + Locale.getDefault() + ">");
        }

        Locale.setDefault(locale);
        javax.swing.JComponent.setDefaultLocale(locale);
    }

    public static String lang(String key, String... params) {
        String translation = null;
        try {
            if (messages != null) {
                translation = messages.getString(key.replaceAll(" ", "_"));
            }
        } catch (MissingResourceException ex) {
            LOGGER.warn("Warning: could not get translation for \"" + key + "\" for locale " + Locale.getDefault());
        }
        if (translation == null) {
            translation = key;
        }

        if (translation != null && !translation.isEmpty()) {
            translation = translation.replaceAll("_", " ");
            StringBuffer sb = new StringBuffer();
            boolean b = false;
            char c;
            for (int i = 0; i < translation.length(); ++i) {
                c = translation.charAt(i);
                if (c == '%') {
                    b = true;
                } else {
                    if (!b) {
                        sb.append(c);
                    } else {
                        b = false;
                        try {
                            int index = Integer.parseInt(String.valueOf(c));
                            if (params != null && index >= 0 && index <= params.length) {
                                sb.append(params[index]);
                            }
                        } catch (NumberFormatException e) {
                            // append literally (for quoting) or insert special
                            // symbol
                            switch (c) {
                                case 'c': // colon
                                    sb.append(':');
                                    break;
                                case 'e': // equal
                                    sb.append('=');
                                    break;
                                default: // anything else, e.g. %
                                    sb.append(c);
                            }
                        }
                    }
                }
            }
            return sb.toString();
        }
        return key;
    }

    public static String lang(String key) {
        return lang(key, (String[]) null);
    }

    public static String menuTitle(String key) {
        String translation = null;
        try {
            if (messages != null) {
                translation = menuTitles.getString(key.replaceAll(" ", "_"));
            }
        } catch (MissingResourceException ex) {
            translation = key;
            LOGGER.warn("Warning: could not get menu item translation for \"" + key + "\"");
        }
        if (translation != null && !translation.isEmpty()) {
            return translation.replaceAll("_", " ");
        } else {
            return key;
        }
    }

    public static String getIntegrityMessage(String key) {
        String translation = null;
        try {
            if (intMessages != null) {
                translation = intMessages.getString(key);
            }
        } catch (MissingResourceException ex) {
            translation = key;

            LOGGER.warn("Warning: could not get translation for integrity message \"" + key + "\"");
        }
        if (translation != null && !translation.isEmpty()) {
            return translation;
        } else {
            return key;
        }
    }
}

