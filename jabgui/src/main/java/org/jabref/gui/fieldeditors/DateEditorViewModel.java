package org.jabref.gui.fieldeditors;

import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import javax.swing.undo.UndoManager;

import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.field.Field;

import org.jabref.model.entry.DateRangeUtil;

import java.time.LocalDate;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateEditorViewModel extends AbstractEditorViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateEditorViewModel.class);

    private final DateTimeFormatter dateFormatter;
    private static final TemporalAccessor RANGE_SENTINEL = LocalDate.of(1, 1, 1);

    public DateEditorViewModel(Field field,
                               SuggestionProvider<?> suggestionProvider,
                               DateTimeFormatter dateFormatter,
                               FieldCheckers fieldCheckers,
                               UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager);
        this.dateFormatter = dateFormatter;
    }

    public Optional<String> getText() {
        return Optional.ofNullable(text.get());
    }

    public void setText(String newValue) {
        String sanitized = DateRangeUtil.sanitizeIncompleteRange(newValue);
        text.set(sanitized);
    }

    public StringConverter<TemporalAccessor> getToStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(TemporalAccessor value) {
                if (value == null || value.equals(RANGE_SENTINEL)) {
                    return "";
                }

                return dateFormatter.format(value);
            }

            @Override
            public TemporalAccessor fromString(String text) {
                if (StringUtil.isBlank(text)) {
                    return RANGE_SENTINEL;
                }

                try {
                    return dateFormatter.parse(text);
                } catch (DateTimeException e) {  // ✔ FIX multi-catch
                    LOGGER.error("Error while parsing date {}", text, e);
                    return RANGE_SENTINEL;
                }
            }
        };
    }
}
