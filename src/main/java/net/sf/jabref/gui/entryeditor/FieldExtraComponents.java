/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.gui.entryeditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.external.ExternalFilePanel;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FieldContentSelector;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.OpenFileFilter;
import net.sf.jabref.gui.date.DatePickerButton;
import net.sf.jabref.gui.entryeditor.EntryEditor.StoreFieldAction;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.journals.Abbreviations;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.date.EasyDateFormat;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.MonthUtil;

public class FieldExtraComponents {

    private static final String ABBREVIATION_TOOLTIP_TEXT = "<HTML>"
            + Localization.lang("Switches between full and abbreviated journal name if the journal name is known.")
            + "<BR>" + Localization.lang("To set up, go to") + " <B>" + Localization.lang("Options") + " -> "
            + Localization.lang("Manage journal abbreviations") + "</B></HTML>";


    /**
     * Add controls for switching between abbreviated and full journal names.
     * If this field also has a FieldContentSelector, we need to combine these.
     *
     * @param frame
     * @param panel
     * @param editor
     * @param entry
     * @param contentSelectors
     * @param storeFieldAction
     * @return
     */
    static JComponent getJournalExtraComponent(JabRefFrame frame, BasePanel panel, FieldEditor editor, BibEntry entry,
            HashSet<FieldContentSelector> contentSelectors, StoreFieldAction storeFieldAction) {
        JPanel controls = new JPanel();
        controls.setLayout(new BorderLayout());
        if (panel.metaData.getData(Globals.SELECTOR_META_PREFIX + editor.getFieldName()) != null) {
            FieldContentSelector ws = new FieldContentSelector(frame, panel, frame, editor, panel.metaData,
                    storeFieldAction, false, ", ");
            contentSelectors.add(ws);
            controls.add(ws, BorderLayout.NORTH);
        }

        // Button to toggle abbreviated/full journal names
        JButton button = new JButton(Localization.lang("Toggle abbreviation"));
        button.setToolTipText(ABBREVIATION_TOOLTIP_TEXT);
        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String text = editor.getText();
                if (Abbreviations.journalAbbrev.isKnownName(text)) {
                    String s = Abbreviations.toggleAbbreviation(text);

                    if (s != null) {
                        editor.setText(s);
                        storeFieldAction.actionPerformed(new ActionEvent(editor, 0, ""));
                        panel.undoManager.addEdit(new UndoableFieldChange(entry, editor.getFieldName(), text, s));
                    }
                }
            }
        });

        controls.add(button, BorderLayout.SOUTH);
        return controls;
    }

    public static JComponent getBrowseExtraComponent(JabRefFrame frame, FieldEditor fieldEditor,
            EntryEditor entryEditor) {
        JButton but = new JButton(Localization.lang("Browse"));
        ((JComponent) fieldEditor).addMouseListener(entryEditor.new ExternalViewerListener());

        but.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String dir = fieldEditor.getText();

                if (dir.isEmpty()) {
                    dir = Globals.prefs.get(fieldEditor.getFieldName() + Globals.FILETYPE_PREFS_EXT, "");
                }

                String chosenFile = FileDialogs.getNewFile(frame, new File(dir), '.' + fieldEditor.getFieldName(),
                        JFileChooser.OPEN_DIALOG, false);

                if (chosenFile != null) {
                    File newFile = new File(chosenFile); // chooser.getSelectedFile();
                    fieldEditor.setText(newFile.getPath());
                    Globals.prefs.put(fieldEditor.getFieldName() + Globals.FILETYPE_PREFS_EXT, newFile.getPath());
                    entryEditor.updateField(fieldEditor);
                }
            }
        });

        return but;
    }

    static JComponent getBrowseDocExtraComponent(JabRefFrame frame, BasePanel panel, FieldEditor fieldEditor,
            EntryEditor entryEditor, Boolean isZip) {

        final String ext = '.' + fieldEditor.getFieldName().toLowerCase();
        final OpenFileFilter off;
        if (isZip) {
            off = new OpenFileFilter(new String[] {ext, ext + ".gz", ext + ".bz2"});
        } else {
            off = new OpenFileFilter(new String[] {ext});
        }

        return new ExternalFilePanel(frame, panel.metaData(), entryEditor, fieldEditor.getFieldName(), off,
                fieldEditor);
    }

    public static JComponent getExternalExtraComponent(FieldEditor fieldEditor, EntryEditor entryEditor) {
        ((JComponent) fieldEditor).addMouseListener(entryEditor.new ExternalViewerListener());

        return null;
    }

    public static JComponent getYesNoExtraComponent(FieldEditor fieldEditor, EntryEditor entryEditor) {
        final String[] options = {"", "Yes", "No"};
        JComboBox<String> yesno = new JComboBox<>(options);
        yesno.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {

                fieldEditor.setText(((String) yesno.getSelectedItem()).toLowerCase());
                entryEditor.updateField(fieldEditor);
            }
        });
        return yesno;

    }

    public static JComponent getMonthExtraComponent(FieldEditor fieldEditor, EntryEditor entryEditor) {
        final String[] options = new String[13];
        options[0] = Localization.lang("Select");
        for (int i = 1; i <= 12; i++) {
            options[i] = MonthUtil.getMonthByNumber(i).fullName;
        }
        JComboBox<String> month = new JComboBox<>(options);
        month.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int monthnumber = month.getSelectedIndex();
                if (monthnumber >= 1) {
                    if (Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE)) {
                        fieldEditor.setText(String.valueOf(monthnumber));
                    } else {
                        fieldEditor.setText((MonthUtil.getMonthByNumber(monthnumber).bibtexFormat));
                    }
                } else {
                    fieldEditor.setText("");
                }
                entryEditor.updateField(fieldEditor);
                month.setSelectedIndex(0);
            }
        });
        return month;

    }

    public static JComponent getSetOwnerExtraComponent(FieldEditor fieldEditor, StoreFieldAction storeFieldAction) {
        JButton button = new JButton(Localization.lang("Auto"));
        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fieldEditor.setText(Globals.prefs.get(JabRefPreferences.DEFAULT_OWNER));
                storeFieldAction.actionPerformed(new ActionEvent(fieldEditor, 0, ""));
            }
        });
        return button;

    }

    public static JComponent getURLExtraComponent(FieldEditor fieldEditor, StoreFieldAction storeFieldAction) {
        ((JComponent) fieldEditor).setDropTarget(new DropTarget((Component) fieldEditor, DnDConstants.ACTION_NONE,
                new SimpleUrlDragDrop(fieldEditor, storeFieldAction)));

        return null;
    }

    public static JComponent getSelectorExtraComponent(JabRefFrame frame, BasePanel panel, FieldEditor editor,
            HashSet<FieldContentSelector> contentSelectors, StoreFieldAction storeFieldAction) {
        FieldContentSelector ws = new FieldContentSelector(frame, panel, frame, editor, panel.metaData,
                storeFieldAction, false,
                "author".equals(editor.getFieldName()) || "editor".equals(editor.getFieldName()) ? " and " : ", ");
        contentSelectors.add(ws);
        return ws;
    }

    public static JComponent getDateTimeExtraComponent(FieldEditor editor, Boolean isDatePicker) {
        ((JTextArea) editor).addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) // double click
                {
                    String date = new EasyDateFormat().getCurrentDate();
                    editor.setText(date);
                }
            }
        });

        // insert a datepicker, if the extras field contains this command
        if (isDatePicker) {
            DatePickerButton datePicker = new DatePickerButton(editor);
            return datePicker.getDatePicker();
        } else {
            return null;
        }

    }

}
