package org.jabref.gui.entryeditor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.contentselector.FieldContentSelector;
import org.jabref.gui.date.DatePickerButton;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.entryeditor.EntryEditor.StoreFieldAction;
import org.jabref.gui.fieldeditors.FieldEditor;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.entry.Month;
import org.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FieldExtraComponents {

    private static final Log LOGGER = LogFactory.getLog(FieldExtraComponents.class);

    private static final String ABBREVIATION_TOOLTIP_TEXT = "<HTML>"
            + Localization.lang("Switches between full and abbreviated journal name if the journal name is known.")
            + "<BR>" + Localization.lang("To set up, go to") + " <B>" + Localization.lang("Options") + " -> "
            + Localization.lang("Manage journal abbreviations") + "</B></HTML>";

    private FieldExtraComponents() {
    }


    /**
     * Add controls for switching between abbreviated and full journal names.
     * If this field also has a FieldContentSelector, we need to combine these.
     *
     * @param panel
     * @param editor
     * @param entry
     * @param storeFieldAction
     * @return
     */
    public static Optional<JComponent> getJournalExtraComponent(JabRefFrame frame, BasePanel panel, FieldEditor editor,
            BibEntry entry, Set<FieldContentSelector> contentSelectors, StoreFieldAction storeFieldAction) {
        JPanel controls = new JPanel();
        controls.setLayout(new BorderLayout());
        if (!panel.getBibDatabaseContext().getMetaData().getContentSelectorValuesForField(editor.getFieldName()).isEmpty()) {
            FieldContentSelector ws = new FieldContentSelector(frame, panel, frame, editor, storeFieldAction, false,
                    ", ");
            contentSelectors.add(ws);
            controls.add(ws, BorderLayout.NORTH);
        }

        // Button to toggle abbreviated/full journal names
        JButton button = new JButton(Localization.lang("Toggle abbreviation"));
        button.setToolTipText(ABBREVIATION_TOOLTIP_TEXT);
        button.addActionListener(actionEvent -> {
            String text = editor.getText();
            JournalAbbreviationRepository abbreviationRepository = Globals.journalAbbreviationLoader
                    .getRepository(Globals.prefs.getJournalAbbreviationPreferences());
            if (abbreviationRepository.isKnownName(text)) {
                String s = abbreviationRepository.getNextAbbreviation(text).orElse(text);

                if (s != null) {
                    editor.setText(s);
                    storeFieldAction.actionPerformed(new ActionEvent(editor, 0, ""));
                    panel.getUndoManager().addEdit(new UndoableFieldChange(entry, editor.getFieldName(), text, s));
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
        /*
        JTextComponent url = (JTextComponent) fieldEditor;

        url.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                checkUrl();
            }

            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                checkUrl();
            }

            @Override
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
        });
        */

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
            fieldEditor.setText(((String) yesno.getSelectedItem()).toLowerCase(Locale.ROOT));
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
        List<String> monthNames = Arrays.stream(Month.values()).map(Month::getFullName).collect(Collectors.toList());
        List<String> options = new ArrayList<>(13);
        options.add(Localization.lang("Select"));
        options.addAll(monthNames);

        JComboBox<String> month = new JComboBox<>(options.toArray(new String[0]));
        month.addActionListener(actionEvent -> {
            int monthNumber = month.getSelectedIndex();
            if (monthNumber >= 1) {
                if (type == BibDatabaseMode.BIBLATEX) {
                    fieldEditor.setText(String.valueOf(monthNumber));
                } else {
                    fieldEditor.setText(Month.getMonthByNumber(monthNumber).get().getJabRefFormat());
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
        FieldContentSelector ws = new FieldContentSelector(frame, panel, frame, editor, storeFieldAction, false,
                InternalBibtexFields.getFieldProperties(editor.getFieldName())
                        .contains(FieldProperty.PERSON_NAMES) ? " and " : ", ");
        contentSelectors.add(ws);
        return Optional.of(ws);
    }

    /**
     * Set up field such that double click inserts the current date
     * If isDataPicker is True, a button with a data picker is returned
     *
     * @param editor reference to the FieldEditor to display the date value
     * @param useDatePicker shows a DatePickerButton if true
     * @param useIsoFormat if true ISO format is always used
     * @return
     */
    public static Optional<JComponent> getDateTimeExtraComponent(FieldEditor editor, boolean useDatePicker,
            boolean useIsoFormat) {
        /*
        ((JTextArea) editor).addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // double click
                    String date = "";
                    if (useIsoFormat) {
                        date = DateTimeFormatter.ISO_DATE.format(LocalDate.now());
                    } else {
                        date = DateTimeFormatter.ofPattern(Globals.prefs.get(JabRefPreferences.TIME_STAMP_FORMAT)).format(
                                LocalDateTime.now());
                    }
                    editor.setText(date);
                }
            }
        });
        */

        // insert a datepicker, if the extras field contains this command
        if (useDatePicker) {
            DatePickerButton datePicker = new DatePickerButton(editor, useIsoFormat);

            /*
            // register a DocumentListener on the underlying text document which notifies the DatePicker which date is currently set
            ((JTextArea) editor).getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void insertUpdate(DocumentEvent e) {
                    datePicker.updateDatePickerDate(editor.getText());
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    datePicker.updateDatePickerDate(editor.getText());
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    datePicker.updateDatePickerDate(editor.getText());
                }
            });
            */

            return Optional.of(datePicker.getDatePicker());
        } else {
            return Optional.empty();
        }

    }

    /**
     * Return a dropdown list with the alternatives for editor type fields
     *
     * @param fieldEditor
     * @param entryEditor
     * @return
     */

    public static Optional<JComponent> getEditorTypeExtraComponent(FieldEditor fieldEditor, EntryEditor entryEditor) {
        final String[] optionValues = {"", "editor", "compiler", "founder", "continuator", "redactor", "reviser",
                "collaborator"};
        final String[] optionDescriptions = {Localization.lang("Select"), Localization.lang("Editor"),
                Localization.lang("Compiler"), Localization.lang("Founder"), Localization.lang("Continuator"),
                Localization.lang("Redactor"), Localization.lang("Reviser"), Localization.lang("Collaborator")};
        JComboBox<String> editorType = new JComboBox<>(optionDescriptions);
        editorType.addActionListener(actionEvent -> {
            fieldEditor.setText(optionValues[editorType.getSelectedIndex()]);
            entryEditor.updateField(fieldEditor);
        });
        return Optional.of(editorType);

    }

    /**
     * Return a dropdown list with the alternatives for pagination type fields
     *
     * @param fieldEditor
     * @param entryEditor
     * @return
     */

    public static Optional<JComponent> getPaginationExtraComponent(FieldEditor fieldEditor, EntryEditor entryEditor) {
        final String[] optionValues = {"", "page", "column", "line", "verse", "section", "paragraph", "none"};
        final String[] optionDescriptions = {Localization.lang("Select"), Localization.lang("Page"),
                Localization.lang("Column"), Localization.lang("Line"), Localization.lang("Verse"),
                Localization.lang("Section"), Localization.lang("Paragraph"), Localization.lang("None")};
        JComboBox<String> pagination = new JComboBox<>(optionDescriptions);
        pagination.addActionListener(actionEvent -> {
            fieldEditor.setText(optionValues[pagination.getSelectedIndex()]);
            entryEditor.updateField(fieldEditor);
        });
        return Optional.of(pagination);
    }

    /**
     * Return a dropdown list with the alternatives for pagination type fields
     *
     * @param fieldEditor
     * @param entryEditor
     * @return
     */

    public static Optional<JComponent> getTypeExtraComponent(FieldEditor fieldEditor, EntryEditor entryEditor,
            boolean isPatent) {
        String[] optionValues;
        String[] optionDescriptions;
        if (isPatent) {
            optionValues = new String[] {"", "patent", "patentde", "patenteu", "patentfr", "patentuk", "patentus",
                    "patreq", "patreqde", "patreqeu", "patreqfr", "patrequk", "patrequs"};
            optionDescriptions = new String[] {Localization.lang("Select"), Localization.lang("Patent"),
                    Localization.lang("German patent"), Localization.lang("European patent"),
                    Localization.lang("French patent"), Localization.lang("British patent"),
                    Localization.lang("U.S. patent"), Localization.lang("Patent request"),
                    Localization.lang("German patent request"), Localization.lang("European patent request"),
                    Localization.lang("French patent request"), Localization.lang("British patent request"),
                    Localization.lang("U.S. patent request")};
        } else {
            optionValues = new String[] {"", "mathesis", "phdthesis", "candthesis", "techreport", "resreport",
                    "software", "datacd", "audiocd"};
            optionDescriptions = new String[] {Localization.lang("Select"), Localization.lang("Master's thesis"),
                    Localization.lang("PhD thesis"), Localization.lang("Candidate thesis"),
                    Localization.lang("Technical report"), Localization.lang("Research report"),
                    Localization.lang("Software"), Localization.lang("Data CD"), Localization.lang("Audio CD")};
        }
        JComboBox<String> type = new JComboBox<>(optionDescriptions);
        type.addActionListener(actionEvent -> {
            fieldEditor.setText(optionValues[type.getSelectedIndex()]);
            entryEditor.updateField(fieldEditor);
        });
        return Optional.of(type);
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
        final String[] optionDescriptions = {Localization.lang("Select"), Localization.lang("Female name"),
                Localization.lang("Male name"),
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
