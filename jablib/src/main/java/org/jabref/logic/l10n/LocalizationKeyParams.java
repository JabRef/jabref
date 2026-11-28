package org.jabref.logic.l10n;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalizationKeyParams {

    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d)");

    private final LocalizationKey key;
    private final List<String> params;

    public LocalizationKeyParams(String key, String... params) {
        this.key = LocalizationKey.fromKey(key);
        this.params = Arrays.asList(params);
        if (this.params.size() > 10) {
            throw new IllegalStateException("Translations can only have at most 10 parameters");
        }
    }

    /// Goes through the key once, so that what a parameter holds is not taken for a placeholder: a
    /// file name or a URL may well contain `%1` or `%20`.
    public String replacePlaceholders() {
        return PLACEHOLDER.matcher(key.getKey()).replaceAll(match -> {
            int index = Integer.parseInt(match.group(1));
            return Matcher.quoteReplacement(index < params.size() ? params.get(index) : match.group());
        });
    }
}
