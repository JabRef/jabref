package org.jabref.logic.citationkeypattern;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record CitationKeyPattern(String stringRepresentation, Category category) {
    public enum Category {
        AUTHOR_RELATED, EDITOR_RELATED, TITLE_RELATED, OTHER_FIELDS, BIBENTRY_FIELDS
    }

    public static final CitationKeyPattern NULL_CITATION_KEY_PATTERN = new CitationKeyPattern("", Category.OTHER_FIELDS);

    // region - Author-related field markers
    public static final CitationKeyPattern AUTHOR_YEAR = new CitationKeyPattern("[auth_year]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHOR_FIRST_FULL = new CitationKeyPattern("[authFirstFull]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHOR_FORE_INI = new CitationKeyPattern("[authForeIni]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHOR_ETAL = new CitationKeyPattern("[auth.etal]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHOR_ET_AL = new CitationKeyPattern("[authEtAl]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHOR_AUTH_EA = new CitationKeyPattern("[auth.auth.ea]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHORS = new CitationKeyPattern("[authors]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHORS_N = new CitationKeyPattern("[authorsN]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTH_INI_N = new CitationKeyPattern("[authIniN]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTH_N = new CitationKeyPattern("[authN]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTH_N_M = new CitationKeyPattern("[authN_M]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHOR_INI = new CitationKeyPattern("[authorIni]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTH_SHORT = new CitationKeyPattern("[authshort]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHORS_ALPHA = new CitationKeyPattern("[authorsAlpha]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHORS_ALPHA_LNI = new CitationKeyPattern("[authorsAlphaLNI]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHORS_LAST = new CitationKeyPattern("[authorsLast]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHORS_LAST_FORE_INI = new CitationKeyPattern("[authorsLastForeIni]", Category.AUTHOR_RELATED);
    // endregion

    // region - Editor-related field markers
    public static final CitationKeyPattern EDTR = new CitationKeyPattern("[edtr]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDTR_INI_N = new CitationKeyPattern("[edtrIniN]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDITORS = new CitationKeyPattern("[editors]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDITOR_LAST = new CitationKeyPattern("[editorLast]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDITOR_INI = new CitationKeyPattern("[editorIni]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDTR_N = new CitationKeyPattern("[edtrN]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDTR_N_M = new CitationKeyPattern("[edtrN_M]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDTR_EDTR_EA = new CitationKeyPattern("[edtr.edtr.ea]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDTRSHORT = new CitationKeyPattern("[edtrshort]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDTR_FORE_INI = new CitationKeyPattern("[edtrForeIni]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDITOR_LAST_FORE_INI = new CitationKeyPattern("[editorLastForeIni]", Category.EDITOR_RELATED);
    // endregion

    // region - Title-related field markers
    public static final CitationKeyPattern SHORTTITLE = new CitationKeyPattern("[shorttitle]", Category.TITLE_RELATED);
    public static final CitationKeyPattern SHORTTITLE_INI = new CitationKeyPattern("[shorttitleINI]", Category.TITLE_RELATED);
    public static final CitationKeyPattern VERYSHORTTITLE = new CitationKeyPattern("[veryshorttitle]", Category.TITLE_RELATED);
    public static final CitationKeyPattern CAMEL = new CitationKeyPattern("[camel]", Category.TITLE_RELATED);
    public static final CitationKeyPattern CAMEL_N = new CitationKeyPattern("[camelN]", Category.TITLE_RELATED);
    public static final CitationKeyPattern TITLE = new CitationKeyPattern("[title]", Category.TITLE_RELATED);
    public static final CitationKeyPattern FULLTITLE = new CitationKeyPattern("[fulltitle]", Category.TITLE_RELATED);
    // endregion

    // region - Other field markers
    public static final CitationKeyPattern ENTRYTYPE = new CitationKeyPattern("[entrytype]", Category.OTHER_FIELDS);
    public static final CitationKeyPattern FIRSTPAGE = new CitationKeyPattern("[firstpage]", Category.OTHER_FIELDS);
    public static final CitationKeyPattern PAGEPREFIX = new CitationKeyPattern("[pageprefix]", Category.OTHER_FIELDS);
    public static final CitationKeyPattern KEYWORD_N = new CitationKeyPattern("[keywordN]", Category.OTHER_FIELDS);
    public static final CitationKeyPattern KEYWORDS_N = new CitationKeyPattern("[keywordsN]", Category.OTHER_FIELDS);
    public static final CitationKeyPattern LASTPAGE = new CitationKeyPattern("[lastpage]", Category.OTHER_FIELDS);
    public static final CitationKeyPattern SHORTYEAR = new CitationKeyPattern("[shortyear]", Category.OTHER_FIELDS);
    // endregion

    // region - Bibentry fields
    public static final CitationKeyPattern AUTHOR = new CitationKeyPattern("[AUTHOR]", Category.BIBENTRY_FIELDS);
    public static final CitationKeyPattern DATE = new CitationKeyPattern("[DATE]", Category.BIBENTRY_FIELDS);
    public static final CitationKeyPattern DAY = new CitationKeyPattern("[DAY]", Category.BIBENTRY_FIELDS);
    public static final CitationKeyPattern GROUPS = new CitationKeyPattern("[GROUPS]", Category.BIBENTRY_FIELDS);
    public static final CitationKeyPattern MONTH = new CitationKeyPattern("[MONTH]", Category.BIBENTRY_FIELDS);
    public static final CitationKeyPattern YEAR = new CitationKeyPattern("[YEAR]", Category.BIBENTRY_FIELDS);
    // endregion

    public CitationKeyPattern(String stringRepresentation) {
        this(stringRepresentation, Category.OTHER_FIELDS);
    }

    public static List<CitationKeyPattern> getAllPatterns() {
        return Arrays.stream(CitationKeyPattern.class.getDeclaredFields())
                     .filter(field -> {
                         int modifiers = field.getModifiers();
                         return Modifier.isPublic(modifiers) &&
                                 Modifier.isStatic(modifiers) &&
                                 Modifier.isFinal(modifiers) &&
                                 field.getType() == CitationKeyPattern.class &&
                                 !field.equals(CitationKeyPattern.NULL_CITATION_KEY_PATTERN);
                     })
                     .map(field -> {
                         try {
                             return (CitationKeyPattern) field.get(null);
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
