package org.jabref.model.openoffice.style;

/*
 * Presentation types of citation groups.
 */
public enum CitationType {

    AUTHORYEAR_PAR,
    AUTHORYEAR_INTEXT,
    INVISIBLE_CIT;

    public boolean inParenthesis() {
        return switch (this) {
            case AUTHORYEAR_PAR, INVISIBLE_CIT -> true;
            case AUTHORYEAR_INTEXT -> false;
        };
    }

    public boolean withText() {
        return (this != INVISIBLE_CIT);
    }
}


