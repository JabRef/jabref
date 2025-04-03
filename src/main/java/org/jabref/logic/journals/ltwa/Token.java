package org.jabref.logic.journals.ltwa;

/**
 * Represent a token in the lexer.
 */
public record Token(
        Type type,
        String value,
        int position) {
    public enum Type {
        WORD,
        ABBREVIATION,
        STOPWORD,
        ARTICLE,
        /**
         * A word that represent section, chapter, series, etc., such as in
         * "Section 2"
         */
        PART,
        ORDINAL,
        SYMBOLS,
        HYPHEN,
        EOS;
    }
}
