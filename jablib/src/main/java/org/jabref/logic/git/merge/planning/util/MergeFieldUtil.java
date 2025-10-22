package org.jabref.logic.git.merge.planning.util;

import java.util.Objects;

import org.jabref.model.entry.field.Field;

public final class MergeFieldUtil {
    public static boolean isMetaField(Field field) {
        String name = field.getName();
        return name.startsWith("_") || "_jabref_shared".equalsIgnoreCase(name);
    }

    public static boolean notEqual(String a, String b) {
        return !Objects.equals(a, b);
    }
}
