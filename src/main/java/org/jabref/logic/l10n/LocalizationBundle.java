package org.jabref.logic.l10n;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * A bundle containing localized strings.
 * It wraps an ordinary resource bundle and performs escaping/unescaping of keys and values similar to
 * {@link Localization}. Needed to support JavaFX inline binding.
 */
public class LocalizationBundle extends ResourceBundle {

    private final ResourceBundle baseBundle;

    public LocalizationBundle(ResourceBundle baseBundle) {
        this.baseBundle = Objects.requireNonNull(baseBundle);
    }

    @Override
    protected Object handleGetObject(String key) {
        return Localization.translate(baseBundle, "message", key);
    }

    @Override
    public Enumeration<String> getKeys() {
        ArrayList<String> baseKeys = Collections.list(baseBundle.getKeys());
        List<String> unescapedKeys = baseKeys.stream().map(key -> new LocalizationKey(key).getTranslationValue())
                .collect(Collectors.toList());
        return Collections.enumeration(unescapedKeys);
    }
}
