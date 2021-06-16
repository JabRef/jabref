package org.jabref.logic.l10n;

import java.util.Objects;

/**
 * Model for a localization to translate. The key is the English text.
 */
public class LocalizationKey {

    private final String key;
    private final String escapedPropertyKey;

    /**
     * @param key plain key - no escaping. E.g., "Copy \cite{key}" or "Newline follows\nsecond line" are valid parameters.
     */
    private LocalizationKey(String key) {
        this.key = key;
        // space, #, !, = and : are not allowed in properties file keys
        // # and ! are only disallowed at the beginning of the key but easier to escape every instance
        this.escapedPropertyKey = key
                .replace("\n", "\\n")
                .replace(" ", "\\ ")
                .replace("#", "\\#")
                .replace("!", "\\!")
                .replace("=", "\\=")
                .replace(":", "\\:");
    }

    /**
     * @param key plain key - no escaping. E.g., "Copy \cite{key}" or "Newline follows\nsecond line" are valid parameters.
     */
    public static LocalizationKey fromKey(String key) {
        return new LocalizationKey(Objects.requireNonNull(key));
    }

    public static LocalizationKey fromEscapedJavaString(String key) {
        // "\n" in the string is an escaped newline. That needs to be kept.
        // "\\" in the string can stay --> needs to be kept
        return new LocalizationKey(Objects.requireNonNull(key));
    }

    /*
    public static LocalizationKey fromPropertyKey(String key) {
        String propertyKey = key;
        // we ne need to unescape the escaped characters (see org.jabref.logic.l10n.LocalizationKey.LocalizationKey)
        String javaCodeKey = key.replaceAll("\\\\([ #!=:])", "$1");
        return new LocalizationKey(javaCodeKey, propertyKey);
    }
    */
    public String getEscapedPropertiesKey() {
        return this.escapedPropertyKey;
    }

    public String getValueForEnglishPropertiesFile() {
        // Newline needs to be escaped
        return this.key.replace("\n", "\\n");
    }

    public String getKey() {
        return this.key;
    }
}
