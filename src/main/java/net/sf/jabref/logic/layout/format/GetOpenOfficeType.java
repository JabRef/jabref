package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

/**
 * Change type of record to match the one used by OpenOffice formatter.
 *
 * Based on the RemoveBrackets.java class (Revision 1.2) by mortenalver
 * @author $author$
 * @version $Revision$
 */
public class GetOpenOfficeType implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        if ("Article".equalsIgnoreCase(fieldText)) {
            return "0";
        }
        if ("Book".equalsIgnoreCase(fieldText)) {
            return "1";
        }
        if ("Booklet".equalsIgnoreCase(fieldText)) {
            return "2";
        }
        if ("BookExcerpt".equalsIgnoreCase(fieldText)) {
            return "4";
        }
        if ("BookExcerptWithTitle".equalsIgnoreCase(fieldText)) {
            return "5";
        }
        if ("Journal".equalsIgnoreCase(fieldText)) {
            return "7";
        }
        if ("Manual".equalsIgnoreCase(fieldText)) {
            return "8";
        }
        if ("Masterthesis".equalsIgnoreCase(fieldText)) {
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
        if ("Techreport".equalsIgnoreCase(fieldText)) {
            return "13";
        }
        if ("Unpublished".equalsIgnoreCase(fieldText)) {
            return "14";
        }
        if ("E-mail".equalsIgnoreCase(fieldText)) {
            return "15";
        }
        if ("WWWDocument".equalsIgnoreCase(fieldText)) {
            return "16";
        }
        // Default, Miscelaneous
        return "10";
    }
}
