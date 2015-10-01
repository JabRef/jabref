package net.sf.jabref.gui.journals;

import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.logic.journals.Abbreviation;
import net.sf.jabref.logic.journals.Abbreviations;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.undo.UndoManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class JournalAbbreviationsUtil {

    // @formatter:off
    private static final String TOOLTIP_TEXT = "<HTML>" + Localization.lang("Switches between full and abbreviated journal name "
            + "if the journal name is known.")
            + "<BR>" + Localization.lang("To set up, go to <B>Tools -> Manage journal abbreviations</B>") + ".</HTML>";
    // @formatter:on

    /**
     * Create a control panel for the entry editor's journal field, to toggle
     * abbreviated/full journal name
     * @param editor The FieldEditor for the journal field.
     * @return The control panel for the entry editor.
     */
    public static JComponent getNameSwitcher(final EntryEditor entryEditor, final FieldEditor editor,
            final UndoManager undoManager) {
        JButton button = new JButton(Localization.lang("Toggle abbreviation"));
        button.setToolTipText(TOOLTIP_TEXT);
        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String text = editor.getText();
                if (Abbreviations.journalAbbrev.isKnownName(text)) {
                    String s = toggleAbbreviation(text);

                    if (s != null) {
                        editor.setText(s);
                        entryEditor.storeFieldAction.actionPerformed(new ActionEvent(editor, 0, ""));
                        undoManager.addEdit(new UndoableFieldChange(entryEditor.getEntry(), editor.getFieldName(),
                                text, s));
                    }
                }
            }

            public String toggleAbbreviation(String currentText) {
                return Abbreviations.journalAbbrev.getNextAbbreviation(currentText).orElse(currentText);
            }
        });

        return button;
    }

    public static TableModel getTableModel(JournalAbbreviationRepository journalAbbreviationRepository) {
        Object[][] cells = new Object[journalAbbreviationRepository.size()][2];
        int row = 0;
        for (Abbreviation abbreviation : journalAbbreviationRepository.getAbbreviations()) {
            cells[row][0] = abbreviation.getName();
            cells[row][1] = abbreviation.getIsoAbbreviation();
            row++;
        }

        return new DefaultTableModel(cells, new Object[] {Localization.lang("Full name"),
                Localization.lang("Abbreviation")}) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }
}
