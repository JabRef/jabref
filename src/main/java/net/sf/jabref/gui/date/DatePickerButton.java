package net.sf.jabref.gui.date;

import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.swing.JComponent;
import javax.swing.JPanel;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.preferences.JabRefPreferences;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.optionalusertools.DateChangeListener;
import com.github.lgooddatepicker.zinternaltools.DateChangeEvent;

/**
 * wrapper and service class for the DatePicker handling at the EntryEditor
 */
public class DatePickerButton implements DateChangeListener {

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
        if (date != null) {
            editor.setText(dateTimeFormatter.format(date.atStartOfDay()));
        } else {
            // in this case the user selected "clear" in the date picker, so we just clear the field
            editor.setText("");
        }
        // Set focus to editor component after changing its text:
        editor.getTextComponent().requestFocus();
    }

    public JComponent getDatePicker() {
        //return datePicker;
        return panel;
    }
}
