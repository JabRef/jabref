package org.jabref.logic.citationkeypattern;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record Pattern(String stringRepresentation, Category category) {
    public enum Category {
        AUTHOR_RELATED, EDITOR_RELATED, TITLE_RELATED, OTHER_FIELDS, BIBENTRY_FIELDS
    }

    public static final Pattern NULL_PATTERN = new Pattern("", Category.OTHER_FIELDS);

    // region - Author-related field markers
    public static final Pattern AUTHOR_YEAR = new Pattern("[auth_year]", Category.AUTHOR_RELATED);
    public static final Pattern AUTHOR_FIRST_FULL = new Pattern("[authFirstFull]", Category.AUTHOR_RELATED);
    public static final Pattern AUTHOR_FORE_INI = new Pattern("[authForeIni]", Category.AUTHOR_RELATED);
    public static final Pattern AUTHOR_ETAL = new Pattern("[auth.etal]", Category.AUTHOR_RELATED);
    public static final Pattern AUTHOR_ET_AL = new Pattern("[authEtAl]", Category.AUTHOR_RELATED);
    public static final Pattern AUTHOR_AUTH_EA = new Pattern("[auth.auth.ea]", Category.AUTHOR_RELATED);
    public static final Pattern AUTHORS = new Pattern("[authors]", Category.AUTHOR_RELATED);
    public static final Pattern AUTHORS_N = new Pattern("[authorsN]", Category.AUTHOR_RELATED);
    public static final Pattern AUTH_INI_N = new Pattern("[authIniN]", Category.AUTHOR_RELATED);
    public static final Pattern AUTH_N = new Pattern("[authN]", Category.AUTHOR_RELATED);
    public static final Pattern AUTH_N_M = new Pattern("[authN_M]", Category.AUTHOR_RELATED);
    public static final Pattern AUTHOR_INI = new Pattern("[authorIni]", Category.AUTHOR_RELATED);
    public static final Pattern AUTH_SHORT = new Pattern("[authshort]", Category.AUTHOR_RELATED);
    public static final Pattern AUTHORS_ALPHA = new Pattern("[authorsAlpha]", Category.AUTHOR_RELATED);
    public static final Pattern AUTHORS_ALPHA_LNI = new Pattern("[authorsAlphaLNI]", Category.AUTHOR_RELATED);
    public static final Pattern AUTHORS_LAST = new Pattern("[authorsLast]", Category.AUTHOR_RELATED);
    public static final Pattern AUTHORS_LAST_FORE_INI = new Pattern("[authorsLastForeIni]", Category.AUTHOR_RELATED);
    // endregion

    // region - Editor-related field markers
    public static final Pattern EDTR = new Pattern("[edtr]", Category.EDITOR_RELATED);
    public static final Pattern EDTR_INI_N = new Pattern("[edtrIniN]", Category.EDITOR_RELATED);
    public static final Pattern EDITORS = new Pattern("[editors]", Category.EDITOR_RELATED);
    public static final Pattern EDITOR_LAST = new Pattern("[editorLast]", Category.EDITOR_RELATED);
    public static final Pattern EDITOR_INI = new Pattern("[editorIni]", Category.EDITOR_RELATED);
    public static final Pattern EDTR_N = new Pattern("[edtrN]", Category.EDITOR_RELATED);
    public static final Pattern EDTR_N_M = new Pattern("[edtrN_M]", Category.EDITOR_RELATED);
    public static final Pattern EDTR_EDTR_EA = new Pattern("[edtr.edtr.ea]", Category.EDITOR_RELATED);
    public static final Pattern EDTRSHORT = new Pattern("[edtrshort]", Category.EDITOR_RELATED);
    public static final Pattern EDTR_FORE_INI = new Pattern("[edtrForeIni]", Category.EDITOR_RELATED);
    public static final Pattern EDITOR_LAST_FORE_INI = new Pattern("[editorLastForeIni]", Category.EDITOR_RELATED);
    // endregion

    // region - Title-related field markers
    public static final Pattern SHORTTITLE = new Pattern("[shorttitle]", Category.TITLE_RELATED);
    public static final Pattern SHORTTITLE_INI = new Pattern("[shorttitleINI]", Category.TITLE_RELATED);
    public static final Pattern VERYSHORTTITLE = new Pattern("[veryshorttitle]", Category.TITLE_RELATED);
    public static final Pattern CAMEL = new Pattern("[camel]", Category.TITLE_RELATED);
    public static final Pattern CAMEL_N = new Pattern("[camelN]", Category.TITLE_RELATED);
    public static final Pattern TITLE = new Pattern("[title]", Category.TITLE_RELATED);
    public static final Pattern FULLTITLE = new Pattern("[fulltitle]", Category.TITLE_RELATED);
    // endregion

    // region - Other field markers
    public static final Pattern ENTRYTYPE = new Pattern("[entrytype]", Category.OTHER_FIELDS);
    public static final Pattern FIRSTPAGE = new Pattern("[firstpage]", Category.OTHER_FIELDS);
    public static final Pattern PAGEPREFIX = new Pattern("[pageprefix]", Category.OTHER_FIELDS);
    public static final Pattern KEYWORD_N = new Pattern("[keywordN]", Category.OTHER_FIELDS);
    public static final Pattern KEYWORDS_N = new Pattern("[keywordsN]", Category.OTHER_FIELDS);
    public static final Pattern LASTPAGE = new Pattern("[lastpage]", Category.OTHER_FIELDS);
    public static final Pattern SHORTYEAR = new Pattern("[shortyear]", Category.OTHER_FIELDS);
    // endregion

    // region - Bibentry fields
    public static final Pattern AUTHOR = new Pattern("[AUTHOR]", Category.BIBENTRY_FIELDS);
    public static final Pattern DATE = new Pattern("[DATE]", Category.BIBENTRY_FIELDS);
    public static final Pattern DAY = new Pattern("[DAY]", Category.BIBENTRY_FIELDS);
    public static final Pattern GROUPS = new Pattern("[GROUPS]", Category.BIBENTRY_FIELDS);
    public static final Pattern MONTH = new Pattern("[MONTH]", Category.BIBENTRY_FIELDS);
    public static final Pattern YEAR = new Pattern("[YEAR]", Category.BIBENTRY_FIELDS);
    // endregion

    public Pattern(String stringRepresentation) {
        this(stringRepresentation, Category.OTHER_FIELDS);
    }

    public static List<Pattern> getAllPatterns() {
        return Arrays.stream(Pattern.class.getDeclaredFields())
                     .filter(field -> {
                         int modifiers = field.getModifiers();
                         return Modifier.isPublic(modifiers) &&
                                 Modifier.isStatic(modifiers) &&
                                 Modifier.isFinal(modifiers) &&
                                 field.getType() == Pattern.class &&
                                 !field.equals(Pattern.NULL_PATTERN);
                     })
                     .map(field -> {
                         try {
                             return (Pattern) field.get(null);
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
