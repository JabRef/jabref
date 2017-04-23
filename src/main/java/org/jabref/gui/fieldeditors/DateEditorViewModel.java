package org.jabref.gui.fieldeditors;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.util.StringConverter;

import org.jabref.model.entry.Date;
import org.jabref.model.strings.StringUtil;

public class DateEditorViewModel extends AbstractEditorViewModel {
    private final DateTimeFormatter dateFormatter;

    public DateEditorViewModel(DateTimeFormatter dateFormatter) {
        this.dateFormatter = dateFormatter;
    }

    public StringConverter<LocalDate> getDateToStringConverter() {
        return new StringConverter<LocalDate>() {

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (StringUtil.isNotBlank(string)) {
                    // We accept all kinds of dates (not just in the format specified)
                    return Date.parse(string).map(Date::toLocalDate).orElse(null);
                } else {
                    return null;
                }
            }
        };
    }
}
