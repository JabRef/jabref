package org.jabref.logic.journals;

public class LwtaAbbreviation {
    private String unAbbreviated;
    private String abbreviation;
    private Position position;
    private boolean allowsSuffix;
    private boolean allowsPrefix;

    enum Position {
        ENDS_WORD, STARTS_WORD, IN_WORD, FULL_WORD
    }

    LwtaAbbreviation(String unAbbreviated, String abbreviation, Position position, boolean allowsPrefix, boolean allowsSuffix) {
        this.unAbbreviated = unAbbreviated;
        this.abbreviation = abbreviation;
        this.position = position;
        this.allowsPrefix = allowsPrefix;
        this.allowsSuffix = allowsSuffix;
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

    public boolean getAllowsPrefix() {
        return allowsPrefix;
    }

    public boolean getAllowsSuffix() {
        return allowsSuffix;
    }
}
