package org.jabref.gui.mergeentries.threewaymerge.fieldsmerger;

import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.KeywordList;

import org.jspecify.annotations.NonNull;

/// A merger for the {@link org.jabref.model.entry.field.StandardField#KEYWORDS} field
public class KeywordMerger implements FieldMerger {
    private final BibEntryPreferences bibEntryPreferences;

    public KeywordMerger(@NonNull BibEntryPreferences bibEntryPreferences) {
        this.bibEntryPreferences = bibEntryPreferences;
    }

    @Override
    public String merge(String keywordsA, String keywordsB) {
        Character delimiter = bibEntryPreferences.getKeywordSeparator();
        return KeywordList.merge(keywordsA, keywordsB, delimiter).getAsString(delimiter);
    }
}
