package org.jabref.logic.citationkeypattern;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public record CitationKeyPattern(String stringRepresentation, Category category) {
    public enum Category {
        AUTHOR_RELATED, EDITOR_RELATED, TITLE_RELATED, OTHER_FIELDS, BIBENTRY_FIELDS
    }

    public static final CitationKeyPattern NULL_CITATION_KEY_PATTERN = new CitationKeyPattern("", Category.OTHER_FIELDS);

    private static final List<CitationKeyPattern> AUTHOR_PATTERNS =
            Stream.of("[auth_year]",
                          "[authFirstFull]",
                          "[authForeIni]",
                          "[auth.etal]",
                          "[authEtAl]",
                          "[auth.auth.ea]",
                          "[authors]",
                          "[authorsN]",
                          "[authIniN]",
                          "[authN]",
                          "[authN_M]",
                          "[authorIni]",
                          "[authshort]",
                          "[authorsAlpha]",
                          "[authorsAlphaLNI]",
                          "[authorsLast]",
                          "[authorsLastForeIni]")
                  .map(pattern -> new CitationKeyPattern(pattern, Category.AUTHOR_RELATED))
                  .toList();

    private static final List<CitationKeyPattern> EDITOR_PATTERNS =
            Stream.of("[edtr]",
                          "[edtrIniN]",
                          "[editors]",
                          "[editorLast]",
                          "[editorIni]",
                          "[edtrN]",
                          "[edtrN_M]",
                          "[edtr.edtr.ea]",
                          "[edtrshort]",
                          "[edtrForeIni]",
                          "[editorLastForeIni]")
                  .map(pattern -> new CitationKeyPattern(pattern, Category.EDITOR_RELATED))
                  .toList();

    private static final List<CitationKeyPattern> TITLE_PATTERNS =
            Stream.of("[shorttitle]",
                          "[shorttitleINI]",
                          "[veryshorttitle]",
                          "[camel]",
                          "[camelN]",
                          "[title]",
                          "[fulltitle]")
                  .map(pattern -> new CitationKeyPattern(pattern, Category.TITLE_RELATED))
                  .toList();

    private static final List<CitationKeyPattern> OTHER_FIELD_PATTERNS =
            Stream.of("[entrytype]",
                          "[firstpage]",
                          "[pageprefix]",
                          "[keywordN]",
                          "[keywordsN]",
                          "[lastpage]",
                          "[shortyear]")
                  .map(pattern -> new CitationKeyPattern(pattern, Category.OTHER_FIELDS))
                  .toList();

    private static final List<CitationKeyPattern> BIBENTRY_FIELDS =
            Stream.of("[AUTHOR]",
                          "[DATE]",
                          "[DAY]",
                          "[GROUPS]",
                          "[MONTH]",
                          "[YEAR]")
                  .map(pattern -> new CitationKeyPattern(pattern, Category.BIBENTRY_FIELDS))
                  .toList();

    public CitationKeyPattern(String stringRepresentation) {
        this(stringRepresentation, Category.OTHER_FIELDS);
    }

    public static List<CitationKeyPattern> getAllPatterns() {
        List<CitationKeyPattern> result = new ArrayList<>();
        result.addAll(AUTHOR_PATTERNS);
        result.addAll(EDITOR_PATTERNS);
        result.addAll(TITLE_PATTERNS);
        result.addAll(OTHER_FIELD_PATTERNS);
        result.addAll(BIBENTRY_FIELDS);
        return result;
    }

    public Category getCategory() {
        return this.category;
    }
}
