package org.jabref.logic.l10n;

import java.util.Objects;

/**
 * Model for a localization to translate. The key is the English text.
 */
public class LocalizationKey {

    private final String javaCodeKey;
    private final String propertyKey;

    private LocalizationKey(String javaCodeKey, String propertyKey) {
        this.javaCodeKey = javaCodeKey;
        this.propertyKey = propertyKey;
    }

    public static LocalizationKey fromJavaKey(String key) {
        String javaCodeKey = Objects.requireNonNull(key);
        // space, #, !, = and : are not allowed in properties file keys (# and ! only at the beginning of the key but easier to escape every instance
        // Newline ('\n') is already escaped in Java source. Thus, there is no need to escape it a second time.
        String propertyKey = key
                // escape the remaining characters
                .replace(" ", "\\ ")
                .replace("#", "\\#")
                .replace("!", "\\!")
                .replace("=", "\\=")
                .replace(":", "\\:");
        return new LocalizationKey(javaCodeKey, propertyKey);
    }

    public static LocalizationKey fromPropertyKey(String key) {
        String propertyKey = key;
        // we ne need to unescape the escaped characters (see org.jabref.logic.l10n.LocalizationKey.LocalizationKey)
        String javaCodeKey = key.replaceAll("\\\\([ #!=:])", "$1");
        return new LocalizationKey(javaCodeKey, propertyKey);
    }

    public String getJavaCodeKey() {
        return this.javaCodeKey;
    }

    public String getPropertiesKey() {
        return this.propertyKey;
    }

    public String getTranslationValue() {
        return this.javaCodeKey;
    }
}
