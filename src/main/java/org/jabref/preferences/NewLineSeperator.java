package org.jabref.preferences;

public enum NewLineSeperator {
    CR,
    LF,
    CRLF;

    /**
     * An enum which contains the possible NewLineSeperators
     * Possible are CR ("\n"), LF ("\r") and the windows standard CR/LF.
     */

    /**
     * @return the name of the current mode as String
     */
    public String getEscapeSign() {
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
     * Returns the {@link NewLineSeperator} that equals the given string.
     **/
    public static NewLineSeperator parse(String data) {
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

