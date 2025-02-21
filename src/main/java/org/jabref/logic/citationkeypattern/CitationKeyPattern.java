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
    public static final CitationKeyPattern AUTHOR_YEAR = new CitationKeyPattern("[auth]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHOR_FIRST_FULL = new CitationKeyPattern("[authFirstFull]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHOR_FORE_INI = new CitationKeyPattern("[authForeIni]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHOR_ETAL = new CitationKeyPattern("[auth.etal]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHOR_ET_AL = new CitationKeyPattern("[authEtAl]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHORS = new CitationKeyPattern("[authors]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHORS_N = new CitationKeyPattern("[authorsN]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTH_INI_N = new CitationKeyPattern("[authIniN]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTH_N = new CitationKeyPattern("[authN]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTH_SHORT = new CitationKeyPattern("[authshort]", Category.AUTHOR_RELATED);
    public static final CitationKeyPattern AUTHORS_ALPHA = new CitationKeyPattern("[authorsAlpha]", Category.AUTHOR_RELATED);
    // endregion

    // region - Editor-related field markers
    public static final CitationKeyPattern EDTR = new CitationKeyPattern("[edtr]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDITORS = new CitationKeyPattern("[editors]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDITOR_LAST = new CitationKeyPattern("[editorLast]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDITOR_INI = new CitationKeyPattern("[editorIni]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDTR_N = new CitationKeyPattern("[edtrN]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDTR_N_M = new CitationKeyPattern("[edtrN_M]", Category.EDITOR_RELATED);
    public static final CitationKeyPattern EDITOR_LAST_FORE_INI = new CitationKeyPattern("[editorLastForeIni]", Category.EDITOR_RELATED);
    // endregion

    // region - Title-related field markers
    public static final CitationKeyPattern SHORTTITLE = new CitationKeyPattern("[shorttitle]", Category.TITLE_RELATED);
    public static final CitationKeyPattern TITLE = new CitationKeyPattern("[title]", Category.TITLE_RELATED);
    public static final CitationKeyPattern FULLTITLE = new CitationKeyPattern("[fulltitle]", Category.TITLE_RELATED);
    // endregion

    // region - Other field markers
    public static final CitationKeyPattern ENTRYTYPE = new CitationKeyPattern("[entrytype]", Category.OTHER_FIELDS);
    public static final CitationKeyPattern FIRSTPAGE = new CitationKeyPattern("[firstpage]", Category.OTHER_FIELDS);
    public static final CitationKeyPattern LASTPAGE = new CitationKeyPattern("[lastpage]", Category.OTHER_FIELDS);
    public static final CitationKeyPattern SHORTYEAR = new CitationKeyPattern("[shortyear]", Category.OTHER_FIELDS);
    // endregion

    // region - Bibentry fields
    public static final CitationKeyPattern AUTHOR = new CitationKeyPattern("[AUTHOR]", Category.BIBENTRY_FIELDS);
    public static final CitationKeyPattern DATE = new CitationKeyPattern("[DATE]", Category.BIBENTRY_FIELDS);
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
