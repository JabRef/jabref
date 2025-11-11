package org.jabref.gui.fieldeditors;

import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

import javax.swing.undo.UndoManager;

import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.Field;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateEditorViewModel extends AbstractEditorViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateEditorViewModel.class);
    private final DateTimeFormatter dateFormatter;
    private static final TemporalAccessor RANGE_SENTINEL = LocalDate.of(1, 1, 1);
    public DateEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, DateTimeFormatter dateFormatter, FieldCheckers fieldCheckers, UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager);
        this.dateFormatter = dateFormatter;
    }

    public StringConverter<TemporalAccessor> getDateToStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(TemporalAccessor date) {
                String currentText = textProperty().get();
                if (currentText != null && !currentText.isEmpty()) {
                    Optional<Date> parsedDate = Date.parse(currentText);
                    if (parsedDate.isPresent() && parsedDate.get().getEndDate().isPresent()) {

                        return currentText;
                    }
                }
                if (date != null && date != RANGE_SENTINEL) {
                    try {
                        return dateFormatter.format(date);
                    } catch (DateTimeException ex) {
                        LOGGER.debug("Cannot format date", ex);
                        return "";
                    }
                }
                return "";
            }
            private String sanitizeIncompleteRange(String dateString) {
                String trimmed = dateString.trim();

                // Remove the trailing slash (e.g., "2010/" → "2010")
                if (trimmed.endsWith("/") && !trimmed.matches(".*\\d+/\\d+.*")) {
                    LOGGER.debug("Sanitizing incomplete range (trailing slash): {}", trimmed);
                    return trimmed.substring(0, trimmed.length() - 1).trim();
                }

                // Remove the leading slash (e.g., "/2010" → "2010")
                if (trimmed.startsWith("/") && !trimmed.matches(".*\\d+/\\d+.*")) {
                    LOGGER.debug("Sanitizing incomplete range (leading slash): {}", trimmed);
                    return trimmed.substring(1).trim();
                }

                return dateString;
            }
            @Override
            public TemporalAccessor fromString(String string) {
                if (StringUtil.isNotBlank(string)) {
                    // ✅ NEW: Sanitize incomplete ranges (e.g., "2010/" → "2010")
                    String sanitizedString = sanitizeIncompleteRange(string);

                    // Priority 1: Check if it's a date range
                    Optional<Date> parsedDate = Date.parse(sanitizedString);
                    if (parsedDate.isPresent() && parsedDate.get().getEndDate().isPresent()) {
                        // It's a range! Return sentinel to signal this is a special case
                        // The toString() method will retrieve the value from textProperty()
                        return RANGE_SENTINEL;
                    }

                    // Priority 2: Try strict format parsing
                    try {
                        return dateFormatter.parse(sanitizedString);
                    } catch (DateTimeParseException exception) {
                        // Priority 3: Try flexible parsing (single dates only)
                        return parsedDate
                                .filter(date -> date.getEndDate().isEmpty())
                                .map(Date::toTemporalAccessor)
                                .orElse(null);
                    }
                } else {
                    return null;
                }
            }
        };

    }

}
