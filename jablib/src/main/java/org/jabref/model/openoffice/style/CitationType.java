package org.jabref.model.openoffice.style;

/*
 * Presentation types of citation groups.
 */
public enum CitationType {

    AUTHORYEAR_PAR,
    AUTHORYEAR_INTEXT,
    INVISIBLE_CIT,

    /// "Doe, 2003" - as AUTHORYEAR_PAR, but the style brackets are left to the user.
    /// See <[#7861](https://github.com/JabRef/jabref/issues/7861)>
    AUTHORYEAR_NOPAR,

    /// "Doe" - the author part only.
    AUTHOR_ONLY,

    /// "2003a" - the year part only, including the uniqueLetter.
    YEAR_ONLY;

    public boolean inParenthesis() {
        return switch (this) {
            case AUTHORYEAR_PAR,
                 INVISIBLE_CIT ->
                    true;
            case AUTHORYEAR_INTEXT,
                 AUTHORYEAR_NOPAR,
                 AUTHOR_ONLY,
                 YEAR_ONLY ->
                    false;
        };
    }

    public boolean withText() {
        return this != INVISIBLE_CIT;
    }

    /// Partial citation markers show only one of the (author, year) parts.
    ///
    /// They are never merged into a citation group because merging appends a bare
    /// uniqueLetter, which would render "Doe" as "Doea".
    public boolean isPartial() {
        return this == AUTHOR_ONLY || this == YEAR_ONLY;
    }
}


