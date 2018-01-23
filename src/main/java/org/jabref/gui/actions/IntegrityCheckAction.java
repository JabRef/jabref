package org.jabref.gui.actions;

import java.awt.Component;
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
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.jabref.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.integrity.IntegrityCheck;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrityCheckAction extends MnemonicAwareAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrityCheckAction.class);
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
                Globals.prefs.getFileDirectoryPreferences(),
                Globals.prefs.getBibtexKeyPatternPreferences(),
                Globals.journalAbbreviationLoader.getRepository(Globals.prefs.getJournalAbbreviationPreferences()),
                Globals.prefs.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));

        final JDialog integrityDialog = new JDialog(frame, true);
        integrityDialog.setUndecorated(true);
        integrityDialog.setLocationRelativeTo(frame);
        JProgressBar integrityProgressBar = new JProgressBar();
        integrityProgressBar.setIndeterminate(true);
        integrityProgressBar.setStringPainted(true);
        integrityProgressBar.setString(Localization.lang("Checking integrity..."));
        integrityDialog.add(integrityProgressBar);
        integrityDialog.pack();
        SwingWorker<List<IntegrityMessage>, Void> worker = new SwingWorker<List<IntegrityMessage>, Void>() {
            @Override
            protected List<IntegrityMessage> doInBackground() {
                List<IntegrityMessage> messages = check.checkBibtexDatabase();
                return messages;
            }

            @Override
            protected void done() {
                integrityDialog.dispose();
            }
        };
        worker.execute();
        integrityDialog.setVisible(true);
        List<IntegrityMessage> messages = null;
        try {
            messages = worker.get();
        } catch (Exception ex) {
            LOGGER.error("Integrity check failed.", ex);
        }

        if (messages.isEmpty()) {
            JOptionPane.showMessageDialog(frame.getCurrentBasePanel(), Localization.lang("No problems found."));
        } else {
            Map<String, Boolean> showMessage = new HashMap<>();
            // prepare data model
            Object[][] model = new Object[messages.size()][4];
            int i = 0;
            for (IntegrityMessage message : messages) {
                model[i][0] = message.getEntry().getId();
                model[i][1] = message.getEntry().getCiteKeyOptional().orElse("");
                model[i][2] = message.getFieldName();
                model[i][3] = message.getMessage();
                showMessage.put(message.getMessage(), true);
                i++;
            }

            // construct view
            JTable table = new JTable(model,
                    new Object[] {"ID", Localization.lang("BibTeX key"), Localization.lang("Field"),
                            Localization.lang("Message")});

            // hide IDs
            TableColumnModel columnModel = table.getColumnModel();
            columnModel.removeColumn(columnModel.getColumn(0));

            RowFilter<Object, Object> filter = new RowFilter<Object, Object>() {

                @Override
                public boolean include(Entry<?, ?> entry) {
                    return showMessage.get(entry.getStringValue(3));
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
                        String entryId = (String) model[table.convertRowIndexToModel(table.getSelectedRow())][0];
                        String fieldName = (String) model[table.convertRowIndexToModel(table.getSelectedRow())][2];
                        frame.getCurrentBasePanel().editEntryByIdAndFocusField(entryId, fieldName);
                    } catch (ArrayIndexOutOfBoundsException exception) {
                        // Ignore -- most likely caused by filtering out the earlier selected row
                    }
                }
            });

            // BibTeX key
            table.getColumnModel().getColumn(0).setPreferredWidth(100);
            // field name
            table.getColumnModel().getColumn(1).setPreferredWidth(60);
            // message
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

            JButton filterNoneButton = new JButton(Localization.lang("Filter None"));
            filterNoneButton.addActionListener(event -> {
                for (Component component : menu.getComponents()) {
                    if (component instanceof JCheckBoxMenuItem) {
                        JCheckBoxMenuItem checkBox = (JCheckBoxMenuItem) component;
                        if (checkBox.isSelected()) {
                            checkBox.setSelected(false);
                            showMessage.put(checkBox.getText(), checkBox.isSelected());
                        }
                    }
                    ((AbstractTableModel) table.getModel()).fireTableDataChanged();
                }
            });

            JButton filterAllButton = new JButton(Localization.lang("Filter All"));
            filterAllButton.addActionListener(event -> {
                for (Component component : menu.getComponents()) {
                    if (component instanceof JCheckBoxMenuItem) {
                        JCheckBoxMenuItem checkBox = (JCheckBoxMenuItem) component;
                        if (!checkBox.isSelected()) {
                            checkBox.setSelected(true);
                            showMessage.put(checkBox.getText(), checkBox.isSelected());
                        }
                    }
                    ((AbstractTableModel) table.getModel()).fireTableDataChanged();
                }
            });

            builder.add(filterNoneButton).xy(1, 3, "left, b");
            builder.add(filterAllButton).xy(1, 3, "right, b");
            builder.add(scrollPane).xy(1, 1);
            builder.add(menuButton).xy(1, 3, "c, b");
            dialog.add(builder.getPanel());
            dialog.setSize(600, 600);

            // show view
            dialog.setVisible(true);
        }
    }
}
