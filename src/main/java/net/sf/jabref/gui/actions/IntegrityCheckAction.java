package net.sf.jabref.gui.actions;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.util.GUIUtil;
import net.sf.jabref.logic.integrity.IntegrityCheck;
import net.sf.jabref.logic.integrity.IntegrityMessage;
import net.sf.jabref.logic.l10n.Localization;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class IntegrityCheckAction extends MnemonicAwareAction {

    private static final String ELLIPSES = "...";


    private final JabRefFrame frame;


    public IntegrityCheckAction(JabRefFrame frame) {
        this.frame = frame;
        putValue(Action.NAME, Localization.menuTitle("Check integrity") + ELLIPSES);
        putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.CHECK_INTEGRITY));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        IntegrityCheck check = new IntegrityCheck(frame.getCurrentBasePanel().getBibDatabaseContext(),
                Globals.prefs.getFileDirectoryPreferences());
        List<IntegrityMessage> messages = check.checkBibtexDatabase();

        if (messages.isEmpty()) {
            JOptionPane.showMessageDialog(frame.getCurrentBasePanel(), Localization.lang("No problems found."));
        } else {
            Map<String, Boolean> showMessage = new HashMap<>();
            // prepare data model
            Object[][] model = new Object[messages.size()][3];
            int i = 0;
            for (IntegrityMessage message : messages) {
                model[i][0] = message.getEntry().getCiteKeyOptional().orElse("");
                model[i][1] = message.getFieldName();
                model[i][2] = message.getMessage();
                showMessage.put(message.getMessage(), true);
                i++;
            }

            // construct view
            JTable table = new JTable(model,
                    new Object[] {Localization.lang("BibTeX key"), Localization.lang("Field"),
                            Localization.lang("Message")});

            RowFilter<Object, Object> filter = new RowFilter<Object, Object>() {

                @Override
                public boolean include(Entry<?, ?> entry) {
                    return showMessage.get(entry.getStringValue(2));
                }
            };

            TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
            sorter.setRowFilter(filter);
            table.setRowSorter(sorter);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setDefaultEditor(Object.class, null);
            ListSelectionModel selectionModel = table.getSelectionModel();


            selectionModel.addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting()) {
                    try {
                        String citeKey = (String) model[table.convertRowIndexToModel(table.getSelectedRow())][0];
                        String fieldName = (String) model[table.convertRowIndexToModel(table.getSelectedRow())][1];
                        frame.getCurrentBasePanel().editEntryByKeyAndFocusField(citeKey, fieldName);
                    } catch (ArrayIndexOutOfBoundsException exception) {
                        // Ignore -- most likely caused by filtering out the earlier selected row
                    }
                }
            });

            GUIUtil.correctRowHeight(table);

            table.getColumnModel().getColumn(0).setPreferredWidth(100);
            table.getColumnModel().getColumn(1).setPreferredWidth(60);
            table.getColumnModel().getColumn(2).setPreferredWidth(400);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            JScrollPane scrollPane = new JScrollPane(table);
            String title = Localization.lang("%0 problem(s) found", String.valueOf(messages.size()));
            JDialog dialog = new JDialog(frame, title, false);

            JPopupMenu menu = new JPopupMenu();
            for (String messageString : showMessage.keySet()) {
                JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(messageString, true);
                menuItem.addActionListener(event -> {
                    showMessage.put(messageString, menuItem.isSelected());
                    ((AbstractTableModel) table.getModel()).fireTableDataChanged();
                });
                menu.add(menuItem);
            }
            JButton menuButton = new JButton(Localization.lang("Filter"));
            menuButton.addActionListener(entry -> menu.show(menuButton, 0, menuButton.getHeight()));
            FormBuilder builder = FormBuilder.create()
                    .layout(new FormLayout("fill:pref:grow", "fill:pref:grow, 2dlu, pref"));

            builder.add(scrollPane).xy(1, 1);
            builder.add(menuButton).xy(1, 3, "c, b");
            dialog.add(builder.getPanel());
            dialog.setSize(600, 600);

            // show view
            dialog.setVisible(true);
        }
    }
}
