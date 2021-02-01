package org.jabref.logic.layout.format;

import java.util.List;

import org.jabref.logic.layout.AbstractParamLayoutFormatter;

/**
 * This formatter takes two arguments and examines the field text.
 * If the field text represents multiple individuals, that is it contains the string "and"
 * then the field text is replaced with the first argument, otherwise it is replaced with the second.
 * For example:
 *
 * <p>
 * \format[IfPlural(Eds.,Ed.)]{\editor}
 * <p>
 * Should expand to 'Eds.' if the document has more than one editor and 'Ed.' if it only has one.
 */
public class IfPlural extends AbstractParamLayoutFormatter {

    private String pluralText;
    private String singularText;

    @Override
    public void setArgument(String arg) {
        List<String> parts = AbstractParamLayoutFormatter.parseArgument(arg);

        if (parts.size() < 2) {
            return; // TODO: too few arguments. Print an error message here?
        }
        pluralText = parts.get(0);
        singularText = parts.get(1);
    }

    @Override
    public String format(String fieldText) {
        if ((fieldText == null) || fieldText.isEmpty() || (pluralText == null)) {
            return ""; // TODO: argument missing or invalid. Print an error message here?
        }
        if (fieldText.matches(".*\\sand\\s.*")) {
            return pluralText;
        } else {
            return singularText;
        }
    }
}
