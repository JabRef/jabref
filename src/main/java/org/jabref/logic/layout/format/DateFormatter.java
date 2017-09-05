package org.jabref.logic.layout.format;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.jabref.logic.layout.ParamLayoutFormatter;

public class DateFormatter implements ParamLayoutFormatter {

    private String formatString = "yyyy-MM-dd"; // Use ISO-format as default

    @Override
    public String format(String fieldText) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
        LocalDate date = LocalDate.parse(fieldText, DateTimeFormatter.ISO_LOCAL_DATE);
        return date.format(formatter);
    }

    @Override
    public void setArgument(String arg) {
        formatString = arg;
    }

}
