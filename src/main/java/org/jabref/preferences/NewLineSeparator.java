package org.jabref.preferences;

public enum NewLineSeparator {
    CR,
    LF,
    CRLF;

    /**
     * An enum which contains the possible NewLineSeperators
     * Possible are CR ("\n"), LF ("\r") and the windows standard CR/LF.
     */

    public String toString() {
        switch (this) {
            case CR:
                return "CR";
            case LF:
                return "LF";
            default:
                return "CR/LF";
        }
    }

    /**
     * @return the name of the current mode as String
     */
    public String getEscapeChars() {
        switch (this) {
            case CR:
                return "\r";
            case LF:
                return "\n";
            default:
                return "\r\n";
        }
    }

    /**
     * Returns the {@link NewLineSeparator} that equals the given string.
     **/
    public static NewLineSeparator parse(String data) {
        switch (data) {
            case "\r":
                return CR;
            case "\n":
                return LF;
            default:
                return CRLF;
        }
    }
}

