package org.jabref.preferences;

/**
 * An enum which contains the possible NewLineSeparators
 * Possible are CR ("\n"), LF ("\r") and the windows standard CR/LF.
 */
public enum NewLineSeparator {
    CR,
    LF,
    CRLF;

    public String getDisplayName() {
        switch (this) {
            case CR:
                return "CR (\"\\r\")";
            case LF:
                return "LF (\"\\n\")";
            default:
                return "CR/LF (\"\\r\\n\")";
        }
    }

    /**
     * @return the name of the current mode as String
     */
    public String toString() {
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

