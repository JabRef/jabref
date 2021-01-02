package org.jabref.logic.l10n;

import java.util.Objects;

/**
 * Model for a localization to translate. The key is the English text.
 */
public class LocalizationKey {

    private final String keyInJavaCode;
    private final String escapedJavaKey;

    public LocalizationKey(String key) {
        this.keyInJavaCode = Objects.requireNonNull(key);
        // space, #, !, = and : are not allowed in properties file keys (# and ! only at the beginning of the key but easier to escape every instance
        // Newline ('\n') is already escaped in Java source. Thus, there is no need to escape it a second time.
        this.escapedJavaKey = key
                // escape the backslash
                .replace("\\", "\\\\")
                // escape the newline
                .replace("\n", "\\n")
                // escape the remaining characters
                .replace(" ", "\\ ")
                .replace("#", "\\#")
                .replace("!", "\\!")
                .replace("=", "\\=")
                .replace(":", "\\:");
    }

    public String getPropertiesKeyUnescaped() {
        return this.keyInJavaCode;
    }

    public String getPropertiesKey() {
        return this.escapedJavaKey;
    }

    public String getTranslationValue() {
        return this.keyInJavaCode;
    }
}
