package org.jabref.logic.openoffice;

/**
 * Exception used to indicate that the plugin attempted to set a paragraph format that is
 * not defined in the current OpenOffice document.
 */
public class UndefinedParagraphFormatException extends Exception {

    private final String formatName;


    public UndefinedParagraphFormatException(String formatName) {
        super();
        this.formatName = formatName;
    }

    public String getFormatName() {
        return formatName;
    }
}
