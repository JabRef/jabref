package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

import java.util.Objects;

import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

/**
 * A merger for the {@link StandardField#GROUPS} field
 * */
public class GroupMerger implements FieldMerger {
//    public static final String GROUPS_SEPARATOR = ", ";
//    public static final Pattern GROUPS_SEPARATOR_REGEX = Pattern.compile("\s*,\s*");

    private final BibEntryPreferences bibEntryPreferences;

    public GroupMerger(BibEntryPreferences bibEntryPreferences) {
        Objects.requireNonNull(bibEntryPreferences);
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
