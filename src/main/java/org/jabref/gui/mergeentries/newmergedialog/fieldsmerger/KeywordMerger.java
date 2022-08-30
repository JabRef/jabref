package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

import java.util.Objects;

import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

/**
 * A merger for the {@link StandardField#KEYWORDS} field
 * */
public class KeywordMerger implements FieldMerger {
    private final PreferencesService preferencesService;

    public KeywordMerger(PreferencesService preferencesService) {
        Objects.requireNonNull(preferencesService);
        this.preferencesService = preferencesService;
    }

    @Override
    public String merge(String keywordsA, String keywordsB) {
        Character delimiter = preferencesService.getGroupsPreferences().getKeywordSeparator();
        return KeywordList.merge(keywordsA, keywordsB, delimiter).getAsString(delimiter);
    }
}
