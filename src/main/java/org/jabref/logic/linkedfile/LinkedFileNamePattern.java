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

    // region - Editor-related field markers
    public static final LinkedFileNamePattern EDTR = new LinkedFileNamePattern("[edtr]", Category.EDITOR_RELATED);
    public static final LinkedFileNamePattern EDTR_INI_N = new LinkedFileNamePattern("[edtrIniN]", Category.EDITOR_RELATED);
    public static final LinkedFileNamePattern EDITORS = new LinkedFileNamePattern("[editors]", Category.EDITOR_RELATED);
    public static final LinkedFileNamePattern EDITOR_LAST = new LinkedFileNamePattern("[editorLast]", Category.EDITOR_RELATED);
    public static final LinkedFileNamePattern EDITOR_INI = new LinkedFileNamePattern("[editorIni]", Category.EDITOR_RELATED);
    public static final LinkedFileNamePattern EDTR_N = new LinkedFileNamePattern("[edtrN]", Category.EDITOR_RELATED);
    public static final LinkedFileNamePattern EDTR_N_M = new LinkedFileNamePattern("[edtrN_M]", Category.EDITOR_RELATED);
    public static final LinkedFileNamePattern EDTR_EDTR_EA = new LinkedFileNamePattern("[edtr.edtr.ea]", Category.EDITOR_RELATED);
    public static final LinkedFileNamePattern EDTRSHORT = new LinkedFileNamePattern("[edtrshort]", Category.EDITOR_RELATED);
    public static final LinkedFileNamePattern EDTR_FORE_INI = new LinkedFileNamePattern("[edtrForeIni]", Category.EDITOR_RELATED);
    public static final LinkedFileNamePattern EDITOR_LAST_FORE_INI = new LinkedFileNamePattern("[editorLastForeIni]", Category.EDITOR_RELATED);
    // endregion

    // region - Title-related field markers
    public static final LinkedFileNamePattern SHORTTITLE = new LinkedFileNamePattern("[shorttitle]", Category.TITLE_RELATED);
    public static final LinkedFileNamePattern SHORTTITLE_INI = new LinkedFileNamePattern("[shorttitleINI]", Category.TITLE_RELATED);
    public static final LinkedFileNamePattern VERYSHORTTITLE = new LinkedFileNamePattern("[veryshorttitle]", Category.TITLE_RELATED);
    public static final LinkedFileNamePattern CAMEL = new LinkedFileNamePattern("[camel]", Category.TITLE_RELATED);
    public static final LinkedFileNamePattern CAMEL_N = new LinkedFileNamePattern("[camelN]", Category.TITLE_RELATED);
    public static final LinkedFileNamePattern TITLE = new LinkedFileNamePattern("[title]", Category.TITLE_RELATED);
    public static final LinkedFileNamePattern FULLTITLE = new LinkedFileNamePattern("[fulltitle]", Category.TITLE_RELATED);
    // endregion

    // region - Other field markers
    public static final LinkedFileNamePattern ENTRYTYPE = new LinkedFileNamePattern("[entrytype]", Category.OTHER_FIELDS);
    public static final LinkedFileNamePattern FIRSTPAGE = new LinkedFileNamePattern("[firstpage]", Category.OTHER_FIELDS);
    public static final LinkedFileNamePattern PAGEPREFIX = new LinkedFileNamePattern("[pageprefix]", Category.OTHER_FIELDS);
    public static final LinkedFileNamePattern KEYWORD_N = new LinkedFileNamePattern("[keywordN]", Category.OTHER_FIELDS);
    public static final LinkedFileNamePattern KEYWORDS_N = new LinkedFileNamePattern("[keywordsN]", Category.OTHER_FIELDS);
    public static final LinkedFileNamePattern LASTPAGE = new LinkedFileNamePattern("[lastpage]", Category.OTHER_FIELDS);
    public static final LinkedFileNamePattern SHORTYEAR = new LinkedFileNamePattern("[shortyear]", Category.OTHER_FIELDS);
    // endregion

    // region - Bibentry fields
    public static final LinkedFileNamePattern AUTHOR = new LinkedFileNamePattern("[AUTHOR]", Category.BIBENTRY_FIELDS);
    public static final LinkedFileNamePattern DATE = new LinkedFileNamePattern("[DATE]", Category.BIBENTRY_FIELDS);
    public static final LinkedFileNamePattern DAY = new LinkedFileNamePattern("[DAY]", Category.BIBENTRY_FIELDS);
    public static final LinkedFileNamePattern GROUPS = new LinkedFileNamePattern("[GROUPS]", Category.BIBENTRY_FIELDS);
    public static final LinkedFileNamePattern MONTH = new LinkedFileNamePattern("[MONTH]", Category.BIBENTRY_FIELDS);
    public static final LinkedFileNamePattern YEAR = new LinkedFileNamePattern("[YEAR]", Category.BIBENTRY_FIELDS);
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
