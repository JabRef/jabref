package org.jabref.gui.mergeentries.threewaymerge.fieldsmerger;

import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.KeywordList;

import org.jspecify.annotations.NonNull;

/// A merger for the [org.jabref.model.entry.field.StandardField#GROUPS] field
public class GroupMerger implements FieldMerger {

    private final Character delimiter;

    public GroupMerger(@NonNull Character delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public String merge(String groupsA, String groupsB) {
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
