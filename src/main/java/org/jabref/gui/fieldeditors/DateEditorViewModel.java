package org.jabref.gui.fieldeditors;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.Date;
import org.jabref.model.strings.StringUtil;

public class DateEditorViewModel extends AbstractEditorViewModel {
    private final DateTimeFormatter dateFormatter;

    public DateEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, DateTimeFormatter dateFormatter, FieldCheckers fieldCheckers) {
        super(fieldName, suggestionProvider, fieldCheckers);
        this.dateFormatter = dateFormatter;
    }

    public StringConverter<TemporalAccessor> getDateToStringConverter() {
        return new StringConverter<TemporalAccessor>() {

            @Override
            public String toString(TemporalAccessor date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public TemporalAccessor fromString(String string) {
                if (StringUtil.isNotBlank(string)) {
                    try {
                        return dateFormatter.parse(string);
                    } catch (DateTimeParseException exception) {
                        // We accept all kinds of dates (not just in the format specified)
                        return Date.parse(string).map(Date::toTemporalAccessor).orElse(null);
                    }
                } else {
                    return null;
                }
            }
        };
    }
}
