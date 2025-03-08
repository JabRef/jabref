package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.StandardField;

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
        Character keywordSeparatorA = detectSeparator(keywordsA);
        Character keywordSeparatorB = detectSeparator(keywordsB);

        if (keywordSeparatorA != null && keywordSeparatorB != null && keywordSeparatorA != keywordSeparatorB) {
            throw new IllegalArgumentException("Multiple different separators detected in keywords");
        }

        this.bibEntryPreferences.setKeywordSeparator(keywordSeparatorA);
        Character delimiter = bibEntryPreferences.getKeywordSeparator();
        return KeywordList.merge(keywordsA, keywordsB, delimiter).getAsString(delimiter);
    }

    private Character detectSeparator(String keywords) {
        Map<Character, Integer> separatorCount = new HashMap<>();

        for (char c : keywords.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) {
                separatorCount.put(c, separatorCount.getOrDefault(c, 0) + 1);
            }
        }

        if (separatorCount.size() > 1) {
            throw new IllegalArgumentException("Multiple different separators detected in keywords: " + keywords);
        }

        return separatorCount.keySet().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No valid separator found in keywords: " + keywords));
    }
}
