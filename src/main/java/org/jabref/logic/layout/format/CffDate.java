package org.jabref.logic.layout.format;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.util.OS;

/**
 * This class is used to parse dates for CFF exports. Since we do not know if the input String contains
 *         year, month and day, we must go through all these cases to return the best CFF format possible.
 *         Different cases are stated below.
 * <p>
 *         Year, Month and Day contained   => preferred-citation:
 *                                              date-released: yyyy-mm-dd
 * <p>
 *         Year and Month contained        => preferred-citation
 *                                              ...
 *                                              month: mm
 *                                              year: yyyy
 * <p>
 *         Year contained                  => preferred-citation:
 *                                              ...
 *                                              year: yyyy
 * <p>
 *         Poorly formatted                => preferred-citation:
 *                                              ...
 *                                              issue-date: text-as-is
 */
public class CffDate implements LayoutFormatter {
    @Override
    public String format(String fieldText) {
        StringBuilder builder = new StringBuilder();
        String formatString = "yyyy-MM-dd";
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
            LocalDate date = LocalDate.parse(fieldText, DateTimeFormatter.ISO_LOCAL_DATE);
            builder.append("date-released: ");
            builder.append(date.format(formatter));
        } catch (DateTimeParseException e) {
            try {
                formatString = "yyyy-MM";
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
                YearMonth yearMonth = YearMonth.parse(fieldText, formatter);
                int month = yearMonth.getMonth().getValue();
                int year = yearMonth.getYear();
                builder.append("month: ");
                builder.append(month);
                builder.append(OS.NEWLINE);
                builder.append("  year: "); // Account for indent since we are in `preferred-citation` indentation block
                builder.append(year);
            } catch (DateTimeParseException f) {
                try {
                    formatString = "yyyy";
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
                    int year = Year.parse(fieldText, formatter).getValue();
                    builder.append("year: ");
                    builder.append(year);
                } catch (DateTimeParseException g) {
                    builder.append("issue-date: ");
                    builder.append(fieldText);
                }
            }
        }
        return builder.toString();
    }
}
