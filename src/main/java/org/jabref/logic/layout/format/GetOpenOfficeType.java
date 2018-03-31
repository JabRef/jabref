package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

/**
 * Change type of record to match the one used by OpenOffice formatter.
 */
public class GetOpenOfficeType implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        if ("Article".equalsIgnoreCase(fieldText)) {
            return "7";
        }
        if ("Book".equalsIgnoreCase(fieldText)) {
            return "1";
        }
        if ("Booklet".equalsIgnoreCase(fieldText)) {
            return "2";
        }
        if ("Inbook".equalsIgnoreCase(fieldText)) {
            return "5";
        }
        if ("Incollection".equalsIgnoreCase(fieldText)) {
            return "5";
        }
        if ("Inproceedings".equalsIgnoreCase(fieldText)) {
            return "6";
        }
        if ("Manual".equalsIgnoreCase(fieldText)) {
            return "8";
        }
        if ("Mastersthesis".equalsIgnoreCase(fieldText)) {
            return "9";
        }
        if ("Misc".equalsIgnoreCase(fieldText)) {
            return "10";
        }
        if ("Other".equalsIgnoreCase(fieldText)) {
            return "10";
        }
        if ("Phdthesis".equalsIgnoreCase(fieldText)) {
            return "9";
        }
        if ("Proceedings".equalsIgnoreCase(fieldText)) {
            return "3";
        }
        if ("Techreport".equalsIgnoreCase(fieldText)) {
            return "13";
        }
        if ("Unpublished".equalsIgnoreCase(fieldText)) {
            return "14";
        }
        // Default, Miscelaneous
        return "10";
    }
}
