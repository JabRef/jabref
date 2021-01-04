package org.jabref.gui.util.component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import org.jabref.gui.Globals;
import org.jabref.gui.fieldeditors.TextInputControlBehavior;
import org.jabref.gui.fieldeditors.contextmenu.EditorContextAction;
import org.jabref.gui.util.BindingsHelper;

/**
 * A date picker with configurable datetime format where both date and time can be changed via the text field and the
 * date can additionally be changed via the JavaFX default date picker. Also supports incomplete dates.
 *
 * First recall how the date picker normally works: - The user selects a date in the popup, which sets {@link
 * #valueProperty()} to the selected date. - The converter ({@link #converterProperty()}) is used to transform the date
 * to a string representation and display it in the text field.
 *
 * The idea is now to intercept the process and add an additional step: - The user selects a date in the popup, which
 * sets {@link #valueProperty()} to the selected date. - The date is converted to a {@link TemporalAccessor} (i.e,
 * enriched by a time component) using {@link #addCurrentTime(LocalDate)} - The string converter ({@link
 * #stringConverterProperty()}) is used to transform the temporal accessor to a string representation and display it in
 * the text field.
 *
 * Inspiration taken from https://github.com/edvin/tornadofx-controls/blob/master/src/main/java/tornadofx/control/DateTimePicker.java
 */
public class TemporalAccessorPicker extends DatePicker {
    private final ObjectProperty<TemporalAccessor> temporalAccessorValue = new SimpleObjectProperty<>(null);

    private final DateTimeFormatter defaultFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ObjectProperty<StringConverter<TemporalAccessor>> converter = new SimpleObjectProperty<>(null);

    public TemporalAccessorPicker() {
        setConverter(new InternalConverter());

        // Synchronize changes of the underlying date value with the temporalAccessorValue
        BindingsHelper.bindBidirectional(valueProperty(), temporalAccessorValue,
                TemporalAccessorPicker::addCurrentTime,
                TemporalAccessorPicker::getDate);

        getEditor().setOnContextMenuRequested(event -> {
            ContextMenu contextMenu = new ContextMenu();
            contextMenu.getItems().setAll(EditorContextAction.getDefaultContextMenuItems(getEditor(), Globals.getKeyPrefs()));
            TextInputControlBehavior.showContextMenu(getEditor(), contextMenu, event);
        });
    }

    private static TemporalAccessor addCurrentTime(LocalDate date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.of(date, LocalTime.now());
    }

    private static LocalDate getDate(TemporalAccessor temporalAccessor) {
        if (temporalAccessor == null) {
            return null;
        }

        return getLocalDate(temporalAccessor);
    }

    private static LocalDate getLocalDate(TemporalAccessor dateTime) {
        // Try to get as much information from the temporal accessor
        LocalDate date = dateTime.query(TemporalQueries.localDate());
        if (date != null) {
            return date;
        }

        try {
            return YearMonth.from(dateTime).atDay(1);
        } catch (DateTimeException exception) {
            return Year.from(dateTime).atDay(1);
        }
    }

    public final ObjectProperty<StringConverter<TemporalAccessor>> stringConverterProperty() {
        return converter;
    }

    public final StringConverter<TemporalAccessor> getStringConverter() {
        StringConverter<TemporalAccessor> newConverter = new StringConverter<>() {
            @Override
            public String toString(TemporalAccessor value) {
                return defaultFormatter.format(value);
            }

            @Override
            public TemporalAccessor fromString(String value) {
                return LocalDateTime.parse(value, defaultFormatter);
            }
        };
        return Objects.requireNonNullElseGet(stringConverterProperty().get(), () -> newConverter);
    }

    public final void setStringConverter(StringConverter<TemporalAccessor> value) {
        stringConverterProperty().set(value);
    }

    public TemporalAccessor getTemporalAccessorValue() {
        return temporalAccessorValue.get();
    }

    public void setTemporalAccessorValue(TemporalAccessor temporalAccessorValue) {
        this.temporalAccessorValue.set(temporalAccessorValue);
    }

    public ObjectProperty<TemporalAccessor> temporalAccessorValueProperty() {
        return temporalAccessorValue;
    }

    private class InternalConverter extends StringConverter<LocalDate> {
        @Override
        public String toString(LocalDate object) {
            TemporalAccessor value = getTemporalAccessorValue();
            return (value != null) ? getStringConverter().toString(value) : "";
        }

        @Override
        public LocalDate fromString(String value) {
            if ((value == null) || value.isEmpty()) {
                temporalAccessorValue.set(null);
                return null;
            }

            TemporalAccessor dateTime = getStringConverter().fromString(value);
            temporalAccessorValue.set(dateTime);
            return getLocalDate(dateTime);
        }
    }
}
