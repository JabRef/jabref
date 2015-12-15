package net.sf.jabref.logic.l10n;

import java.util.Objects;

public class LocalizationKey {

    private final String key;

    public LocalizationKey(String key) {
        this.key = Objects.requireNonNull(key);
    }

    public String getPropertiesKeyUnescaped() {
        // space, = and : are not allowed in properties file keys
        return this.key.replaceAll(" ", "_");
    }

    public String getPropertiesKey() {
        // space, = and : are not allowed in properties file keys
        return this.key.replaceAll(" ", "_").replace("=", "\\=").replace(":", "\\:").replace("\\\\", "\\");
    }

    public String getTranslationValue() {
        return this.key.replaceAll("_", " ").replaceAll("\\\\=", "=").replaceAll("\\\\:", ":");
    }
}
