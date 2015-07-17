package net.sf.jabref.journals;

import net.sf.jabref.EntryEditor;
import net.sf.jabref.FieldEditor;
import net.sf.jabref.Globals;
import net.sf.jabref.journals.logic.Abbreviation;
import net.sf.jabref.journals.logic.JournalAbbreviationRepository;
import net.sf.jabref.undo.UndoableFieldChange;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.undo.UndoManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class JournalAbbreviationsUtil {
    private static final String TOOLTIP_TEXT = "<HTML>" + Globals.lang("Switches between full and abbreviated journal name "
            + "if the journal name is known.")
            + "<BR>" + Globals.lang("To set up, go to <B>Tools -> Manage journal abbreviations</B>") + ".</HTML>";

    /**
     * Create a control panel for the entry editor's journal field, to toggle
     * abbreviated/full journal name
     * @param editor The FieldEditor for the journal field.
     * @return The control panel for the entry editor.
     */
    public static JComponent getNameSwitcher(final EntryEditor entryEditor, final FieldEditor editor,
            final UndoManager undoManager) {
        JButton button = new JButton(Globals.lang("Toggle abbreviation"));
        button.setToolTipText(TOOLTIP_TEXT);
        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String text = editor.getText();
                if (Globals.journalAbbrev.isKnownName(text)) {
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
                return Globals.journalAbbrev.getNextAbbreviation(currentText).or(currentText);
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

        return new DefaultTableModel(cells, new Object[] {Globals.lang("Full name"),
                Globals.lang("Abbreviation")}) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }
}
