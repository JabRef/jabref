package org.jabref.logic.journals;

public class LwtaAbbreviation {
    private String unAbbreviated;
    private String abbreviation;
    private Position position;

    enum Position {
        ENDS_WORD, STARTS_WORD, IN_WORD, FULL_WORD
    }

    LwtaAbbreviation(String unAbbreviated, String abbreviation, Position position) {
        this.unAbbreviated = unAbbreviated;
        this.abbreviation = abbreviation;
        this.position = position;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getUnAbbreviated() {
        return unAbbreviated;
    }

    public Position getPosition() {
        return position;
    }
}
