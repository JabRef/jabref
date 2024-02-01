package org.jabref.model.openoffice.style;

/*
 * Presentation types of citation groups.
 */
public enum CitationType {

    AUTHORYEAR_PAR,
    AUTHORYEAR_INTEXT,
    AUTHORYEAR_PLAIN,
    INVISIBLE_CIT;

    public boolean inParenthesis() {
        return switch (this) {
            case AUTHORYEAR_PAR, INVISIBLE_CIT -> true;
            case AUTHORYEAR_INTEXT, AUTHORYEAR_PLAIN -> false;
        };
    }

    public boolean plainCit() {
        return switch (this) {
            case AUTHORYEAR_PAR, INVISIBLE_CIT, AUTHORYEAR_INTEXT -> false;
            case AUTHORYEAR_PLAIN -> true;
        };
    }

    public boolean withText() {
        return this != INVISIBLE_CIT;
    }
}


