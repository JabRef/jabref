package org.jabref.gui.mergeentries.threewaymerge.fieldsmerger;

import org.jabref.model.entry.KeywordList;

import org.jspecify.annotations.NonNull;

/// A merger for the [org.jabref.model.entry.field.StandardField#KEYWORDS] field
public class KeywordMerger implements FieldMerger {
    private final Character delimiter;

    public KeywordMerger(@NonNull Character delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public String merge(String keywordsA, String keywordsB) {
        return KeywordList.merge(keywordsA, keywordsB, delimiter).getAsString(delimiter);
    }
}
