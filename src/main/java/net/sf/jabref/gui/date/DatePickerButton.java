/*  Copyright (C) 2003-2011 Raik Nagel
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
// function : wrapper and service class for the DatePicker handling at the
//            EntryEditor

package net.sf.jabref.gui.date;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JPanel;

import net.sf.jabref.logic.util.date.EasyDateFormat;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.util.FocusRequester;

import com.michaelbaranov.microba.calendar.DatePicker;

public class DatePickerButton implements ActionListener {

    private final DatePicker datePicker = new DatePicker();
    private final JPanel panel = new JPanel();
    private final FieldEditor editor;


    public DatePickerButton(FieldEditor pEditor) {
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
            editor.setText(new EasyDateFormat().getDateAt(date));
            // Set focus to editor component after changing its text:
            new FocusRequester(editor.getTextComponent());
        }
    }

    public JComponent getDatePicker() {
        //return datePicker;
        return panel;
    }
}
