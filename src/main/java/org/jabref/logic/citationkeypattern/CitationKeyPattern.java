package org.jabref.logic.citationkeypattern;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record CitationKeyPattern(String stringRepresentation) {
    public static final CitationKeyPattern NULL_CITATION_KEY_PATTERN = new CitationKeyPattern("");

    // region - Author-related field markers
    public static final CitationKeyPattern AUTHOR_YEAR = new CitationKeyPattern("[auth]");
    public static final CitationKeyPattern AUTHOR_FIRST_FULL = new CitationKeyPattern("[authFirstFull]");
    public static final CitationKeyPattern AUTHOR_FORE_INI = new CitationKeyPattern("[authForeIni]");
    public static final CitationKeyPattern AUTHOR_ETAL = new CitationKeyPattern("[auth.etal]");
    public static final CitationKeyPattern AUTHOR_ET_AL = new CitationKeyPattern("[authEtAl]");
    public static final CitationKeyPattern AUTHOR_AUTH_EA = new CitationKeyPattern("[auth.auth.ea]");
    public static final CitationKeyPattern AUTHORS = new CitationKeyPattern("[authors]");
    public static final CitationKeyPattern AUTHORS_N = new CitationKeyPattern("[authorsN]");
    public static final CitationKeyPattern AUTH_INI_N = new CitationKeyPattern("[authIniN]");
    public static final CitationKeyPattern AUTH_N = new CitationKeyPattern("[authN]");
    public static final CitationKeyPattern AUTH_N_M = new CitationKeyPattern("[authN_M]");
    public static final CitationKeyPattern AUTHOR_INI = new CitationKeyPattern("[authorIni]");
    public static final CitationKeyPattern AUTH_SHORT = new CitationKeyPattern("[authshort]");
    public static final CitationKeyPattern AUTHORS_ALPHA = new CitationKeyPattern("[authorsAlpha]");
    public static final CitationKeyPattern AUTHORS_ALPHA_LNI = new CitationKeyPattern("[authorsAlphaLni]");
    public static final CitationKeyPattern AUTHORS_LAST = new CitationKeyPattern("[authorsLast]");
    public static final CitationKeyPattern AUTHORS_LAST_FORE_INI = new CitationKeyPattern("[authorsLastForeIni]");
    // endregion

    // region - Editor-related field markers
    public static final CitationKeyPattern EDTR = new CitationKeyPattern("[edtr]");
    public static final CitationKeyPattern EDTR_INI_N = new CitationKeyPattern("[edtrIniN]");
    public static final CitationKeyPattern EDITORS = new CitationKeyPattern("[editors]");
    public static final CitationKeyPattern EDITOR_LAST = new CitationKeyPattern("[editorLast]");
    public static final CitationKeyPattern EDITOR_INI = new CitationKeyPattern("[editorIni]");
    public static final CitationKeyPattern EDTR_N = new CitationKeyPattern("[edtrN]");
    public static final CitationKeyPattern EDTR_N_M = new CitationKeyPattern("[edtrN_M]");
    public static final CitationKeyPattern EDTR_EDTR_EA = new CitationKeyPattern("[edtr.edtr.ea]");
    public static final CitationKeyPattern EDTRSHORT = new CitationKeyPattern("[edtrshort]");
    public static final CitationKeyPattern EDTR_FORE_INI = new CitationKeyPattern("[edtrForeIni]");
    public static final CitationKeyPattern EDITOR_LAST_FORE_INI = new CitationKeyPattern("[editorLastForeIni]");
    // endregion

    // region - Title-related field markers
    public static final CitationKeyPattern SHORTTITLE = new CitationKeyPattern("[shorttitle]");
    public static final CitationKeyPattern SHORTTITLE_INI = new CitationKeyPattern("[shorttitleINI]");
    public static final CitationKeyPattern VERYSHORTTITLE = new CitationKeyPattern("[veryshorttitle]");
    public static final CitationKeyPattern CAMEL = new CitationKeyPattern("[camel]");
    public static final CitationKeyPattern CAMEL_N = new CitationKeyPattern("[camelN]");
    public static final CitationKeyPattern TITLE = new CitationKeyPattern("[title]");
    public static final CitationKeyPattern FULLTITLE = new CitationKeyPattern("[fulltitle]");
    // endregion

    // region - Other field markers
    public static final CitationKeyPattern ENTRYTYPE = new CitationKeyPattern("[entrytype]");
    public static final CitationKeyPattern FIRSTPAGE = new CitationKeyPattern("[firstpage]");
    public static final CitationKeyPattern PAGEPREFIX = new CitationKeyPattern("[pageprefix]");
    public static final CitationKeyPattern KEYWORD_N = new CitationKeyPattern("[keywordN]");
    public static final CitationKeyPattern KEYWORDS_N = new CitationKeyPattern("[keywordsN]");
    public static final CitationKeyPattern LASTPAGE = new CitationKeyPattern("[lastpage]");
    public static final CitationKeyPattern SHORTYEAR = new CitationKeyPattern("[shortyear]");
    // endregion

    // region - Bibentry fields
    public static final CitationKeyPattern AUTHOR = new CitationKeyPattern("[AUTHOR]");
    public static final CitationKeyPattern DATE = new CitationKeyPattern("[DATE]");
    public static final CitationKeyPattern DAY = new CitationKeyPattern("[DAY]");
    public static final CitationKeyPattern GROUPS = new CitationKeyPattern("[GROUPS]");
    public static final CitationKeyPattern MONTH = new CitationKeyPattern("[MONTH]");
    public static final CitationKeyPattern YEAR = new CitationKeyPattern("[YEAR]");
    // endregion

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
}
