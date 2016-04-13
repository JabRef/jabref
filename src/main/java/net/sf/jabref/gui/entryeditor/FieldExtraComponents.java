/*  Copyright (C) 2003-2016 JabRef contributors.
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.FieldProperties;
import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FieldContentSelector;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.date.DatePickerButton;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.gui.entryeditor.EntryEditor.StoreFieldAction;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.mergeentries.MergeEntryDOIDialog;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.importer.fetcher.CrossRef;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLUtil;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.logic.util.date.EasyDateFormat;
import net.sf.jabref.model.database.BibDatabaseMode;
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
    public static Optional<JComponent> getJournalExtraComponent(JabRefFrame frame, BasePanel panel, FieldEditor editor,
            BibEntry entry, Set<FieldContentSelector> contentSelectors, StoreFieldAction storeFieldAction) {
        JPanel controls = new JPanel();
        controls.setLayout(new BorderLayout());
        if (panel.getBibDatabaseContext().getMetaData().getData(Globals.SELECTOR_META_PREFIX + editor.getFieldName()) != null) {
            FieldContentSelector ws = new FieldContentSelector(frame, panel, frame, editor, panel.getBibDatabaseContext().getMetaData(),
                    storeFieldAction, false, ", ");
            contentSelectors.add(ws);
            controls.add(ws, BorderLayout.NORTH);
        }

        // Button to toggle abbreviated/full journal names
        JButton button = new JButton(Localization.lang("Toggle abbreviation"));
        button.setToolTipText(ABBREVIATION_TOOLTIP_TEXT);
        button.addActionListener(actionEvent -> {
            String text = editor.getText();
            JournalAbbreviationRepository abbreviationRepository = Globals.journalAbbreviationLoader.getRepository();
            if (abbreviationRepository.isKnownName(text)) {
                String s = abbreviationRepository.getNextAbbreviation(text).orElse(text);

                if (s != null) {
                    editor.setText(s);
                    storeFieldAction.actionPerformed(new ActionEvent(editor, 0, ""));
                    panel.undoManager.addEdit(new UndoableFieldChange(entry, editor.getFieldName(), text, s));
                }
            }
        });

        controls.add(button, BorderLayout.SOUTH);
        return Optional.of(controls);
    }

    /**
     * Set up a mouse listener for opening an external viewer for with with EXTRA_EXTERNAL
     *
     * @param fieldEditor
     * @param panel
     * @return
     */
    public static Optional<JComponent> getExternalExtraComponent(BasePanel panel, FieldEditor fieldEditor) {
        JPanel controls = new JPanel();
        controls.setLayout(new BorderLayout());
        JButton button = new JButton(Localization.lang("Open"));
        button.setEnabled(false);
        button.addActionListener(actionEvent -> {
            try {
                JabRefDesktop.openExternalViewer(panel.getBibDatabaseContext(), fieldEditor.getText(), fieldEditor.getFieldName());
            } catch (IOException ex) {
                panel.output(Localization.lang("Unable to open link."));
            }
        });

        controls.add(button, BorderLayout.SOUTH);

        // enable/disable button
        JTextComponent url = (JTextComponent) fieldEditor;

        DocumentListener documentListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent documentEvent) {
                checkUrl();
            }

            public void insertUpdate(DocumentEvent documentEvent) {
                checkUrl();
            }

            public void removeUpdate(DocumentEvent documentEvent) {
                checkUrl();
            }

            private void checkUrl() {
                if (URLUtil.isURL(url.getText())) {
                    button.setEnabled(true);
                } else {
                    button.setEnabled(false);
                }
            }
        };
        url.getDocument().addDocumentListener(documentListener);

        return Optional.of(controls);
    }

    /**
     * Set up a mouse listener for opening an external viewer and fetching by DOI
     *
     * @param fieldEditor
     * @param panel
     * @return
     */
    public static Optional<JComponent> getDoiExtraComponent(BasePanel panel, EntryEditor entryEditor, FieldEditor fieldEditor) {
        JPanel controls = new JPanel();
        controls.setLayout(new BorderLayout());
        // open doi link
        JButton button = new JButton(Localization.lang("Open"));
        button.setEnabled(false);
        button.addActionListener(actionEvent -> {
            try {
                JabRefDesktop.openExternalViewer(panel.getBibDatabaseContext(), fieldEditor.getText(), fieldEditor.getFieldName());
            } catch (IOException ex) {
                panel.output(Localization.lang("Unable to open link."));
            }
        });
        // lookup doi
        JButton doiButton = new JButton(Localization.lang("Lookup DOI"));
        doiButton.addActionListener(actionEvent -> {
                Optional<DOI> doi = CrossRef.findDOI(entryEditor.getEntry());
                if (doi.isPresent()) {
                    JTextComponent field = (JTextComponent) fieldEditor.getTextComponent();
                    field.setText(doi.get().getDOI());
                } else {
                    panel.frame().setStatus(Localization.lang("No DOI found"));
                }
        });
        // fetch bibtex data
        JButton fetchButton = new JButton(Localization.lang("Get BibTeX data from DOI"));
        fetchButton.setEnabled(false);
        fetchButton.addActionListener(actionEvent -> new MergeEntryDOIDialog(panel));

        controls.add(button, BorderLayout.NORTH);
        controls.add(doiButton, BorderLayout.CENTER);
        controls.add(fetchButton, BorderLayout.SOUTH);

        // enable/disable button
        JTextComponent doi = (JTextComponent) fieldEditor;

        DocumentListener documentListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent documentEvent) {
                checkDoi();
            }

            public void insertUpdate(DocumentEvent documentEvent) {
                checkDoi();
            }

            public void removeUpdate(DocumentEvent documentEvent) {
                checkDoi();
            }

            private void checkDoi() {
                Optional<DOI> doiUrl = DOI.build(doi.getText());
                if(doiUrl.isPresent()) {
                    button.setEnabled(true);
                    fetchButton.setEnabled(true);
                } else {
                    button.setEnabled(false);
                    fetchButton.setEnabled(false);
                }
            }
        };
        doi.getDocument().addDocumentListener(documentListener);

        return Optional.of(controls);
    }

    /**
     * Return a dropdown list containing Yes and No for fields with EXTRA_YES_NO
     *
     * @param fieldEditor
     * @param entryEditor
     * @return
     */
    public static Optional<JComponent> getYesNoExtraComponent(FieldEditor fieldEditor, EntryEditor entryEditor) {
        final String[] options = {"", "Yes", "No"};
        JComboBox<String> yesno = new JComboBox<>(options);
        yesno.addActionListener(actionEvent -> {
            fieldEditor.setText(((String) yesno.getSelectedItem()).toLowerCase());
            entryEditor.updateField(fieldEditor);
        });
        return Optional.of(yesno);

    }

    /**
     * Return a dropdown list with the month names for fields with EXTRA_MONTH
     *
     * @param fieldEditor
     * @param entryEditor
     * @param type
     * @return
     */
    public static Optional<JComponent> getMonthExtraComponent(FieldEditor fieldEditor, EntryEditor entryEditor, BibDatabaseMode type) {
        final String[] options = new String[13];
        options[0] = Localization.lang("Select");
        for (int i = 1; i <= 12; i++) {
            options[i] = MonthUtil.getMonthByNumber(i).fullName;
        }
        JComboBox<String> month = new JComboBox<>(options);
        month.addActionListener(actionEvent -> {
            int monthnumber = month.getSelectedIndex();
            if (monthnumber >= 1) {
                if (type == BibDatabaseMode.BIBLATEX) {
                    fieldEditor.setText(String.valueOf(monthnumber));
                } else {
                    fieldEditor.setText("#" + (MonthUtil.getMonthByNumber(monthnumber).bibtexFormat) + "#");
                }
            } else {
                fieldEditor.setText("");
            }
            entryEditor.updateField(fieldEditor);
            month.setSelectedIndex(0);
        });
        return Optional.of(month);

    }

    /**
     * Return a button which sets the owner if the field for fields with EXTRA_SET_OWNER
     * @param fieldEditor
     * @param storeFieldAction
     * @return
     */
    public static Optional<JComponent> getSetOwnerExtraComponent(FieldEditor fieldEditor,
            StoreFieldAction storeFieldAction) {
        JButton button = new JButton(Localization.lang("Auto"));
        button.addActionListener(actionEvent -> {
            fieldEditor.setText(Globals.prefs.get(JabRefPreferences.DEFAULT_OWNER));
            storeFieldAction.actionPerformed(new ActionEvent(fieldEditor, 0, ""));
        });
        return Optional.of(button);

    }

    /**
     * Set up a drop target for URLs for fields with EXTRA_URL
     *
     * @param fieldEditor
     * @param storeFieldAction
     * @return
     */
    public static Optional<JComponent> getURLExtraComponent(FieldEditor fieldEditor,
            StoreFieldAction storeFieldAction) {
        ((JComponent) fieldEditor).setDropTarget(new DropTarget((Component) fieldEditor, DnDConstants.ACTION_NONE,
                new SimpleUrlDragDrop(fieldEditor, storeFieldAction)));

        return Optional.empty();
    }

    /**
     * Return a button opening a content selector for fields where one exists
     *
     * @param frame
     * @param panel
     * @param editor
     * @param contentSelectors
     * @param storeFieldAction
     * @return
     */
    public static Optional<JComponent> getSelectorExtraComponent(JabRefFrame frame, BasePanel panel, FieldEditor editor,
            Set<FieldContentSelector> contentSelectors, StoreFieldAction storeFieldAction) {
        FieldContentSelector ws = new FieldContentSelector(frame, panel, frame, editor, panel.getBibDatabaseContext().getMetaData(),
                storeFieldAction, false,
                InternalBibtexFields.getFieldExtras(editor.getFieldName())
                        .contains(FieldProperties.PERSON_NAMES) ? " and " : ", ");
        contentSelectors.add(ws);
        return Optional.of(ws);
    }

    /**
     * Set up field such that double click inserts the current date
     * If isDataPicker is True, a button with a data picker is returned
     *
     * @param editor
     * @param isDatePicker
     * @return
     */
    public static Optional<JComponent> getDateTimeExtraComponent(FieldEditor editor, Boolean isDatePicker) {
        ((JTextArea) editor).addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {// double click
                    String date = new EasyDateFormat().getCurrentDate();
                    editor.setText(date);
                }
            }
        });

        // insert a datepicker, if the extras field contains this command
        if (isDatePicker) {
            DatePickerButton datePicker = new DatePickerButton(editor);
            return Optional.of(datePicker.getDatePicker());
        } else {
            return Optional.empty();
        }

    }

    /**
     * Return a dropdown list with the gender alternatives for fields with GENDER
     *
     * @param fieldEditor
     * @param entryEditor
     * @return
     */

    public static Optional<JComponent> getGenderExtraComponent(FieldEditor fieldEditor, EntryEditor entryEditor) {
        final String[] optionValues = {"", "sf", "sm", "sp", "pf", "pm", "pn", "pp"};
        final String[] optionDescriptions = {"", Localization.lang("Female name"), Localization.lang("Male name"),
                Localization.lang("Neuter name"), Localization.lang("Female names"), Localization.lang("Male names"),
                Localization.lang("Neuter names"), Localization.lang("Mixed names")};
        JComboBox<String> gender = new JComboBox<>(optionDescriptions);
        gender.addActionListener(actionEvent -> {
            fieldEditor.setText(optionValues[gender.getSelectedIndex()]);
            entryEditor.updateField(fieldEditor);
        });
        return Optional.of(gender);

    }
}
