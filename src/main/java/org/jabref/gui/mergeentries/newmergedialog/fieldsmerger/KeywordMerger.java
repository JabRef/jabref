package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.PreferencesService;

public class KeywordMerger implements FieldMerger {
    private final PreferencesService preferencesService;

    public KeywordMerger(PreferencesService preferencesService) {
        Objects.requireNonNull(preferencesService);
        this.preferencesService = preferencesService;
    }

    @Override
    public String merge(String keywordsA, String keywordsB) {
        String keywordSeparator = preferencesService.getGroupsPreferences().getKeywordSeparator() + " ";

        if (StringUtil.isBlank(keywordsA) && StringUtil.isBlank(keywordsB)) {
            return "";
        } else if (StringUtil.isBlank(keywordsA)) {
            return keywordsB;
        } else if (StringUtil.isBlank(keywordsB)) {
            return keywordsA;
        } else {
            Set<String> keywordsASet = new HashSet<>(Arrays.stream(keywordsA.split(keywordSeparator)).toList());
            List<String> keywordsBList = Arrays.stream(keywordsB.split(keywordSeparator)).toList();
            keywordsASet.addAll(keywordsBList);

            return String.join(keywordSeparator, keywordsASet);
        }
    }
}
