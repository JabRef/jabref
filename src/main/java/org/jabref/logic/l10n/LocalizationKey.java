package org.jabref.logic.l10n;

import java.util.Objects;

public class LocalizationKey {

    private final String key;

    public LocalizationKey(String key) {
        this.key = Objects.requireNonNull(key);
    }

    public String getPropertiesKeyUnescaped() {
        // space, #, !, = and : are not allowed in properties file keys
        return this.key;
    }

    public String getPropertiesKey() {
        // space, #, !, = and : are not allowed in properties file keys (# and ! only at the beginning of the key but easier to escape every instance
        return this.key.replace(" ", "\\ ").replace("#", "\\#").replace("!", "\\!").replace("=", "\\=")
                .replace(":", "\\:").replace("\\\\", "\\");
    }

    public String getTranslationValue() {
        return this.key.replace("\\ ", " ").replace("\\#", "#").replace("\\!", "!").replace("\\=", "=").replace("\\:",
                ":");
    }
}
