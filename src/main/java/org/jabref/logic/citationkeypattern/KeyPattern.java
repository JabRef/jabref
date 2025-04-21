package org.jabref.logic.citationkeypattern;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record KeyPattern(String stringRepresentation, Category category) {
    public enum Category {
        AUTHOR_RELATED, EDITOR_RELATED, TITLE_RELATED, OTHER_FIELDS, BIBENTRY_FIELDS
    }

    public static final KeyPattern NULL_PATTERN = new KeyPattern("", Category.OTHER_FIELDS);

    // region - Author-related field markers
    public static final KeyPattern AUTHOR_YEAR = new KeyPattern("[auth_year]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTHOR_FIRST_FULL = new KeyPattern("[authFirstFull]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTHOR_FORE_INI = new KeyPattern("[authForeIni]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTHOR_ETAL = new KeyPattern("[auth.etal]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTHOR_ET_AL = new KeyPattern("[authEtAl]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTHOR_AUTH_EA = new KeyPattern("[auth.auth.ea]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTHORS = new KeyPattern("[authors]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTHORS_N = new KeyPattern("[authorsN]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTH_INI_N = new KeyPattern("[authIniN]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTH_N = new KeyPattern("[authN]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTH_N_M = new KeyPattern("[authN_M]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTHOR_INI = new KeyPattern("[authorIni]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTH_SHORT = new KeyPattern("[authshort]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTHORS_ALPHA = new KeyPattern("[authorsAlpha]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTHORS_ALPHA_LNI = new KeyPattern("[authorsAlphaLNI]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTHORS_LAST = new KeyPattern("[authorsLast]", Category.AUTHOR_RELATED);
    public static final KeyPattern AUTHORS_LAST_FORE_INI = new KeyPattern("[authorsLastForeIni]", Category.AUTHOR_RELATED);
    // endregion

    // region - Editor-related field markers
    public static final KeyPattern EDTR = new KeyPattern("[edtr]", Category.EDITOR_RELATED);
    public static final KeyPattern EDTR_INI_N = new KeyPattern("[edtrIniN]", Category.EDITOR_RELATED);
    public static final KeyPattern EDITORS = new KeyPattern("[editors]", Category.EDITOR_RELATED);
    public static final KeyPattern EDITOR_LAST = new KeyPattern("[editorLast]", Category.EDITOR_RELATED);
    public static final KeyPattern EDITOR_INI = new KeyPattern("[editorIni]", Category.EDITOR_RELATED);
    public static final KeyPattern EDTR_N = new KeyPattern("[edtrN]", Category.EDITOR_RELATED);
    public static final KeyPattern EDTR_N_M = new KeyPattern("[edtrN_M]", Category.EDITOR_RELATED);
    public static final KeyPattern EDTR_EDTR_EA = new KeyPattern("[edtr.edtr.ea]", Category.EDITOR_RELATED);
    public static final KeyPattern EDTRSHORT = new KeyPattern("[edtrshort]", Category.EDITOR_RELATED);
    public static final KeyPattern EDTR_FORE_INI = new KeyPattern("[edtrForeIni]", Category.EDITOR_RELATED);
    public static final KeyPattern EDITOR_LAST_FORE_INI = new KeyPattern("[editorLastForeIni]", Category.EDITOR_RELATED);
    // endregion

    // region - Title-related field markers
    public static final KeyPattern SHORTTITLE = new KeyPattern("[shorttitle]", Category.TITLE_RELATED);
    public static final KeyPattern SHORTTITLE_INI = new KeyPattern("[shorttitleINI]", Category.TITLE_RELATED);
    public static final KeyPattern VERYSHORTTITLE = new KeyPattern("[veryshorttitle]", Category.TITLE_RELATED);
    public static final KeyPattern CAMEL = new KeyPattern("[camel]", Category.TITLE_RELATED);
    public static final KeyPattern CAMEL_N = new KeyPattern("[camelN]", Category.TITLE_RELATED);
    public static final KeyPattern TITLE = new KeyPattern("[title]", Category.TITLE_RELATED);
    public static final KeyPattern FULLTITLE = new KeyPattern("[fulltitle]", Category.TITLE_RELATED);
    // endregion

    // region - Other field markers
    public static final KeyPattern ENTRYTYPE = new KeyPattern("[entrytype]", Category.OTHER_FIELDS);
    public static final KeyPattern FIRSTPAGE = new KeyPattern("[firstpage]", Category.OTHER_FIELDS);
    public static final KeyPattern PAGEPREFIX = new KeyPattern("[pageprefix]", Category.OTHER_FIELDS);
    public static final KeyPattern KEYWORD_N = new KeyPattern("[keywordN]", Category.OTHER_FIELDS);
    public static final KeyPattern KEYWORDS_N = new KeyPattern("[keywordsN]", Category.OTHER_FIELDS);
    public static final KeyPattern LASTPAGE = new KeyPattern("[lastpage]", Category.OTHER_FIELDS);
    public static final KeyPattern SHORTYEAR = new KeyPattern("[shortyear]", Category.OTHER_FIELDS);
    // endregion

    // region - Bibentry fields
    public static final KeyPattern AUTHOR = new KeyPattern("[AUTHOR]", Category.BIBENTRY_FIELDS);
    public static final KeyPattern DATE = new KeyPattern("[DATE]", Category.BIBENTRY_FIELDS);
    public static final KeyPattern DAY = new KeyPattern("[DAY]", Category.BIBENTRY_FIELDS);
    public static final KeyPattern GROUPS = new KeyPattern("[GROUPS]", Category.BIBENTRY_FIELDS);
    public static final KeyPattern MONTH = new KeyPattern("[MONTH]", Category.BIBENTRY_FIELDS);
    public static final KeyPattern YEAR = new KeyPattern("[YEAR]", Category.BIBENTRY_FIELDS);
    // endregion

    public KeyPattern(String stringRepresentation) {
        this(stringRepresentation, Category.OTHER_FIELDS);
    }

    public static List<KeyPattern> getAllPatterns() {
        return Arrays.stream(KeyPattern.class.getDeclaredFields())
                     .filter(field -> {
                         int modifiers = field.getModifiers();
                         return Modifier.isPublic(modifiers) &&
                                 Modifier.isStatic(modifiers) &&
                                 Modifier.isFinal(modifiers) &&
                                 field.getType() == KeyPattern.class &&
                                 !field.equals(KeyPattern.NULL_PATTERN);
                     })
                     .map(field -> {
                         try {
                             return (KeyPattern) field.get(null);
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
