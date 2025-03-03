package org.jabref.logic.linkedfile;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record LinkedFileNamePattern(String stringRepresentation, Category category) {
    public enum Category {
        AUTHOR_RELATED, EDITOR_RELATED, TITLE_RELATED, OTHER_FIELDS, BIBENTRY_FIELDS
    }

    public static final LinkedFileNamePattern NULL_LINKED_FILE_NAME_PATTERN = new LinkedFileNamePattern("", Category.OTHER_FIELDS);

    // region - Author-related field markers
    public static final LinkedFileNamePattern AUTHOR_YEAR = new LinkedFileNamePattern("[auth_year]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTHOR_FIRST_FULL = new LinkedFileNamePattern("[authFirstFull]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTHOR_FORE_INI = new LinkedFileNamePattern("[authForeIni]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTHOR_ETAL = new LinkedFileNamePattern("[auth.etal]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTHOR_ET_AL = new LinkedFileNamePattern("[authEtAl]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTHOR_AUTH_EA = new LinkedFileNamePattern("[auth.auth.ea]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTHORS = new LinkedFileNamePattern("[authors]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTHORS_N = new LinkedFileNamePattern("[authorsN]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTH_INI_N = new LinkedFileNamePattern("[authIniN]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTH_N = new LinkedFileNamePattern("[authN]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTH_N_M = new LinkedFileNamePattern("[authN_M]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTHOR_INI = new LinkedFileNamePattern("[authorIni]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTH_SHORT = new LinkedFileNamePattern("[authshort]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTHORS_ALPHA = new LinkedFileNamePattern("[authorsAlpha]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTHORS_ALPHA_LNI = new LinkedFileNamePattern("[authorsAlphaLNI]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTHORS_LAST = new LinkedFileNamePattern("[authorsLast]", Category.AUTHOR_RELATED);
    public static final LinkedFileNamePattern AUTHORS_LAST_FORE_INI = new LinkedFileNamePattern("[authorsLastForeIni]", Category.AUTHOR_RELATED);
    // endregion

    public LinkedFileNamePattern(String stringRepresentation) {
        this(stringRepresentation, Category.OTHER_FIELDS);
    }

    public static List<LinkedFileNamePattern> getAllPatterns() {
        return Arrays.stream(LinkedFileNamePattern.class.getDeclaredFields())
                     .filter(field -> {
                         int modifiers = field.getModifiers();
                         return Modifier.isPublic(modifiers) &&
                                 Modifier.isStatic(modifiers) &&
                                 Modifier.isFinal(modifiers) &&
                                 field.getType() == LinkedFileNamePattern.class &&
                                 !field.equals(LinkedFileNamePattern.NULL_LINKED_FILE_NAME_PATTERN);
                     })
                     .map(field -> {
                         try {
                             return (LinkedFileNamePattern) field.get(null);
                         } catch (IllegalAccessException e) {
                             throw new RuntimeException("Could not access field", e);
                         }
                     })
                     .collect(Collectors.toList());
    }

    public Category getCategory() {
        return this.category;
    }
}
