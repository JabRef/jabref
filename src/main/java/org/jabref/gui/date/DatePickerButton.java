package org.jabref.gui.date;

import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jabref.Globals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.fieldeditors.FieldEditor;
import org.jabref.preferences.JabRefPreferences;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.optionalusertools.DateChangeListener;
import com.github.lgooddatepicker.zinternaltools.DateChangeEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * wrapper and service class for the DatePicker handling at the EntryEditor
 */
public class DatePickerButton implements DateChangeListener {

    private static final Log LOGGER = LogFactory.getLog(DatePickerButton.class);

    private final DatePicker datePicker;
    private final JPanel panel = new JPanel();
    private final FieldEditor editor;
    private final DateTimeFormatter dateTimeFormatter;


    public DatePickerButton(FieldEditor pEditor, boolean useIsoFormat) {
        if (useIsoFormat) {
            dateTimeFormatter = DateTimeFormatter.ISO_DATE;
        } else {
            dateTimeFormatter = DateTimeFormatter.ofPattern(Globals.prefs.get(JabRefPreferences.TIME_STAMP_FORMAT));
        }

        // Create a date picker with hidden text field (showing button only).
        DatePickerSettings dateSettings = new DatePickerSettings();
        dateSettings.setVisibleDateTextField(false);
        dateSettings.setGapBeforeButtonPixels(0);

        datePicker = new DatePicker(dateSettings);
        datePicker.addDateChangeListener(this);
        datePicker.getComponentToggleCalendarButton().setIcon(IconTheme.JabRefIcon.DATE_PICKER.getIcon());
        datePicker.getComponentToggleCalendarButton().setText("");

        panel.setLayout(new BorderLayout());
        panel.add(datePicker, BorderLayout.WEST);
        editor = pEditor;
    }

    @Override
    public void dateChanged(DateChangeEvent dateChangeEvent) {
        LocalDate date = datePicker.getDate();
        String newDate = "";
        if (date != null) {
            newDate = dateTimeFormatter.format(date.atStartOfDay());
        }
        if (!newDate.equals(editor.getText())) {
            editor.setText(newDate);
        }
        // Set focus to editor component after changing its text:
        editor.getTextComponent().requestFocus();
    }

    public JComponent getDatePicker() {
        //return datePicker;
        return panel;
    }

    /**
     * Used to set the calender popup to the currently used Date
     * @param dateString
     */
    public void updateDatePickerDate(String dateString) {
        // unregister DateChangeListener before update to prevent circular calls resulting in IllegalStateExceptions
        datePicker.removeDateChangeListener(this);

        if(dateString!=null && !dateString.isEmpty()) {
            try {
                datePicker.setDate(LocalDate.parse(dateString, dateTimeFormatter));
            } catch (DateTimeParseException exception) {
                LOGGER.warn("Unable to parse stored date for field '"+editor.getFieldName()+"' with current settings. "
                        + "Clear button in calender popup will not work.");
            }
        }

        datePicker.addDateChangeListener(this);
    }
}
