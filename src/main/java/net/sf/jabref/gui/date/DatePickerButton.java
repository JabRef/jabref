package net.sf.jabref.gui.date;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JPanel;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.logic.util.date.EasyDateFormat;

import com.michaelbaranov.microba.calendar.DatePicker;

/**
 * wrapper and service class for the DatePicker handling at the EntryEditor
 */
public class DatePickerButton implements ActionListener {

    private final DatePicker datePicker = new DatePicker();
    private final JPanel panel = new JPanel();
    private final FieldEditor editor;
    private final boolean isoFormat;


    public DatePickerButton(FieldEditor pEditor, Boolean isoFormat) {
        this.isoFormat = isoFormat;
        datePicker.showButtonOnly(true);
        datePicker.addActionListener(this);
        datePicker.setShowTodayButton(true);
        panel.setLayout(new BorderLayout());
        panel.add(datePicker, BorderLayout.WEST);
        editor = pEditor;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Date date = datePicker.getDate();
        if (date != null) {
            if (isoFormat) {
                editor.setText(EasyDateFormat.isoDateFormat().getDateAt(date));
            } else {
                editor.setText(EasyDateFormat.fromPreferences(Globals.prefs).getDateAt(date));
            }
            // Set focus to editor component after changing its text:
            new FocusRequester(editor.getTextComponent());
        }
    }

    public JComponent getDatePicker() {
        //return datePicker;
        return panel;
    }
}
