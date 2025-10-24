package org.jabref.gui.mergeentries.threewaymerge.fieldsmerger;

import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.KeywordList;

import org.jspecify.annotations.NonNull;

/// A merger for the {@link org.jabref.model.entry.field.StandardField#GROUPS} field
public class GroupMerger implements FieldMerger {

    private final @NonNull BibEntryPreferences bibEntryPreferences;

    public GroupMerger(@NonNull BibEntryPreferences bibEntryPreferences) {
        this.bibEntryPreferences = bibEntryPreferences;
    }

    @Override
    public String merge(String groupsA, String groupsB) {
        Character delimiter = bibEntryPreferences.getKeywordSeparator();

        if (StringUtil.isBlank(groupsA) && StringUtil.isBlank(groupsB)) {
            return "";
        } else if (StringUtil.isBlank(groupsA)) {
            return groupsB;
        } else if (StringUtil.isBlank(groupsB)) {
            return groupsA;
        } else {
            return KeywordList.merge(groupsA, groupsB, delimiter).getAsString(delimiter);
        }
    }
}
