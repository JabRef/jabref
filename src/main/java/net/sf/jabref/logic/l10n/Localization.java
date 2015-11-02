package net.sf.jabref.logic.l10n;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class Localization {
    private static final Log LOGGER = LogFactory.getLog(Localization.class);

    private static final Locale defaultLocale = Locale.getDefault();

    public static final String RESOURCE_PREFIX = "l10n/JabRef";
    public static final String MENU_RESOURCE_PREFIX = "l10n/Menu";

    private static ResourceBundle messages;
    private static ResourceBundle menuTitles;

    public static void setLanguage(String language) {
        Locale locale = new Locale(language);

        try {
            messages = ResourceBundle.getBundle(RESOURCE_PREFIX, locale, new EncodingControl("UTF-8"));
            menuTitles = ResourceBundle.getBundle(MENU_RESOURCE_PREFIX, locale, new EncodingControl("UTF-8"));

            // silent fallback to system locale when bundle is not found
            if(!messages.getLocale().equals(locale)) {
                LOGGER.warn("Bundle for locale <" + locale + "> not found. Falling back to system locale <" + defaultLocale + ">");
            }
        } catch(MissingResourceException e) {
            LOGGER.warn("Bundle for locale <" + locale + "> not found. Fallback to system locale <" + defaultLocale
                    + "> failed, using locale <en> instead", e);

            locale = new Locale("en");
            messages = ResourceBundle.getBundle(RESOURCE_PREFIX, locale, new EncodingControl("UTF-8"));
            menuTitles = ResourceBundle.getBundle(MENU_RESOURCE_PREFIX, locale, new EncodingControl("UTF-8"));
        } finally {
            // Set consistent VM locales
            Locale.setDefault(locale);
            javax.swing.JComponent.setDefaultLocale(locale);
        }
    }

    /**
     * In the translation, %c is translated to ":", %e is translated to "=", %<anythingelse> to <anythingelse>, %0, ...
     * %9 to the respective params given
     *
     * @param resBundle the ResourceBundle to use
     * @param idForErrorMessage output when translation is not found ö * @param key the key to lookup in resBundle
     * @param params a list of Strings to replace %0, %1, ...
     * @return
     */
    private static String translate(ResourceBundle resBundle, String idForErrorMessage, String key, String... params) {
        String translation = null;
        try {
            if (resBundle != null) {
                translation = resBundle.getString(key.replaceAll(" ", "_"));
            }
        } catch (MissingResourceException ex) {
            LOGGER.warn("Warning: could not get " + idForErrorMessage + " translation for \"" + key + "\" for locale "
                    + Locale.getDefault(), ex);
        }
        if (translation == null) {
            translation = key;
        }

        // replace %0, %1, ...
        if ((translation != null) && !translation.isEmpty()) {
            // also done if no params are given
            //  Then, %c is translated to ":", %e is translated to "=", ...
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
                            if ((params != null) && (index >= 0) && (index <= params.length)) {
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

    public static String lang(String key, String... params) {
        return translate(messages, "message", key, params);
    }

    public static String lang(String key) {
        return lang(key, (String[]) null);
    }

    public static String menuTitle(String key, String... params) {
        return translate(menuTitles, "menu item", key, params);
    }

}

