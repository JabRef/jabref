package org.jabref.gui.fieldeditors;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import javax.swing.undo.UndoManager;

import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DateEditorViewModelTest {
    private DateEditorViewModel viewModel;
    private StringConverter<TemporalAccessor> dateToStringConverter;

    private TemporalAccessor sentinel() {
        return LocalDate.of(1, 1, 1);
    }

    @BeforeEach
    void setup() {
        Field field = Mockito.mock(Field.class);
        SuggestionProvider<?> suggestionProvider = Mockito.mock(SuggestionProvider.class);
        FieldCheckers fieldCheckers = Mockito.mock(FieldCheckers.class);
        UndoManager undoManager = new UndoManager();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        viewModel = new DateEditorViewModel(field, suggestionProvider, formatter, fieldCheckers, undoManager);
        dateToStringConverter = viewModel.getDateToStringConverter();
    }

    @Test
    void fromStringRecognizesDateRangeAndReturnsSentinel() {
        StringConverter<TemporalAccessor> converter = viewModel.getDateToStringConverter();
        TemporalAccessor result = converter.fromString("2020-01-01/2020-12-31");
        assertEquals(sentinel(), result);
    }

    @Test
    void toStringReturnsOriginalRangeText() {
        viewModel.textProperty().set("2020-01-01/2020-12-31");
        StringConverter<TemporalAccessor> converter = viewModel.getDateToStringConverter();

        String output = converter.toString(sentinel());
        assertEquals("2020-01-01/2020-12-31", output);
    }

    @Test
    void sanitizeTrailingSlash() {
        StringConverter<TemporalAccessor> converter = viewModel.getDateToStringConverter();
        TemporalAccessor result = converter.fromString("2020/");
        Year year = Year.from(result);
        assertEquals(2020, year.getValue());
    }

    @Test
    void sanitizeLeadingSlash() {
        StringConverter<TemporalAccessor> converter = viewModel.getDateToStringConverter();
        TemporalAccessor result = converter.fromString("/2020");
        Year year = Year.from(result);
        assertEquals(2020, year.getValue());
    }

    @Test
    void singleDateFormatsNormally() {
        StringConverter<TemporalAccessor> converter = viewModel.getDateToStringConverter();
        TemporalAccessor parsed = converter.fromString("2020-05-20");

        String output = converter.toString(parsed);
        assertEquals("2020-05-20", output);
    }

    @Test
    void invalidDateReturnsNull() {
        StringConverter<TemporalAccessor> converter = dateToStringConverter;
        TemporalAccessor result = converter.fromString("invalid-date");

        assertNull(result);
    }
}
