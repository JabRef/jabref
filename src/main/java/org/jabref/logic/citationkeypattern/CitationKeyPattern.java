package org.jabref.logic.citationkeypattern;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record CitationKeyPattern(String stringRepresentation) {
    // Public because needed for representing null value
    public static final CitationKeyPattern NULL_CITATION_KEY_PATTERN = new CitationKeyPattern("");

    // region - Author-related field markers
    private static final CitationKeyPattern AUTHOR_YEAR = new CitationKeyPattern("[auth]");
    private static final CitationKeyPattern AUTHOR_FIRST_FULL = new CitationKeyPattern("[authFirstFull]");
    private static final CitationKeyPattern AUTHOR_FORE_INI = new CitationKeyPattern("[authForeIni]");
    private static final CitationKeyPattern AUTHOR_ETAL = new CitationKeyPattern("[auth.etal]");
    private static final CitationKeyPattern AUTHOR_ET_AL = new CitationKeyPattern("[authEtAl]");
    private static final CitationKeyPattern AUTHOR_AUTH_EA = new CitationKeyPattern("[auth.auth.ea]");
    private static final CitationKeyPattern AUTHORS = new CitationKeyPattern("[authors]");
    private static final CitationKeyPattern AUTHORS_N = new CitationKeyPattern("[authorsN]");
    private static final CitationKeyPattern AUTH_INI_N = new CitationKeyPattern("[authIniN]");
    private static final CitationKeyPattern AUTH_N = new CitationKeyPattern("[authN]");
    private static final CitationKeyPattern AUTH_N_M = new CitationKeyPattern("[authN_M]");
    private static final CitationKeyPattern AUTHOR_INI = new CitationKeyPattern("[authorIni]");
    private static final CitationKeyPattern AUTH_SHORT = new CitationKeyPattern("[authshort]");
    private static final CitationKeyPattern AUTHORS_ALPHA = new CitationKeyPattern("[authorsAlpha]");
    private static final CitationKeyPattern AUTHORS_ALPHA_LNI = new CitationKeyPattern("[authorsAlphaLni]");
    private static final CitationKeyPattern AUTHORS_LAST = new CitationKeyPattern("[authorsLast]");
    private static final CitationKeyPattern AUTHORS_LAST_FORE_INI = new CitationKeyPattern("[authorsLastForeIni]");
    // endregion

    // region - Editor-related field markers
    private static final CitationKeyPattern EDTR = new CitationKeyPattern("[edtr]");
    private static final CitationKeyPattern EDTR_INI_N = new CitationKeyPattern("[edtrIniN]");
    private static final CitationKeyPattern EDITORS = new CitationKeyPattern("[editors]");
    private static final CitationKeyPattern EDITOR_LAST = new CitationKeyPattern("[editorLast]");
    private static final CitationKeyPattern EDITOR_INI = new CitationKeyPattern("[editorIni]");
    private static final CitationKeyPattern EDTR_N = new CitationKeyPattern("[edtrN]");
    private static final CitationKeyPattern EDTR_N_M = new CitationKeyPattern("[edtrN_M]");
    private static final CitationKeyPattern EDTR_EDTR_EA = new CitationKeyPattern("[edtr.edtr.ea]");
    private static final CitationKeyPattern EDTRSHORT = new CitationKeyPattern("[edtrshort]");
    private static final CitationKeyPattern EDTR_FORE_INI = new CitationKeyPattern("[edtrForeIni]");
    private static final CitationKeyPattern EDITOR_LAST_FORE_INI = new CitationKeyPattern("[editorLastForeIni]");
    // endregion

    // region - Title-related field markers
    private static final CitationKeyPattern SHORTTITLE = new CitationKeyPattern("[shorttitle]");
    private static final CitationKeyPattern SHORTTITLE_INI = new CitationKeyPattern("[shorttitleINI]");
    private static final CitationKeyPattern VERYSHORTTITLE = new CitationKeyPattern("[veryshorttitle]");
    private static final CitationKeyPattern CAMEL = new CitationKeyPattern("[camel]");
    private static final CitationKeyPattern CAMEL_N = new CitationKeyPattern("[camelN]");
    private static final CitationKeyPattern TITLE = new CitationKeyPattern("[title]");
    private static final CitationKeyPattern FULLTITLE = new CitationKeyPattern("[fulltitle]");
    // endregion

    // region - Other field markers
    private static final CitationKeyPattern ENTRYTYPE = new CitationKeyPattern("[entrytype]");
    private static final CitationKeyPattern FIRSTPAGE = new CitationKeyPattern("[firstpage]");
    private static final CitationKeyPattern PAGEPREFIX = new CitationKeyPattern("[pageprefix]");
    private static final CitationKeyPattern KEYWORD_N = new CitationKeyPattern("[keywordN]");
    private static final CitationKeyPattern KEYWORDS_N = new CitationKeyPattern("[keywordsN]");
    private static final CitationKeyPattern LASTPAGE = new CitationKeyPattern("[lastpage]");
    private static final CitationKeyPattern SHORTYEAR = new CitationKeyPattern("[shortyear]");
    // endregion

    // region - Bibentry fields
    private static final CitationKeyPattern AUTHOR = new CitationKeyPattern("[AUTHOR]");
    private static final CitationKeyPattern DATE = new CitationKeyPattern("[DATE]");
    private static final CitationKeyPattern DAY = new CitationKeyPattern("[DAY]");
    private static final CitationKeyPattern GROUPS = new CitationKeyPattern("[GROUPS]");
    private static final CitationKeyPattern MONTH = new CitationKeyPattern("[MONTH]");
    private static final CitationKeyPattern YEAR = new CitationKeyPattern("[YEAR]");
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
