package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            Set<String> leftGroups = new HashSet<>(Arrays.stream(groupsA.split(GROUPS_SEPARATOR)).toList());
            List<String> rightGroups = Arrays.stream(groupsB.split(GROUPS_SEPARATOR)).toList();
            leftGroups.addAll(rightGroups);

            return String.join(GROUPS_SEPARATOR, leftGroups);
        }
    }
}
