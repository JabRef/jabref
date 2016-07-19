package net.sf.jabref.gui.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.integrity.IntegrityCheck;
import net.sf.jabref.logic.integrity.IntegrityMessage;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.preferences.JabRefPreferences;

public class IntegrityCheckAction extends MnemonicAwareAction {

    private static final String ELLIPSES = "...";


    private final JabRefFrame frame;


    public IntegrityCheckAction(JabRefFrame frame) {
        this.frame = frame;
        putValue(Action.NAME, Localization.menuTitle("Check integrity") + ELLIPSES);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        IntegrityCheck check = new IntegrityCheck(frame.getCurrentBasePanel().getBibDatabaseContext());
        List<IntegrityMessage> messages = check.checkBibtexDatabase();

        if (messages.isEmpty()) {
            JOptionPane.showMessageDialog(frame.getCurrentBasePanel(), Localization.lang("No problems found."));
        } else {
            // prepare data model
            Object[][] model = new Object[messages.size()][3];
            int i = 0;
            for (IntegrityMessage message : messages) {
                model[i][0] = message.getEntry().getCiteKey();
                model[i][1] = message.getFieldName();
                model[i][2] = message.getMessage();
                i++;
            }

            // construct view
            JTable table = new JTable(model,
                    new Object[] {Localization.lang("BibTeX key"), Localization.lang("Field"),
                            Localization.lang("Message")});

            table.setAutoCreateRowSorter(true);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setDefaultEditor(Object.class, null);
            ListSelectionModel selectionModel = table.getSelectionModel();

            selectionModel.addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting()) {
                    String citeKey = (String) model[table.convertRowIndexToModel(table.getSelectedRow())][0];
                    String fieldName = (String) model[table.convertRowIndexToModel(table.getSelectedRow())][1];
                    frame.getCurrentBasePanel().editEntryByKeyAndFocusField(citeKey, fieldName);
                }
            });

            table.setRowHeight(Globals.prefs.getInt(JabRefPreferences.MENU_FONT_SIZE) + 2);

            table.getColumnModel().getColumn(0).setPreferredWidth(80);
            table.getColumnModel().getColumn(1).setPreferredWidth(30);
            table.getColumnModel().getColumn(2).setPreferredWidth(250);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            JScrollPane scrollPane = new JScrollPane(table);
            String title = Localization.lang("%0 problem(s) found", String.valueOf(messages.size()));
            JDialog dialog = new JDialog(frame, title, false);
            dialog.add(scrollPane);
            dialog.setSize(600, 500);

            // show view
            dialog.setVisible(true);
        }
    }
}
