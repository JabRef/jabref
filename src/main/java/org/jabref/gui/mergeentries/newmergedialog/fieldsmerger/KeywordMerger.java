package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

import java.util.Objects;

import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.BibEntryPreferences;

/**
 * A merger for the {@link StandardField#KEYWORDS} field
 * */
public class KeywordMerger implements FieldMerger {
    private final BibEntryPreferences bibEntryPreferences;

    public KeywordMerger(BibEntryPreferences bibEntryPreferences) {
        Objects.requireNonNull(bibEntryPreferences);
        this.bibEntryPreferences = bibEntryPreferences;
    }

    @Override
    public String merge(String keywordsA, String keywordsB) {
        Character delimiter = bibEntryPreferences.getKeywordSeparator();
        return KeywordList.merge(keywordsA, keywordsB, delimiter).getAsString(delimiter);
    }
}
