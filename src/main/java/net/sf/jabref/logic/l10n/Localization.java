package net.sf.jabref.logic.l10n;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.charset.Charset;
import java.util.*;

public class Localization {
    private static final Log LOGGER = LogFactory.getLog(Localization.class);

    // Encodings
    public static final String[] ENCODINGS;
    public static final Map<String, String> ENCODING_NAMES_LOOKUP;

    private static final String RESOURCE_PREFIX = "l10n/JabRef";
    private static final String MENU_RESOURCE_PREFIX = "l10n/Menu";
    private static final String INTEGRITY_RESOURCE_PREFIX = "l10n/IntegrityMessage";
    public static final String[] ALL_ENCODINGS = // (String[])
            // Charset.availableCharsets().keySet().toArray(new
            // String[]{});
            new String[] {"ISO8859_1", "UTF8", "UTF-16", "ASCII", "Cp1250", "Cp1251", "Cp1252",
                    "Cp1253", "Cp1254", "Cp1257", "SJIS",
                    "KOI8_R", // Cyrillic
                    "EUC_JP", // Added Japanese encodings.
                    "Big5", "Big5_HKSCS", "GBK", "ISO8859_2", "ISO8859_3", "ISO8859_4", "ISO8859_5",
                    "ISO8859_6", "ISO8859_7", "ISO8859_8", "ISO8859_9", "ISO8859_13", "ISO8859_15"};

    private static ResourceBundle messages;
    private static ResourceBundle menuTitles;
    private static ResourceBundle intMessages;

    static {
        // Build list of encodings, by filtering out all that are not supported
        // on this system:
        List<String> encodings = new ArrayList<String>();
        for (String ALL_ENCODING : Localization.ALL_ENCODINGS) {
            if (Charset.isSupported(ALL_ENCODING)) {
                encodings.add(ALL_ENCODING);
            }
        }
        ENCODINGS = encodings.toArray(new String[encodings.size()]);
        // Build a map for translating Java encoding names into common encoding names:
        ENCODING_NAMES_LOOKUP = new HashMap<String, String>();
        ENCODING_NAMES_LOOKUP.put("Cp1250", "windows-1250");
        ENCODING_NAMES_LOOKUP.put("Cp1251", "windows-1251");
        ENCODING_NAMES_LOOKUP.put("Cp1252", "windows-1252");
        ENCODING_NAMES_LOOKUP.put("Cp1253", "windows-1253");
        ENCODING_NAMES_LOOKUP.put("Cp1254", "windows-1254");
        ENCODING_NAMES_LOOKUP.put("Cp1257", "windows-1257");
        ENCODING_NAMES_LOOKUP.put("ISO8859_1", "ISO-8859-1");
        ENCODING_NAMES_LOOKUP.put("ISO8859_2", "ISO-8859-2");
        ENCODING_NAMES_LOOKUP.put("ISO8859_3", "ISO-8859-3");
        ENCODING_NAMES_LOOKUP.put("ISO8859_4", "ISO-8859-4");
        ENCODING_NAMES_LOOKUP.put("ISO8859_5", "ISO-8859-5");
        ENCODING_NAMES_LOOKUP.put("ISO8859_6", "ISO-8859-6");
        ENCODING_NAMES_LOOKUP.put("ISO8859_7", "ISO-8859-7");
        ENCODING_NAMES_LOOKUP.put("ISO8859_8", "ISO-8859-8");
        ENCODING_NAMES_LOOKUP.put("ISO8859_9", "ISO-8859-9");
        ENCODING_NAMES_LOOKUP.put("ISO8859_13", "ISO-8859-13");
        ENCODING_NAMES_LOOKUP.put("ISO8859_15", "ISO-8859-15");
        ENCODING_NAMES_LOOKUP.put("KOI8_R", "KOI8-R");
        ENCODING_NAMES_LOOKUP.put("UTF8", "UTF-8");
        ENCODING_NAMES_LOOKUP.put("UTF-16", "UTF-16");
        ENCODING_NAMES_LOOKUP.put("SJIS", "Shift_JIS");
        ENCODING_NAMES_LOOKUP.put("GBK", "GBK");
        ENCODING_NAMES_LOOKUP.put("Big5_HKSCS", "Big5-HKSCS");
        ENCODING_NAMES_LOOKUP.put("Big5", "Big5");
        ENCODING_NAMES_LOOKUP.put("EUC_JP", "EUC-JP");
        ENCODING_NAMES_LOOKUP.put("ASCII", "US-ASCII");
    }

    public static void setLanguage(String language, String country) {
        Locale locale = new Locale(language, country);
        messages = ResourceBundle.getBundle(RESOURCE_PREFIX, locale, new EncodingControl("UTF-8"));
        menuTitles = ResourceBundle.getBundle(MENU_RESOURCE_PREFIX, locale, new EncodingControl("UTF-8"));
        intMessages = ResourceBundle.getBundle(INTEGRITY_RESOURCE_PREFIX, locale, new EncodingControl("UTF-8"));
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

            LOGGER.warn("Warning: could not get menu item translation for \"" + key + "\"");
        }
        if (translation != null && !translation.isEmpty()) {
            return translation;
        } else {
            return key;
        }
    }
}

