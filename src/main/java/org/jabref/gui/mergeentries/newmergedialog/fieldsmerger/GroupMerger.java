package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

/**
 * A merger for the {@link StandardField#GROUPS} field
 * */
public class GroupMerger implements FieldMerger {
    public static final String GROUPS_SEPARATOR = ", ";
    public static final Pattern GROUPS_SEPARATOR_REGEX = Pattern.compile("\s*,\s*");

    @Override
    public String merge(String groupsA, String groupsB) {
        if (StringUtil.isBlank(groupsA) && StringUtil.isBlank(groupsB)) {
            return "";
        } else if (StringUtil.isBlank(groupsA)) {
            return groupsB;
        } else if (StringUtil.isBlank(groupsB)) {
            return groupsA;
        } else {
            return Arrays.stream((groupsA + GROUPS_SEPARATOR + groupsB).split(GROUPS_SEPARATOR_REGEX.pattern()))
                         .distinct()
                         .collect(Collectors.joining(GROUPS_SEPARATOR));
        }
    }
}
