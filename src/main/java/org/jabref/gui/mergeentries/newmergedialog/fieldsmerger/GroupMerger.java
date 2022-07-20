package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.jabref.model.strings.StringUtil;

public class GroupMerger implements FieldMerger {
    public static final String GROUPS_SEPARATOR = ", ";

    @Override
    public String merge(String groupsA, String groupsB) {
        if (StringUtil.isBlank(groupsA) && StringUtil.isBlank(groupsB)) {
            return "";
        } else if (StringUtil.isBlank(groupsA)) {
            return groupsB;
        } else if (StringUtil.isBlank(groupsB)) {
            return groupsA;
        } else {
            return Arrays.stream((groupsA + GROUPS_SEPARATOR + groupsB).split(GROUPS_SEPARATOR))
                         .distinct()
                         .collect(Collectors.joining(GROUPS_SEPARATOR));
        }
    }
}
