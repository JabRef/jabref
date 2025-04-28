package org.jabref.logic.l10n;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

public class LocalizationKeyParams {

    private final LocalizationKey key;
    private final List<String> params;

    public LocalizationKeyParams(String key, String... params) {
        this.key = LocalizationKey.fromKey(key);
        this.params = Arrays.asList(params);
        if (this.params.size() > 10) {
            throw new IllegalStateException("Translations can only have at most 10 parameters");
        }
    }

    public String replacePlaceholders() {
        String translation = key.getKey();

        for (int i = 0; i < params.size(); i++) {
            String param = params.get(i);
            translation = translation.replaceAll("%" + i, Matcher.quoteReplacement(param));
        }

        return translation;
    }
}
